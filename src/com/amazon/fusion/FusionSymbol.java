// Copyright (c) 2013-2015 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.falseBool;
import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionBool.trueBool;
import static com.amazon.fusion.FusionString.makeString;
import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import com.amazon.fusion.FusionBool.BaseBool;
import com.amazon.fusion.FusionText.BaseText;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.WeakHashMap;


final class FusionSymbol
{
    private FusionSymbol() {}


    abstract static class BaseSymbol
        extends BaseText
    {
        static final BaseSymbol[] EMPTY_ARRAY = new BaseSymbol[0];

        private BaseSymbol() {}


        /**
         * NOT FOR APPLICATION USE!
         *
         * @param value must not be empty but may be null to make
         * {@code null.symbol}.
         */
        static BaseSymbol internSymbol(String value)
        {
            if (value == null) return NULL_SYMBOL;

            if (value.isEmpty())
            {
                throw new IllegalArgumentException("Cannot make an empty symbol");
            }

            // Prevent other threads from touching the intern table.
            // This doesn't prevent the GC from removing entries!
            synchronized (ourActualSymbols)
            {
                WeakReference<ActualSymbol> ref = ourActualSymbols.get(value);
                if (ref != null)
                {
                    // There's a chance that the entry for a string will exist but
                    // the weak reference has been cleared.
                    ActualSymbol interned = ref.get();
                    if (interned != null) return interned;
                }

                // We don't have an interned symbol, so make one.
                ActualSymbol sym = new ActualSymbol(value);
                ref = new WeakReference<>(sym);
                ourActualSymbols.put(value, ref);

                return sym;
            }
        }

        /**
         * NOT FOR APPLICATION USE!
         */
        static BaseSymbol[] internSymbols(String[] names)
        {
            int len = names.length;
            if (len == 0) return EMPTY_ARRAY;

            BaseSymbol[] syms = new BaseSymbol[len];
            for (int i = 0; i < len; i++)
            {
                syms[i] = internSymbol(names[i]);
            }
            return syms;
        }


        static String[] unsafeSymbolsToJavaStrings(Object[] fusionSymbols)
        {
            int len = fusionSymbols.length;
            if (len == 0) return EMPTY_STRING_ARRAY;

            String[] strs = new String[len];
            for (int i = 0; i < len; i++)
            {
                strs[i] = ((BaseSymbol) fusionSymbols[i]).stringValue();
            }
            return strs;
        }


        @Override
        BaseSymbol annotate(Evaluator eval, String[] annotations)
        {
            return FusionSymbol.annotate(this, annotations);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseSymbol)
            {
                String r = ((BaseSymbol) right).stringValue();
                if (r != null)
                {
                    String l = this.stringValue(); // not null
                    if (l.equals(r))
                    {
                        return trueBool(eval);
                    }
                }
            }

            return falseBool(eval);
        }

        boolean isNonEmpty()
        {
            String value = stringValue();
            return (value != null && value.length() != 0);
        }

