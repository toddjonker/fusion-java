// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * Numeric product.
 */
final class ProductProc
    extends Procedure
{
    ProductProc()
    {
        //    "                                                                               |
        super("Returns the product of the arguments. With no arguments, returns 1.",
              "int or dec", DOTDOTDOT);
    }

    Number multiply(Number result, Number operand)
            throws FusionException
        {
            Number newResult = null;
            if (result instanceof BigInteger && operand instanceof BigInteger)
            {
                BigInteger op1 = (BigInteger)result;
                BigInteger op2 = (BigInteger)operand;
                newResult = op1.multiply(op2);
            } else if (result instanceof BigInteger && operand instanceof BigDecimal)
            {
                BigDecimal bResult = new BigDecimal((BigInteger)result);
                newResult = bResult.multiply((BigDecimal)operand);
            } else if (result instanceof BigDecimal && operand instanceof BigInteger)
            {
                BigDecimal bOperand = new BigDecimal((BigInteger)operand);
                newResult = bOperand.multiply((BigDecimal)result);
            } else if (result instanceof BigDecimal && operand instanceof BigDecimal)
            {
                BigDecimal op1 = (BigDecimal)result;
                BigDecimal op2 = (BigDecimal)operand;
                newResult = op1.multiply(op2);
            }

            return newResult;
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            Number result = BigInteger.ONE;

            for (int i = 0; i < args.length; i++)
            {
                Number operandNum = checkBigArg(i, args);
                if (operandNum == null)
                {
                    throw contractFailure("Expected: int or decimal; observed: "+
                                          FusionValue.writeToString(args[i]));
                }
                result = multiply(result,operandNum);
            }

            Object finalResult = null;
            if (result instanceof BigInteger)
            {
                finalResult = eval.newInt((BigInteger)result);
            }
            else if (result instanceof BigDecimal)
            {
                finalResult = eval.newDecimal((BigDecimal)result);
            }

            return finalResult;
        }
}
