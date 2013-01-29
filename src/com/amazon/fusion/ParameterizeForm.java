// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionWrite.safeWriteToString;


final class ParameterizeForm
    extends SyntacticForm
{
    ParameterizeForm()
    {
        //    "                                                                               |
        super("((PARAM EXPR) ...) BODY ...+",
              "Dynamically binds the PARAMs to the EXPR values while evaluating the BODY.\n" +
              "The PARAMs are evaluated first, in order; each must result in a dynamic\n" +
              "parameter procedure. The EXPRs are then evaluated in order, and then the params\n" +
              "are changed to their results for the dynamic extent of the BODY.\n" +
              "BODY may be one or more forms; the result of the last form is the result of the\n" +
              "entire expression.");
    }


    @Override
    SyntaxValue expand(Evaluator eval, Expander ctx, Environment env,
                       SyntaxSexp source)
        throws FusionException
    {
        SyntaxChecker check = check(source);
        final int exprSize = check.arityAtLeast(3);

        SyntaxChecker checkBindings =
            check.subformSeq("sequence of parameterizations", 1);
        SyntaxSequence bindingForms = checkBindings.form();

        final int numBindings = bindingForms.size();
        SyntaxValue[] expandedForms = new SyntaxValue[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            SyntaxChecker checkPair =
                checkBindings.subformSexp("parameter/value pair", i);
            checkPair.arityExact(2);

            SyntaxSexp binding = (SyntaxSexp) checkPair.form();

            SyntaxValue paramExpr = binding.get(0);
            paramExpr = ctx.expand(env, paramExpr);

            SyntaxValue boundExpr = binding.get(1);
            boundExpr = ctx.expand(env, boundExpr);

            binding = SyntaxSexp.make(binding.getLocation(),
                                      paramExpr, boundExpr);
            expandedForms[i] = binding;
        }

        bindingForms = SyntaxSexp.make(bindingForms.getLocation(),
                                       expandedForms);

        // Expand the body expressions
        expandedForms = new SyntaxValue[exprSize];
        expandedForms[0] = source.get(0);
        expandedForms[1] = bindingForms;

        for (int i = 2; i < exprSize; i++)
        {
            SyntaxValue bodyExpr = source.get(i);
            expandedForms[i] = ctx.expand(env, bodyExpr);
        }

        source = SyntaxSexp.make(source.getLocation(), expandedForms);
        return source;
    }


    //========================================================================


    @Override
    CompiledForm compile(Evaluator eval, Environment env, SyntaxSexp source)
        throws FusionException
    {
        SyntaxSexp bindingForms = (SyntaxSexp) source.get(1);

        final int numBindings = bindingForms.size();

        CompiledForm[] parameterForms = new CompiledForm[numBindings];
        CompiledForm[] valueForms     = new CompiledForm[numBindings];

        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(i);

            SyntaxValue paramExpr = binding.get(0);
            parameterForms[i] = eval.compile(env, paramExpr);

            SyntaxValue valueExpr = binding.get(1);
            valueForms[i] = eval.compile(env, valueExpr);
        }

        CompiledForm body = BeginForm.compile(eval, env, source, 2);

        return new CompiledParameterize(parameterForms, valueForms, body);
    }


    //========================================================================


    private final class CompiledParameterize
        implements CompiledForm
    {
        private final CompiledForm[] myParameterForms;
        private final CompiledForm[] myValueForms;
        private final CompiledForm   myBody;

        CompiledParameterize(CompiledForm[] parameterForms,
                             CompiledForm[] valueForms,
                             CompiledForm   body)
        {
            myParameterForms = parameterForms;
            myValueForms     = valueForms;
            myBody           = body;
        }


        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            final int numBindings = myParameterForms.length;

            DynamicParameter[] parameters = new DynamicParameter[numBindings];
            for (int i = 0; i < numBindings; i++)
            {
                CompiledForm paramForm = myParameterForms[i];
                Object paramValue = eval.eval(store, paramForm);
                try
                {
                    parameters[i] = (DynamicParameter) paramValue;
                }
                catch (ClassCastException e)
                {
                    String message =
                        "Parameter expression evaluated to non-parameter: " +
                        safeWriteToString(eval, paramValue);
                    throw contractFailure(message);
                }
            }

            Object[] boundValues = new Object[numBindings];
            for (int i = 0; i < numBindings; i++)
            {
                CompiledForm valueForm = myValueForms[i];
                Object value = eval.eval(store, valueForm);
                boundValues[i] = value;
            }

            Evaluator bodyEval = eval.markedContinuation(parameters, boundValues);

            // TODO tail recursion
            Object result = bodyEval.eval(store, myBody);
            return result;
        }
    }
}
