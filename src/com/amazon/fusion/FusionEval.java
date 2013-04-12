// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.Syntax.datumToSyntaxMaybe;
import static com.amazon.fusion.Syntax.isSyntax;


final class FusionEval
{
    private FusionEval() {}


    private static SyntaxValue topLevelStx(Evaluator eval,
                                           Object topLevelForm,
                                           boolean enrichSyntaxObject,
                                           String whosCalling)
        throws FusionException
    {
        SyntaxValue stx;
        if (Syntax.isSyntax(eval, topLevelForm))
        {
            stx = (SyntaxValue) topLevelForm;
            if (enrichSyntaxObject)
            {
                stx = enrich(eval, stx);
            }
        }
        else
        {
            stx = datumToSyntaxMaybe(eval, topLevelForm, null);
            if (topLevelForm == null)
            {
                throw new ArgTypeFailure(whosCalling,
                                         "Syntax object or Ionizable data",
                                         0, topLevelForm);
            }
            stx = enrich(eval, stx);
        }

        return stx;
    }

    /**
     * The default evaluation handler, evaluating the given source
     * within the current namespace.
     *
     * @param topLevelForm is not enriched with lexical information if it is
     *  a syntax object.
     *
     * @see <a href="http://docs.racket-lang.org/reference/eval.html#%28def._%28%28quote._~23~25kernel%29._current-eval%29%29">
         Racket's <code>eval</code></a>
     */
    @SuppressWarnings("javadoc")
    private static Object defaultEval(Evaluator eval, Object topLevelForm)
        throws FusionException
    {
        SyntaxValue stx =
            topLevelStx(eval, topLevelForm, false, "default_eval_handler");

        Namespace ns = eval.findCurrentNamespace();

        {
            // TODO FUSION-33 this should partial-expand and splice begins
            Expander expander = new Expander(eval);
            stx = expander.expand(ns, stx);
        }

        CompiledForm compiled = eval.compile(ns, stx);
        stx = null; // Don't hold garbage

        return eval.eval(ns, compiled); // TODO TAIL
    }


    /**
     * Placeholder so we can later add current-eval parameter.
     */
    static Object callCurrentEval(Evaluator eval, Object topLevelForm)
        throws FusionException
    {
        return defaultEval(eval, topLevelForm);
    }


    /**
     * Expands, compiles, and evaluates a single top-level form.
     * <p>
     * Equivalent to Racket's {@code eval} (but incomplete at the moment.)
     *
     * @param ns may be null to use {@link Evaluator#findCurrentNamespace()}.
     * @param topLevelForm will be enriched.
     */
    static Object eval(Evaluator eval, Object topLevelForm, Namespace ns)
        throws FusionException
    {
        eval = eval.parameterizeCurrentNamespace(ns);

        if (isSyntax(eval, topLevelForm))
        {
            topLevelForm = enrich(eval, (SyntaxValue) topLevelForm);
        }

        return callCurrentEval(eval, topLevelForm); // TODO TAIL
    }


    /**
     * Like {@link #eval(Evaluator, Object, Namespace)},
     * but does not enrich the source's lexical context.
     *
     * @param ns may be null to use {@link Evaluator#findCurrentNamespace()}.
     */
    static Object evalSyntax(Evaluator eval, SyntaxValue source, Namespace ns)
        throws FusionException
    {
        eval = eval.parameterizeCurrentNamespace(ns);

        return callCurrentEval(eval, source); // TODO TAIL
    }


    /**
     * Enriches a syntax object "in the same way as eval", using the current
     * namespace.
     */
    private static SyntaxValue enrich(Evaluator eval, SyntaxValue topLevelForm)
    {
        Namespace ns = eval.findCurrentNamespace();

        // Handle (module ...) such that we don't push bindings into the body.
        if (topLevelForm instanceof SyntaxSexp)
        {
            SyntaxSexp maybeModule = (SyntaxSexp) topLevelForm;
            if (maybeModule.size() > 1 &&
                maybeModule.get(0) instanceof SyntaxSymbol)
            {
                SyntaxSymbol maybeKeyword = (SyntaxSymbol) maybeModule.get(0);
                maybeKeyword = (SyntaxSymbol) ns.syntaxIntroduce(maybeKeyword);
                SyntaxSymbol moduleKeyword =
                    eval.makeKernelIdentifier("module");
                if (maybeKeyword.freeIdentifierEqual(moduleKeyword))
                {
                    SyntaxValue[] children = maybeModule.extract();
                    children[0] = maybeKeyword;
                    return SyntaxSexp.make(eval, children);
                }
            }
        }

        topLevelForm = ns.syntaxIntroduce(topLevelForm);
        return topLevelForm;
    }


    //========================================================================


    static final class ExpandProc
        extends Procedure1
    {
        ExpandProc()
        {
            //    "                                                                               |
            super("Expands a top-level form to core syntax, using the bindings of the current\n" +
                  "namespace.\n" +
                  "\n" +
                  "The `top_level_form` may be a syntax object or another datum.",
                  "top_level_form");
        }

        /**
         * @see FusionEval#eval(Evaluator, Object, Namespace)
         */
        @Override
        Object doApply(Evaluator eval, Object arg0)
            throws FusionException
        {
            SyntaxValue topLevelForm =
                topLevelStx(eval, arg0, true, identify());

            Namespace ns = eval.findCurrentNamespace();
            Expander expander = new Expander(eval);
            topLevelForm = expander.expand(ns, topLevelForm);

            return topLevelForm;
        }
    }


    static final class EvalProc
        extends Procedure
    {
        EvalProc()
        {
            //    "                                                                               |
            super("Evaluates a `top_level_form` within a `namespace`.  If `namespace` is absent\n" +
                  "then the [`current_namespace`](namespace.html#current_namespace) parameter is\n" +
                  "used.\n" +
                  "\n" +
                  "The `top_level_form` must be a valid top-level syntactic form with respect to\n" +
                  "the bindings visible in the namespace.  The form is expanded, compiled, and\n" +
                  "evaluated, and its result is returned.  Any side effects made to the namespace\n" +
                  "will be visible to later evaluations.",
                  "top_level_form", "[namespace]");
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityRange(1, 2, args);

            Namespace ns = null;
            if (args.length == 2)
            {
                if (args[1] instanceof Namespace)
                {
                    ns = (Namespace) args[1];
                }
                else
                {
                    throw argFailure("namespace", 1, args);
                }
            }

            return FusionEval.eval(eval, args[0], ns);  // TODO TAIL
        }
    }
}
