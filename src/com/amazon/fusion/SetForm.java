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
    SyntaxValue expand(Evaluator eval, ExpandContext ctx, Environment env,
                       SyntaxSexp source)
        throws FusionException
    {
        SyntaxChecker check = check(source);
        check.arityExact(3);

        SyntaxSymbol id = check.requiredIdentifier("variable identifier", 1);
        Binding binding = id.resolve();
        if (binding instanceof FreeBinding)
        {
            throw check.failure("variable has no binding", id);
        }

        SyntaxValue[] children = source.extract();
        SyntaxValue valueExpr = source.get(2);
        children[2] = eval.expand(ctx, env, valueExpr);

        source = SyntaxSexp.make(source.getLocation(), children);
        return source;
    }


    @Override
    CompiledForm compile(Evaluator eval, Environment env, SyntaxSexp source)
        throws FusionException
    {
        CompiledForm valueForm = eval.compile(env, source.get(2));

        SyntaxSymbol id = (SyntaxSymbol) source.get(1);
        Binding binding = id.resolve();

        return binding.compileSet(eval, env, valueForm);
    }
}