        private boolean isKeyword()
        {
            String value = stringValue();
            return (value != null
                    && value.startsWith("_")
                    && value.endsWith("_"));
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            if (isKeyword())
            {
                return SyntaxKeyword.makeOriginal(eval, loc, this);
            }
            return SyntaxSymbol.makeOriginal(eval, loc, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
            throws FusionException
        {
            if (isKeyword())
            {
                return SyntaxKeyword.make(eval, loc, this);
            }
            return SyntaxSymbol.make(eval, loc, this);
        }
    }


    private static class NullSymbol
        extends BaseSymbol
    {
        private NullSymbol() {}

        @Override
        String stringValue()
        {
            return null;
        }

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof BaseSymbol
                         && ((BaseSymbol) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            // No need to check for keywords.
            return SyntaxSymbol.makeOriginal(eval, loc, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
            throws FusionException
        {
            // No need to check for keywords.
            return SyntaxSymbol.make(eval, loc, this);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newNullSymbol();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeNull(IonType.SYMBOL);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null.symbol");
        }
    }


    /**
     * An interned, unannotated, non-null symbol.
     */
    private static class ActualSymbol
        extends BaseSymbol
    {
        private final String myContent;

        private ActualSymbol(String content)
        {
            assert content != null;
            myContent = content;
        }

        @Override
        public boolean equals(Object other)
        {
            return this == other;  // Due to interning.
        }

        @Override
        public int hashCode()
        {
            return myContent.hashCode();
        }

        @Override
        String stringValue()
        {
            return myContent;
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newSymbol(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeSymbol(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            IonTextUtils.printSymbol(out, myContent);
        }

        @Override
        void display(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append(myContent);
        }
    }


    private static final class AnnotatedSymbol
        extends BaseSymbol
        implements Annotated
    {
        /** Not null or empty */
        final String[] myAnnotations;

        /** Not null, and not AnnotatedBool */
        final BaseSymbol  myValue;

        private AnnotatedSymbol(String[] annotations, BaseSymbol value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (other instanceof AnnotatedSymbol)
            {
                AnnotatedSymbol that = (AnnotatedSymbol) other;
                if (this.myValue == that.myValue)
                {
                    return Arrays.equals(myAnnotations, that.myAnnotations);
                }
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int result = 1;
            result = 31 * result + myValue.hashCode();
            result = 31 * result + Arrays.hashCode(myAnnotations);
            return result;
        }

        @Override
        public String[] annotationsAsJavaStrings()
        {
            return myAnnotations;
        }

        @Override
        BaseSymbol annotate(Evaluator eval, String[] annotations)
        {
            return FusionSymbol.annotate(myValue, annotations);
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

        @Override
        String stringValue()
        {
            return myValue.stringValue();
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.tightEquals(eval, right);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.looseEquals(eval, right);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            IonValue iv = myValue.copyToIonValue(factory,
                                                 throwOnConversionFailure);
            iv.setTypeAnnotations(myAnnotations);
            return iv;
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.setTypeAnnotations(myAnnotations);
            myValue.ionize(eval, out);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            writeAnnotations(out, myAnnotations);
            myValue.write(eval, out);
        }
    }


    //========================================================================
    // Constructors


    private static final BaseSymbol NULL_SYMBOL  = new NullSymbol();

    /**
     * Interning table for unannotated, non-null symbols.
     * <p>
     * The keys are the same string instances contained by the symbols, so the
     * entry will be retained at least as long as the symbol is reachable.
     * Using a weak reference to the symbol allows it to be reclaimed when it
     * is otherwise unused. After the symbol is GCed then the hash entry will
     * be removed.
     * <p>
     * This approach allows us to avoid creating an {@code ActualSymbol} in the
     * common case that the symbol is already interned.
     */
    private static final
    WeakHashMap<String, WeakReference<ActualSymbol>>
        ourActualSymbols = new WeakHashMap<>(256);

    /**
     * Interning table for annotated symbols.
     * <p>
     * Here we use the {@code AnnotatedSymbol} as the key because it needs to
     * capture the base symbol and the annotations, so there's no shortcut like
     * there is with the {@code ActualSymbol} table.
     */
    private static final
    WeakHashMap<AnnotatedSymbol, WeakReference<AnnotatedSymbol>>
        ourAnnotatedSymbols = new WeakHashMap<>(256);


    /**
     * @param value must not be empty but may be null to make
     * {@code null.symbol}.
     *
     * @return not null.
     */
    static BaseSymbol makeSymbol(Evaluator eval, String value)
    {
        return BaseSymbol.internSymbol(value);
    }


    private static BaseSymbol annotate(BaseSymbol unannotated,
                                       String[] annotations)
    {
        assert ! (unannotated instanceof AnnotatedSymbol);

        if (annotations.length == 0) return unannotated;

        AnnotatedSymbol sym = new AnnotatedSymbol(annotations, unannotated);

        synchronized (ourAnnotatedSymbols)
        {
            WeakReference<AnnotatedSymbol> ref = ourAnnotatedSymbols.get(sym);
            if (ref != null)
            {
                // There's a chance that the entry for a string will exist but
                // the weak reference has been cleared.
                AnnotatedSymbol interned = ref.get();
                if (interned != null) return interned;
            }

            // We don't have an interned symbol, so intern the one we've made.
            ref = new WeakReference<>(sym);
            ourAnnotatedSymbols.put(sym, ref);
        }

        return sym;
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.symbol}.
     *
     * @return not null.
     */
    static BaseSymbol makeSymbol(Evaluator eval,
                                 String[]  annotations,
                                 String    value)
    {
        BaseSymbol base = makeSymbol(eval, value);
        return annotate(base, annotations);
    }


    /**
     * @param fusionSymbol must be a Fusion symbol.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseSymbol unsafeSymbolAnnotate(Evaluator eval,
                                           Object fusionSymbol,
                                           String[] annotations)
    {
        BaseSymbol base = (BaseSymbol) fusionSymbol;
        return base.annotate(eval, annotations);
    }


    //========================================================================
    // Predicates


    public static boolean isSymbol(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseSymbol);
    }

    static boolean isSymbol(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseSymbol);
    }


    //========================================================================
    // Conversions


    /**
     * @param fusionSymbol must be a Fusion symbol.
     *
     * @return null if given {@code null.symbol}.
     */
    static String unsafeSymbolToJavaString(Evaluator eval, Object fusionSymbol)
        throws FusionException
    {
        return ((BaseSymbol) fusionSymbol).stringValue();
    }


    /**
     * Converts a Fusion symbol to a {@link String}.
     *
     * @return null if the value isn't a Fusion symbol.
     */
    static String symbolToJavaString(Evaluator eval, Object value)
        throws FusionException
    {
        if (isSymbol(eval, value))
        {
            return unsafeSymbolToJavaString(eval, value);
        }
        return null;
    }


    static String[] unsafeSymbolsToJavaStrings(Evaluator eval,
                                               Object[]  fusionSymbols)
        throws FusionException

    {
        return BaseSymbol.unsafeSymbolsToJavaStrings(fusionSymbols);
    }


    //========================================================================
    // Procedure Helpers

    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static String checkSymbolArg(Evaluator eval,
                                 Procedure who,
                                 String    expectation,
                                 int       argNum,
                                 Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseSymbol)
        {
            return ((BaseSymbol) arg).stringValue();
        }

        throw who.argFailure(expectation, argNum, args);
    }


    /**
     * @return may be null
     */
    static String checkNullableSymbolArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable symbol";
        return checkSymbolArg(eval, who, expectation, argNum, args);
    }


    /**
     * @return not null
     */
    static String checkRequiredSymbolArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null symbol";
        String result = checkSymbolArg(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argFailure(expectation, argNum, args);
        }
        return result;
    }


    /**
     * @deprecated Use
     * {@link #checkNullableSymbolArg(Evaluator, Procedure, int, Object...)}.
     */
    @Deprecated
    static String checkNullableArg(Procedure who, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkNullableSymbolArg(null, who, argNum, args);
    }

    /**
     * @deprecated Use
     * {@link #checkRequiredSymbolArg(Evaluator, Procedure, int, Object...)}.
     */
    @Deprecated
    static String checkRequiredArg(Procedure who, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkRequiredSymbolArg(null, who, argNum, args);
    }



    //========================================================================
    // Procedures


    static final class IsSymbolProc
        extends Procedure1
    {
        IsSymbolProc()
        {
            //    "                                                                               |
            super("Determines whether a `value` is of type `symbol`, returning `true` or `false`.",
                  "value");
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isSymbol(eval, arg);
            return makeBool(eval, r);
        }
    }


    static final class ToStringProc
        extends Procedure
    {
        ToStringProc()
        {
            //    "                                                                               |
            super("Converts a `symbol` to a string with the same text. Returns `null.string` when\n"
                + "given `null.symbol`.",
                  "symbol");
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(args);

            String input = checkNullableArg(this, 0, args);
            return makeString(eval, input);
        }
    }
}
