// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionCompare.isSame;
import static com.amazon.fusion.FusionSexp.emptySexp;
import static com.amazon.fusion.FusionSexp.pair;
import com.amazon.ion.IonValue;
import java.util.Arrays;

/**
 * Models Fusion source code, using a custom DOM implementation of Ion.
 * Unlike the {@link IonValue} model, this one allows sharing of nodes in a
 * DAG structure.
 */
abstract class SyntaxValue
    extends BaseValue
{
    /** A zero-length array. */
    final static SyntaxValue[] EMPTY_ARRAY = new SyntaxValue[0];

    // TODO WORKAROUND FUSION-47 Should use interned symbol
    // We use 'new String' to ensure uniqueness of the object identity.
    static final Object STX_PROPERTY_ORIGIN = new String("origin");

    private final SourceLocation mySrcLoc;

    /** Not null, to streamline things. */
    private final Object[] myProperties;


    /**
     * @param loc may be null.
     * @param properties must not be null.
     */
    SyntaxValue(SourceLocation loc, Object[] properties)
    {
        assert properties != null;
        mySrcLoc = loc;
        myProperties = properties;
    }


    /**
     * Determines whether the wrapped datum is a null value.
     */
    @Override
    abstract boolean isAnyNull();


    /**
     * Gets the location associated with this syntax node, if it exists.
     * @return may be null.
     */
    SourceLocation getLocation()
    {
        return mySrcLoc;
    }


    Object[] getProperties()
    {
        return myProperties;
    }

    /**
     * @param key must not be null.
     * @return void if no value is associated with the key.
     */
    Object findProperty(Evaluator eval, Object key)
        throws FusionException
    {
        for (int i = 0; i < myProperties.length; i += 2)
        {
            if (isSame(eval, key, myProperties[i]).isTrue())
            {
                return myProperties[i + 1];
            }
        }
        return FusionVoid.voidValue(eval);
    }


    abstract SyntaxValue copyReplacingProperties(Object[] properties);


    SyntaxValue copyWithProperty(Evaluator eval, Object key, Object value)
        throws FusionException
    {
        // Determine whether the property already exists so we can replace it.
        int length = myProperties.length;
        for (int i = 0; i < length; i += 2)
        {
            if (isSame(eval, key, myProperties[i]).isTrue())
            {
                Object[] newProperties = Arrays.copyOf(myProperties, length);
                newProperties[i + 1] = value;
                return copyReplacingProperties(newProperties);
            }
        }

        Object[] newProperties = Arrays.copyOf(myProperties, length + 2);
        newProperties[length    ] = key;
        newProperties[length + 1] = value;
        return copyReplacingProperties(newProperties);
    }


    final SyntaxValue trackOrigin(Evaluator    eval,
                                  SyntaxValue  origStx,
                                  SyntaxSymbol origin)
        throws FusionException
    {
        Object[] oProps = origStx.myProperties;

        // Reserve space for origin, in case either list has it yet.
        int maxLen = oProps.length + myProperties.length + 2;
        Object[] merged = new Object[maxLen];
        int m = 0;

        for (int i = 0; i < myProperties.length; i += 2)
        {
            Object k = myProperties[i];
            Object v = myProperties[i + 1];

            for (int j = 0; j < oProps.length; j += 2)
            {
                if (isSame(eval, k, oProps[j]).isTrue())
                {
                    Object o = oProps[j + 1];
                    if (k == STX_PROPERTY_ORIGIN)
                    {
                        assert origin != null;
                        o = pair(eval, origin, o);
                        origin = null;
                    }
                    v = pair(eval, v, o);
                    break;
                }
            }

            if (origin != null && k == STX_PROPERTY_ORIGIN)
            {
                Object o = emptySexp(eval);
                o = pair(eval, origin, o);
                v = pair(eval, v, o);
                origin = null;
            }

            merged[m++] = k;
            merged[m++] = v;
        }

        // Copy what remains from the original properties.
        pass2:
        for (int i = 0; i < oProps.length; i += 2)
        {
            Object k = oProps[i];
            Object v = oProps[i + 1];

            for (int j = 0; j < myProperties.length; j += 2)
            {
                if (isSame(eval, k, myProperties[j]).isTrue())
                {
                    continue pass2;
                }
            }

            if (origin != null && k == STX_PROPERTY_ORIGIN)
            {
                v = pair(eval, origin, v);
                origin = null;
            }

            merged[m++] = k;
            merged[m++] = v;
        }

        // We haven't found origin in either list, so add it.
        if (origin != null)
        {
            Object v = emptySexp(eval);
            v = pair(eval, origin, v);

            merged[m++] = STX_PROPERTY_ORIGIN;
            merged[m++] = v;
        }

        // Remove empty space at the end.
        if (merged.length != m)
        {
            merged = Arrays.copyOf(merged, m);
        }

        return copyReplacingProperties(merged);
    }


    /**
     * Prepends a wrap onto our existing wraps.
     * This will return a new instance as necessary to preserve immutability.
     */
    SyntaxValue addWrap(SyntaxWrap wrap)
        throws FusionException
    {
        return this;
    }

    /**
     * Prepends a sequence of wraps onto our existing wraps.
     * This will return a new instance as necessary to preserve immutability.
     */
    SyntaxValue addWraps(SyntaxWraps wraps)
        throws FusionException
    {
        return this;
    }


    SyntaxValue addOrRemoveMark(int mark)
        throws FusionException
    {
        SyntaxWrap wrap = new MarkWrap(mark);
        return addWrap(wrap);
    }


    /**
     * Removes any wraps from this value and any children.
     * @return an equivalent syntax value with no wraps.
     * May return this instance when that's already the case.
     */
    SyntaxValue stripWraps(Evaluator eval)
        throws FusionException
    {
        return this;
    }


    /** Don't call directly! Go through the evaluator. */
    SyntaxValue doExpand(Expander expander, Environment env)
        throws FusionException
    {
        return this;
    }


    /** Don't call directly! Go through the evaluator. */
    abstract CompiledForm doCompile(Evaluator eval, Environment env)
        throws FusionException;


    /**
     * Unwraps syntax, returning plain values. Only one layer is unwrapped, so
     * if this is a container, the result will contain syntax objects.
     */
    abstract Object unwrap(Evaluator eval)
        throws FusionException;


    /**
     * Unwraps syntax recursively, returning plain values.
     * Used by `quote` and `synatax_to_datum`.
     */
    abstract Object syntaxToDatum(Evaluator eval)
        throws FusionException;


    @Override
    SyntaxValue datumToSyntaxMaybe(Evaluator      eval,
                                   SyntaxSymbol   context,
                                   SourceLocation loc)
        throws FusionException
    {
        return this;
    }


    @Override
    SyntaxValue wrapAsSyntax(Evaluator eval, SourceLocation loc)
    {
        throw new IllegalStateException("Cannot wrap syntax as syntax");
    }
}
