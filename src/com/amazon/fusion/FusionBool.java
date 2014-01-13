// Copyright (c) 2013-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionValue.evaluator;
import static com.amazon.fusion.SimpleSyntaxValue.makeSyntax;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import java.io.IOException;


/**
 * Utilities for working with Fusion {@code bool} values.
 *
 * @see FusionValue
 */
public final class FusionBool
{
    private FusionBool() {}


    //========================================================================
    // Representation Classes


    abstract static class BaseBool
        extends BaseValue
    {
        private BaseBool() {}

        abstract boolean isTrue();
        abstract boolean isFalse();
        abstract Boolean asJavaBoolean();

        @Override
        SyntaxValue toStrippedSyntaxMaybe(Evaluator eval)
        {
            return makeSyntax(eval, /*location*/ null, this);
        }
    }


    private static class NullBool
        extends BaseBool
    {
        private NullBool() {}

        @Override
        boolean isAnyNull() { return true; }

        @Override
        boolean isTrue()    { return false; }

        @Override
        boolean isFalse()   { return false; }

        @Override
        BaseBool isTruthy(Evaluator eval)  { return FALSE_BOOL; }

        @Override
        BaseBool not(Evaluator eval) { return TRUE_BOOL; }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof BaseBool
                         && ((BaseBool) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        Boolean asJavaBoolean() { return null; }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newNullBool();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeNull(IonType.BOOL);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null.bool");
        }
    }


    private static class TrueBool
        extends BaseBool
    {
        private TrueBool() {}

        @Override
        boolean isAnyNull() { return false; }

        @Override
        boolean isTrue()    { return true; }

        @Override
        boolean isFalse()   { return false; }

        @Override
        BaseBool isTruthy(Evaluator eval)  { return TRUE_BOOL; }

        @Override
        BaseBool not(Evaluator eval) { return FALSE_BOOL; }

        @Override
        Boolean asJavaBoolean() { return TRUE; }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseBool)
            {
                if (((BaseBool) right).isTrue())
                {
                    return TRUE_BOOL;
                }
            }

