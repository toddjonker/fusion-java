// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.LocalEnvironment.LexicalBinding;
import com.amazon.ion.IonList;
import com.amazon.ion.IonValue;

final class ForListKeyword
    extends KeywordValue
{
    ForListKeyword()
    {
        //    "                                                                               |
        super("((IDENT SEQ-EXPR)) BODY ...+",
              "Iterates the SEQ-EXPR, binding IDENT to each element in turn and evaluating\n" +
              "BODY. Returns a new list of the results.");
    }


    @Override
    SyntaxValue expand(Evaluator eval, Environment env, SyntaxSexp source)
        throws SyntaxFailure
    {
        SyntaxChecker check = check(source);
        check.arityAtLeast(2);

        SyntaxChecker checkBindings =
            check.subformSeq("sequence of bindings", 1);
        SyntaxSequence bindingForms = checkBindings.form();

        final int numBindings = bindingForms.size();
        SyntaxSymbol[] boundNames    = new SyntaxSymbol[numBindings];
        SyntaxValue[]  boundValues   = new SyntaxValue [numBindings];
        SyntaxValue[]  expandedForms = new SyntaxValue [numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            SyntaxChecker checkPair =
                checkBindings.subformSexp("binding pair", i);
            checkPair.arityExact(2);

            boundNames[i] = checkPair.requiredSymbol("bound name", 0);

            SyntaxValue subform = checkPair.requiredForm("bound value", 1);

            // Bound values use the outer lexical environment
            boundValues[i] = subform.expand(eval, env);
        }

        LocalEnvironment bodyEnv = new LocalEnvironment(env, boundNames);
        SyntaxWrap localWrap = new EnvironmentRenameWrap(bodyEnv);

        // Wrap the bound names so they resolve to their own binding.
        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSymbol name = boundNames[i].addWrap(localWrap);
            name.resolve();
            expandedForms[i] = SyntaxSexp.make(bindingForms.get(i).getLocation(),
                                               name,
                                               boundValues[i]);
        }

        bindingForms = SyntaxSexp.make(bindingForms.getLocation(),
                                       expandedForms);

        // Prepare the body.
        expandedForms = new SyntaxValue[source.size()];
        expandedForms[0] = source.get(0);
        expandedForms[1] = bindingForms;

        for (int i = 2; i < source.size(); i++)
        {
            SyntaxValue bodyStx = source.get(i);
            bodyStx = bodyStx.addWrap(localWrap);
            expandedForms[i] = bodyStx.expand(eval, bodyEnv);
        }

        source = SyntaxSexp.make(source.getLocation(), expandedForms);
        return source;
    }


    //========================================================================


    @Override
    CompiledForm compile(Evaluator eval, Environment env, SyntaxSexp forStx)
        throws FusionException
    {
        SyntaxSequence bindingForms = (SyntaxSequence) forStx.get(1);

        final int numBindings = bindingForms.size();

        LexicalBinding[] bindings = new LexicalBinding[numBindings];
        CompiledForm[] valueForms = new CompiledForm[numBindings];

        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(i);
            SyntaxSymbol boundIdentifier = (SyntaxSymbol) binding.get(0);
            bindings[i] = (LexicalBinding) boundIdentifier.resolve();

            SyntaxValue boundExpr = binding.get(1);
            valueForms[i] = eval.compile(env, boundExpr);
        }

        // Dummy environment to keep track of depth
        env = new LocalEnvironment(env, SyntaxSymbol.EMPTY_ARRAY);

        CompiledForm body = BeginKeyword.compile(eval, env, forStx, 2);

        return new CompiledForList(bindings, valueForms, body);
    }


    //========================================================================


    private final class CompiledForList
        implements CompiledForm
    {
        // TODO FUSION-48 don't retain bindings
        private final LexicalBinding[] myBindings;
        private final CompiledForm[]   myValueForms;
        private final CompiledForm     myBody;

        CompiledForList(LexicalBinding[] bindings, CompiledForm[] valueForms,
                        CompiledForm body)
        {
            myBindings   = bindings;
            myValueForms = valueForms;
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            final int numBindings = myBindings.length;

            IonList result = eval.getSystem().newEmptyList();

            if (numBindings != 0)
            {
                Stream[] streams = new Stream[numBindings];

                for (int i = 0; i < numBindings; i++)
                {
                    CompiledForm form = myValueForms[i];
                    Object boundValue = eval.eval(store, form);
                    streams[i] = Sequences.streamFor(boundValue);
                }

                // TODO FUSION-49 separate Store from Environment
                FusionValue[] boundValues = new FusionValue[numBindings];
                LocalEnvironment bodyEnv =
                    new LocalEnvironment((Environment) store,
                                         myBindings, boundValues);

                while (Sequences.allHaveNext(streams))
                {
                    // Determine the next round of bound values
                    for (int i = 0; i < numBindings; i++)
                    {
                        Stream s = streams[i];
                        bodyEnv.bind(i, s.next());
                    }

                    Object nextResult = eval.eval(bodyEnv, myBody);
                    IonValue value = toIonValue(nextResult);
                    if (value != null)
                    {
                        AddProc.invoke(result, value);
                    }
                    else
                    {
                        throw contractFailure("body returned non-Ion value: "
                            + writeToString(nextResult));
                    }
                }
            }
            return new DomValue(result);
        }
    }
}
