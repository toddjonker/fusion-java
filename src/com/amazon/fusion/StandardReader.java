// Copyright (c) 2012-2019 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionList.immutableList;
import static com.amazon.fusion.FusionList.nullList;
import static com.amazon.fusion.FusionNumber.makeDecimal;
import static com.amazon.fusion.FusionNumber.makeFloat;
import static com.amazon.fusion.FusionNumber.makeInt;
import static com.amazon.fusion.FusionSexp.immutableSexp;
import static com.amazon.fusion.FusionSexp.nullSexp;
import static com.amazon.fusion.FusionString.makeString;
import static com.amazon.fusion.FusionStruct.STRUCT_MERGE_FUNCTION;
import static com.amazon.fusion.FusionStruct.immutableStruct;
import static com.amazon.fusion.FusionStruct.nullStruct;
import static com.amazon.fusion.FusionSymbol.makeSymbol;
import static com.amazon.fusion.FusionTimestamp.makeTimestamp;
import static com.amazon.fusion.SourceLocation.forCurrentSpan;
import static com.amazon.fusion.SyntaxValue.STX_PROPERTY_ORIGINAL;
import static com.amazon.ion.IntegerSize.BIG_INTEGER;
import static java.lang.Boolean.TRUE;
import static java.util.AbstractMap.SimpleEntry;
import com.amazon.fusion.FusionList.BaseList;
import com.amazon.fusion.FusionSexp.BaseSexp;
import com.amazon.ion.Decimal;
import com.amazon.ion.IntegerSize;
import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.Timestamp;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 *
 */
class StandardReader
{
    /**
     * Reads a single Ion datum.
     *
     * @param source must be positioned on the value to be read.
     */
    static Object read(Evaluator eval,
                       IonReader source)
        throws FusionException
    {
        try
        {
            return read(eval, source, null, false);
        }
        catch (IonException e)
        {
            throw new FusionException("Error reading data: " + e.getMessage(),
                                      e);
        }
    }


    /**
     * Reads a single Ion datum as source.
     *
     * @param source must be positioned on the value to be read.
     * @param name may be null.
     */
    static SyntaxValue readSyntax(Evaluator  eval,
                                  IonReader  source,
                                  SourceName name)
        throws FusionException
    {
        try
        {
            return (SyntaxValue) read(eval, source, name, true);
        }
        catch (IonException e)
        {
            // We don't try to create a SourceLocation from the reader because
            // it usually doesn't have a current span when an error is thrown,
            // and since the IonException's message will contain it.
            String nameStr = (name != null ? name.display() : "source");
            String message =
                "Error reading " + nameStr + ":\n" + e.getMessage();
            throw new FusionErrorException(message, e);
        }
    }


    /**
     * Reads a single Ion datum, optionally as syntax objects.
     * @param source must be positioned on the value to be read.
     * @param name may be null.
     *
     * @throws IonException if there's a problem reading the source data.
     */
    private static Object read(Evaluator  eval,
                               IonReader  source,
                               SourceName name,
                               boolean    readingSyntax)
        throws FusionException, IonException
    {
        IonType type = source.getType();
        assert type != null;

        String[] anns = source.getTypeAnnotations();
        SourceLocation loc =
            (readingSyntax ? forCurrentSpan(source, name) : null);

        BaseValue datum;
        switch (type)
        {
            case NULL:
            {
                datum = FusionNull.makeNullNull(eval, anns);
                break;
            }
            case BOOL:
            {
                if (source.isNullValue())
                {
                    datum = makeBool(eval, anns, (Boolean) null);
                }
                else
                {
                    boolean value = source.booleanValue();
                    datum = makeBool(eval, anns, value);
                }
                break;
            }
            case INT:
            {
                IntegerSize size = source.getIntegerSize();
                if (size == null) size = BIG_INTEGER;
                switch (size)
                {
                    case INT:
                    {
                        int value = source.intValue();
                        datum = makeInt(eval, anns, value);
                        break;
                    }
                    case LONG:
                    {
                        long value = source.longValue();
                        datum = makeInt(eval, anns, value);
                        break;
                    }
                    case BIG_INTEGER:
                    default:
                    {
                        BigInteger value = source.bigIntegerValue();
                        datum = makeInt(eval, anns, value);
                        break;
                    }
                }
                break;
            }
            case DECIMAL:
            {
                Decimal value = source.decimalValue();
                datum = makeDecimal(eval, anns, value);
                break;
            }
            case FLOAT:
            {
                if (source.isNullValue())
                {
                    datum = makeFloat(eval, anns, (Double) null);
                }
                else
                {
                    double value = source.doubleValue();
                    datum = makeFloat(eval, anns, value);
                }
                break;
            }
            case TIMESTAMP:
            {
                Timestamp value = source.timestampValue();
                datum = makeTimestamp(eval, anns, value);
                break;
            }
            case STRING:
            {
                String value = source.stringValue();
                datum = makeString(eval, anns, value);
                break;
            }
            case SYMBOL:
            {
                String value = source.stringValue();
                datum = makeSymbol(eval, anns, value);
                break;
            }
            case BLOB:
            {
                byte[] value =
                    (source.isNullValue() ? null : source.newBytes());
                datum = FusionBlob.forBytesNoCopy(eval, anns, value);
                break;
            }
            case CLOB:
            {
                byte[] value =
                    (source.isNullValue() ? null : source.newBytes());
                datum = FusionClob.forBytesNoCopy(eval, anns, value);
                break;
            }
            case LIST:
            {
                datum = readList(eval, source, name, readingSyntax, anns);
                break;
            }
            case SEXP:
            {
                datum = readSexp(eval, source, name, readingSyntax, anns);
                break;
            }
            case STRUCT:
            {
                datum = readStruct(eval, source, name, readingSyntax, anns);
                break;
            }
            default:
            {
                throw new UnsupportedOperationException("Bad type: " + type);
            }
        }

        if (readingSyntax)
        {
            SyntaxValue stx = datum.makeOriginalSyntax(eval, loc);
            assert stx.findProperty(eval, STX_PROPERTY_ORIGINAL) == TRUE;
            return stx;
        }

        return datum;
    }



