// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionAssertionException.makeAssertError;
import static dev.ionfusion.fusion.FusionString.checkNullableStringArg;
import static dev.ionfusion.fusion.FusionString.checkRequiredStringArg;

import dev.ionfusion.runtime.base.FusionException;

final class MakeAssertionErrorProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(eval, 3, args);

        String message    = checkNullableStringArg(eval, this, 0, args);
        Object result     = args[1];
        String expression = checkRequiredStringArg(eval, this, 2, args);

        return makeAssertError(eval, message, expression, result);
    }
}
