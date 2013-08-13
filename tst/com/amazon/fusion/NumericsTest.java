// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;

public class NumericsTest
    extends CoreTestCase
{
    @Before
    public void requires()
        throws FusionException
    {
        topLevel().requireModule("/fusion/math");
        topLevel().requireModule("/fusion/timestamp");
    }

    @Test
    public void testSum()
        throws Exception
    {
        //assertEval(0, "(+)");
        assertBigInt(1, "(+ 1)");
        assertBigInt(2, "(+ 1 1)");
        assertBigInt(40, "(+ 1 19 20)");
        eval("(define ten 10)");
        assertBigInt(20, "(+ ten (+ ten (+ 3 -3)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "8888888888888888888888888888888888888888888888888888888889";
        BigInteger expected = new BigInteger(expectedStr);
        assertEval(expected,"(+ "+bigIntStr+" 1)");

        String largerExpectedStr = "17777777777777777777777777777777777777777777777777777777776";
        BigInteger largerExpected = new BigInteger(largerExpectedStr);
        assertEval(largerExpected,"(+ "+bigIntStr+" "+bigIntStr+")");
    }

    @Test
    public void testSumMixed()
        throws Exception
    {
        assertEval("10.0", "(+ 100d-1)");
        assertEval("10.25", "(+ 100d-1 0.25)");
        assertEval("12.25", "(+ 100d-1 0.25 2)");
        assertEval("12.00", "(+ 100d-1 0.25 2.0 -0.25)");
        eval("(define quarter 0.25)");
        assertEval("10.50", "(+ quarter (+ quarter (+ 0.0 10)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "8888888888888888888888888888888888888888888888888888888888.5";
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEval(expected,"(+ "+bigIntStr+" 0.5)");

        // big decimal test suite
        String bigDecStr = "8888888888888888888888888888888888888888888888888888888888.5";
        String expectedStr2 = "8888888888888888888888888888888888888888888888888888888888.25";
        BigDecimal expected2 = new BigDecimal(expectedStr2);
        assertEval(expected2,"(+ "+bigDecStr+" -0.25)");
    }

    @Test
    public void testSumArgType()
        throws Exception
    {
        expectArgTypeFailure("(+ 10 10e-2)",1);
    }

    @Test
    public void testProduct()
        throws Exception
    {
        assertBigInt(1, "(*)");
        assertBigInt(4, "(* 4)");
        assertBigInt(20, "(* 4 5)");
        assertBigInt(-20, "(* 4 -5)");
        assertBigInt(40, "(* 4 5 2)");
        eval("(define ten 10)");
        assertBigInt(11, "(+ ten (+ ten (* 3 -3)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "17777777777777777777777777777777777777777777777777777777776";
        BigInteger expected = new BigInteger(expectedStr);
        assertEval(expected,"(* "+bigIntStr+" 2)");

        BigInteger expected2 = new BigInteger("-"+bigIntStr);
        assertEval(expected2,"(* "+bigIntStr+" -1)");
    }

    @Test
    public void testProductMixed()
        throws Exception
    {
        assertEval("10.0", "(* 100d-1)");
        assertEval("2.500", "(* 100d-1 0.25)");
        assertEval("5.000", "(* 100d-1 0.25 2)");
        assertEval("-1.250000", "(* 100d-1 0.25 2.0 -0.25)");
        eval("(define quarter 0.25)");
        assertEval("1.25000", "(* quarter (* quarter (* 2.0 10)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "4444444444444444444444444444444444444444444444444444444444";
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEval(expected,"(* "+bigIntStr+" 0.5)");
    }

    @Test
    public void testProductArgType()
        throws Exception
    {
        expectArgTypeFailure("(* 10 10e-2)",1);
    }

    @Test
    public void testDifference()
        throws Exception
    {
        assertBigInt(-1, "(- 1)");
        assertBigInt(-1, "(- 3 4)");
        assertBigInt(-6, "(- 3 4 5)");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "4444444444444444444444444444444444444444444444444444444444";
        BigInteger expected = new BigInteger(expectedStr);
        assertEval(expected,"(- "+bigIntStr+" "+expectedStr+")");

        BigInteger expected2 = new BigInteger("-"+expectedStr);
        assertEval(expected2,"(- "+bigIntStr+" "+expectedStr+" "+expectedStr+" "+expectedStr+")");
    }

    @Test
    public void testDifferenceMixed()
        throws Exception
    {
        //assertEval("-10.0", "(- 100d-1)");
        //assertEval("9.75", "(- 100d-1 0.25)");
        assertEval("7.75", "(- 100d-1 0.25 2)");
        assertEval("8.00", "(- 100d-1 0.25 2.0 -0.25)");
        eval("(define quarter 0.25)");
        assertEval("-7.99", "(- 0.26 (- quarter (- 2.0 10)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "8888888888888888888888888888888888888888888888888888888887.75";
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEval(expected,"(- "+bigIntStr+" 0.25)");
    }

    @Test
    public void testDifferenceNoArgs()
        throws Exception
    {
        expectArityFailure("(-)");
    }

    @Test
    public void testDifferenceArgType()
        throws Exception
    {
        expectArgTypeFailure("(- 10 10e-2)",1);
    }

    @Test
    public void testAtoi()
        throws Exception
    {
        assertBigInt(5, "(to_int \"5\")");
        assertBigInt(-2, "(to_int \"-2\")");

        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        BigInteger expected = new BigInteger(bigIntStr);
        assertEval(expected,"(to_int \""+bigIntStr+"\")");
    }

    @Test
    public void testAtoiFail()
        throws Exception
    {
        expectFusionException("(to_int \"hello\")");
        expectArityFailure("(to_int)");

        expectArgTypeFailure("(to_int 2)",0);
        expectArgTypeFailure("(to_int null.string)", 0);
        expectArgTypeFailure("(to_int \"2.33332\")", 0);
    }

    @Test
    public void testItoa()
       throws Exception
    {
        assertString("5","(to_string 5)");
        assertString("-2","(to_string -2)");
        assertString("hello","(to_string \"hello\")");

        String reallyBigNumber = "8888888888888888888888888888888888888888888888888888888888";
        assertEval("\""+reallyBigNumber+"\"", "(to_string "+reallyBigNumber+")");
    }

    @Test
    public void testItoaFail()
       throws Exception
    {
        expectArgTypeFailure("(to_string 2.33332)", 0);
        expectArgTypeFailure("(to_string null.int)", 0);

        expectArityFailure("(to_string)");
        expectArityFailure("(to_string 2 2)");
    }
}