            return FALSE_BOOL;
        }


        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newBool(true);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeBool(true);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("true");
        }
    }


    private static class FalseBool
        extends BaseBool
    {
        private FalseBool() {}

        @Override
        boolean isAnyNull() { return false; }

        @Override
        boolean isTrue()    { return false; }

        @Override
        boolean isFalse()   { return true; }

        @Override
        BaseBool isTruthy(Evaluator eval)  { return FALSE_BOOL; }

        @Override
        BaseBool not(Evaluator eval) { return TRUE_BOOL; }

        @Override
        Boolean asJavaBoolean() { return FALSE; }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseBool)
            {
                if (((BaseBool) right).isFalse())
                {
                    return TRUE_BOOL;
                }
            }

            return FALSE_BOOL;
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newBool(false);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeBool(false);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("false");
        }
    }


    private static final class AnnotatedBool
        extends BaseBool
        implements Annotated
    {
        /** Not null or empty */
        final String[] myAnnotations;

        /** Not null, and not AnnotatedBool */
        final BaseBool  myValue;

        private AnnotatedBool(String[] annotations, BaseBool value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public String[] annotationsAsJavaStrings()
        {
            return myAnnotations;
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

        @Override
        boolean isTrue()    { return myValue.isTrue(); }

        @Override
        boolean isFalse()   { return myValue.isFalse(); }

        @Override
        BaseBool not(Evaluator eval) { return myValue.not(eval); }

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
        Boolean asJavaBoolean() { return myValue.asJavaBoolean(); }

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


    private static final BaseBool NULL_BOOL  = new NullBool();
    private static final TrueBool TRUE_BOOL  = new TrueBool();
    private static final FalseBool FALSE_BOOL = new FalseBool();


    static TrueBool trueBool(Evaluator eval)
    {
        return TRUE_BOOL;
    }

    static FalseBool falseBool(Evaluator eval)
    {
        return FALSE_BOOL;
    }


    static BaseBool makeBool(Evaluator eval, boolean value)
    {
        return (value ? TRUE_BOOL : FALSE_BOOL);
    }


    /**
     * @param value may be null to make {@code null.string}.
     *
     * @return not null.
     */
    static BaseBool makeBool(Evaluator eval, Boolean value)
    {
        return (value == null ? NULL_BOOL : makeBool(eval, (boolean) value));
    }


    private static BaseBool annotate(BaseBool unannotated,
                                     String[] annotations)
    {
        assert ! (unannotated instanceof AnnotatedBool);

        if (annotations.length == 0) return unannotated;

        return new AnnotatedBool(annotations, unannotated);
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseBool makeBool(Evaluator eval,
                             String[]  annotations,
                             boolean   value)
    {
        BaseBool base = makeBool(eval, value);
        return annotate(base, annotations);
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.string}.
     *
     * @return not null.
     */
    static BaseBool makeBool(Evaluator eval,
                             String[]  annotations,
                             Boolean   value)
    {
        BaseBool base = makeBool(eval, value);
        return annotate(base, annotations);
    }


    /**
     * @param fusionBool must be a Fusion bool.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseBool unsafeBoolAnnotate(Evaluator eval,
                                       Object    fusionBool,
                                       String[]  annotations)
    {
        BaseBool base = (BaseBool) fusionBool;
        if (base instanceof AnnotatedBool)
        {
            base = ((AnnotatedBool) base).myValue;
        }
        return annotate(base, annotations);
    }


    //========================================================================
    // Predicates


    /**
     * Determines whether a Fusion value has type {@code bool}.
     */
    public static boolean isBool(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseBool);
    }

    static boolean isBool(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseBool);
    }


    /**
     * Determines whether a Fusion {@code bool} value is {@code true}.
     *
     * @param value must be a Fusion {@code bool}.
     */
    static boolean unsafeBoolIsTrue(Evaluator eval, Object value)
        throws FusionException
    {
        BaseBool b = (BaseBool) value;
        return b.isTrue();
    }

    /**
     * Determines whether a Fusion value is {@code true}.
     */
    public static boolean isTrue(TopLevel top, Object value)
        throws FusionException
    {
        return isTrue(evaluator(top), value);
    }

    static boolean isTrue(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseBool)
        {
            BaseBool b = (BaseBool) value;
            return b.isTrue();
        }
        return false;
    }


    /**
     * Determines whether a Fusion value is {@code false}.
     */
    public static boolean isFalse(TopLevel top, Object value)
        throws FusionException
    {
        return isFalse(evaluator(top), value);
    }

    static boolean isFalse(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseBool)
        {
            BaseBool b = (BaseBool) value;
            return b.isFalse();
        }

        return false;
    }


    //========================================================================
    // Conversions


    /**
     * @param fusionBool must be a Fusion bool.
     *
     * @return null if given {@code null.bool}.
     */
    static Boolean unsafeBoolToJavaBoolean(Evaluator eval, Object fusionBool)
    {
        return ((BaseBool) fusionBool).asJavaBoolean();
    }


    /**
     * Converts a Fusion bool to a Java {@link Boolean}.
     *
     * @return null if the value isn't Fusion true or false.
     */
    static Boolean boolToJavaBoolean(Evaluator eval, Object value)
        throws FusionException
    {
        if (isBool(eval, value))
        {
            return unsafeBoolToJavaBoolean(eval, value);
        }
        return null;
    }


    //========================================================================
    // Procedures


    static final class IsBoolProc
        extends Procedure1
    {
        IsBoolProc()
        {
            //    "                                                                               |
            super("Determines whether a `value` is of type `bool`, returning `true` or `false`.",
                  "value");
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isBool(eval, arg);
            return makeBool(eval, r);
        }
    }
}
