// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

final class SetForm
    extends SyntacticForm
{
    SetForm()
    {
        super("VAR VALUE",
              "Mutates the given variable, assigning it the VALUE.");
    }


    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        SyntaxChecker check = check(stx);
        check.arityExact(3);

        SyntaxSymbol id = check.requiredIdentifier("variable identifier", 1);
        Binding binding = id.resolve();
        if (binding instanceof FreeBinding)
        {
            throw check.failure("variable has no binding", id);
        }

        SyntaxValue[] children = stx.extract();
        SyntaxValue valueExpr = stx.get(2);
        children[2] = expander.expandExpression(env, valueExpr);

        stx = SyntaxSexp.make(expander, stx.getLocation(), children);
        return stx;
    }


    @Override
    CompiledForm compile(Evaluator eval, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        CompiledForm valueForm = eval.compile(env, stx.get(2));

        SyntaxSymbol id = (SyntaxSymbol) stx.get(1);
        Binding binding = id.resolve();

        return binding.compileSet(eval, env, valueForm);
    }
}
