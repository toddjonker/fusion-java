// Copyright (c) 2012-2016 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.FusionSymbol.BaseSymbol;

/**
 * Bindings are used during expansion and compilation to identify a specific
 * binding site.
 * They are compiled away and are not used at eval-time.
 */
abstract class Binding
{
    abstract BaseSymbol getName();

    /**
     * Determines whether this is a {@link FreeBinding} with the given name.
     * This implementation returns false.
     *
     * @param name must be interned.
     */
    boolean isFree(BaseSymbol name)
    {
        return false;
    }

    /**
     * Gets the original binding to which this binding refers.
     * Free, local, and module-level bindings are always themselves original.
     * For imported bindings, the original is the target module-level binding.
     * The original of a top-level binding can be either its local definition
     * or an imported module-level binding (and it can change over time).
     *
     * @return not null.
     */
    Binding originalBinding()
    {
        return this;
    }

    /**
     * Determines whether two bindings refer to the same
     * {@link #originalBinding()}, despite any renames on import or export.
     *
     * @param other must not be null.
     */
    boolean sameTarget(Binding other)
    {
        return originalBinding() == other.originalBinding();
    }

    /**
     * Don't call directly! Second half of double-dispatch from
     * {@link Namespace#lookup(Binding)}.
     *
     * @return null if there's no value associated with the binding.
     */
    abstract Object lookup(Namespace ns);


    CompiledForm compileDefine(Evaluator eval,
                               Environment env,
                               SyntaxSymbol id,
                               CompiledForm valueForm)
        throws FusionException
    {
        throw new IllegalStateException("Unexpected `define` context.");
    }


    /** Compile a reference to the variable denoted by this binding. */
    abstract CompiledForm compileReference(Evaluator eval,
                                           Environment env)
        throws FusionException;

    /** Compile a #%top reference. */
    abstract CompiledForm compileTopReference(Evaluator eval,
                                              Environment env,
                                              SyntaxSymbol id)
        throws FusionException;

    /** Compile a mutation of the variable denoted by this binding. */
    abstract CompiledForm compileSet(Evaluator eval,
                                     Environment env,
                                     CompiledForm valueForm)
        throws FusionException;
}