    /**
     * @param source must be positioned on the value to be read, but not
     * stepped-in.  Must not be positioned on a null value.
     * @param name may be null.
     *
     * @throws IonException if there's a problem reading the source data.
     */
    private static ArrayList<Object> readSequence(Evaluator  eval,
                                                  IonReader  source,
                                                  SourceName name,
                                                  boolean    readingSyntax)
        throws FusionException, IonException
    {
        assert ! source.isNullValue();

        ArrayList<Object> children = new ArrayList<>();

        source.stepIn();
        while (source.next() != null)
        {
            Object child = read(eval, source, name, readingSyntax);
            children.add(child);
        }
        source.stepOut();

        return children;
    }


    /**
     * @return an immutable list of syntax objects.
     *
     * @throws IonException if there's a problem reading the source data.
     */
    private static BaseList readList(Evaluator  eval,
                                     IonReader  source,
                                     SourceName name,
                                     boolean    readingSyntax,
                                     String[]   annotations)
        throws FusionException, IonException
    {
        if (source.isNullValue())
        {
            return nullList(eval, annotations);
        }

        ArrayList<Object> elements =
            readSequence(eval, source, name, readingSyntax);
        return immutableList(eval, annotations, elements);
    }


    /**
     * @throws IonException if there's a problem reading the source data.
     */
    private static BaseSexp readSexp(Evaluator  eval,
                                     IonReader  source,
                                     SourceName name,
                                     boolean    readingSyntax,
                                     String[]   annotations)
        throws FusionException, IonException
    {
        if (source.isNullValue())
        {
            return nullSexp(eval, annotations);
        }

        List<Object> elements =
            readSequence(eval, source, name, readingSyntax);
        return immutableSexp(eval, annotations, elements);
    }


    private static Iterator<Map.Entry<String, Object>>
        makeIonReaderStructIterator(final Evaluator eval,
                                    final IonReader source,
                                    final SourceName name,
                                    final boolean readingSyntax)
    {
        return new Iterator<Map.Entry<String, Object>>()
        {
            Object current;

            private void attemptAdvance() {
                if (current == null)
                {
                    current = source.next();
                }
            }

            @Override
            public boolean hasNext()
            {
                attemptAdvance();
                return current != null;
            }

            @Override
            public Map.Entry<String, Object> next()
            {
                if (hasNext())
                {
                    String key = source.getFieldName();
                    Object value;
                    try
                    {
                        value = read(eval, source, name, readingSyntax);
                    }
                    catch (FusionException e)
                    {
                        // Throwing a RuntimeException so that we can adhere to
                        //   the Iterator interface.
                        throw new RuntimeException(e);
                    }

                    Map.Entry<String, Object> ret = new SimpleEntry<>(key, value);
                    current = null;
                    return ret;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }



    /**
     * @throws IonException if there's a problem reading the source data.
     */
    private static BaseValue readStruct(Evaluator  eval,
                                        IonReader  source,
                                        SourceName name,
                                        boolean    readingSyntax,
                                        String[]   anns)
        throws FusionException, IonException
    {
        if (source.isNullValue())
        {
            return nullStruct(eval, anns);
        }

        source.stepIn();

        Iterator<Map.Entry<String, Object>> iterator =
            makeIonReaderStructIterator(eval, source, name, readingSyntax);
        FunctionalHashTrie<String, Object> fht =
            FunctionalHashTrie.merge(iterator,
                                     STRUCT_MERGE_FUNCTION);

        source.stepOut();

        try
        {
            return immutableStruct(fht, anns);
        }
        catch (RuntimeException e)
        {
            if (e.getCause() instanceof FusionException)
            {
                throw (FusionException) e.getCause();
            }
            throw e;
        }
    }
}
