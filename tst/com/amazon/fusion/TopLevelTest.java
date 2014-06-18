// Copyright (c) 2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 */
public class TopLevelTest
    extends CoreTestCase
{
    @Test
    public void testLookupFromImport()
        throws Exception
    {
        TopLevel top = topLevel();
        Object fv = top.lookup("pair");
        assertTrue(FusionProcedure.isProcedure(top, fv));
    }

    @Test
    public void testLookupNoBinding()
        throws Exception
    {
        TopLevel top = topLevel();
        Object fv = top.lookup("no binding!");
        assertNull(fv);
    }

    @Test
    public void testDefineAndLookup()
        throws Exception
    {
        TopLevel top = topLevel();

        top.define("v", 12);
        Object fv = top.lookup("v");
        assertTrue(FusionNumber.isInt(top, fv));

        top.define("A questionable name", null);
        fv = top.lookup("A questionable name");
        assertTrue(FusionVoid.isVoid(top, fv));
    }

    @Test
    public void testCallProcByValue()
        throws Exception
    {
        TopLevel top = topLevel();
        Object plus = top.lookup("+");
        assertTrue(FusionProcedure.isProcedure(top, plus));
        checkLong(3, top.call(plus, 1, 2));
    }
}
