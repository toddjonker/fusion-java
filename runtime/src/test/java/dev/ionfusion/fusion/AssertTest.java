// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

import dev.ionfusion.runtime.base.FusionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * These checks can't be ported to Fusion yet because there's no way to inspect an
 * exception's message.
 */
public class AssertTest
    extends CoreTestCase
{
    private void expectAssertFailure(String expr)
        throws Exception
    {
        try
        {
            eval("(assert " + expr + ")");
            Assertions.fail("Expected exception");
        }
        catch (FusionAssertionException e)
        {
            Assertions.assertEquals(null, e.getUserMessage());
        }

        try
        {
            eval("(assert " + expr + " \"barney\")");
            Assertions.fail("Expected exception");
        }
        catch (FusionAssertionException e)
        {
            Assertions.assertEquals("barney", e.getUserMessage());
        }

        try
        {
            eval("(assert " + expr + " \"barney\" 13)");
            Assertions.fail("Expected exception");
        }
        catch (FusionAssertionException e)
        {
            Assertions.assertEquals("barney13", e.getUserMessage());
        }
    }

    @Test
    public void testAssertFailure()
        throws Exception
    {
        for (String form : BooleanTest.UNTRUTHY_EXPRESSIONS)
        {
            expectAssertFailure(form);
        }
    }


    @Test
    public void expressionCodeIsWrittenToExceptionMessage()
    {
        // Whitespace and comments should be absorbed.
        String expr = "(assert (= 3 \n   (+  /*omitted*/ 1 1)) '''oops''')";
        FusionException e = assertEvalThrows(FusionAssertionException.class, expr);
        assertThat(e.getMessage(),
                   containsString("Expression: (= 3 (+ 1 1))\n"));
    }


    @Test
    public void assertFormIsOnTopOfStackTrace()
    {
        String expr = "(begin \n" +
                      "  (assert (void) \"msg\"))";

        FusionException e = assertEvalThrows(FusionAssertionException.class, expr);
        assertThat(e.getMessage(),
                   endsWith("{{{void}}}\n" +
                            "  ...at 2nd line, 3rd column\n" +
                            "  ...at 1st line, 1st column"));
    }
}
