// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

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

    Object findProperty(Evaluator eval, Object key)
        throws FusionException
    {
        for (int i = 0; i < myProperties.length; i += 2)
        {
            if (FusionCompare.isSame(eval, key, myProperties[i]).isTrue())
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
            if (FusionCompare.isSame(eval, key, myProperties[i]).isTrue())
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
