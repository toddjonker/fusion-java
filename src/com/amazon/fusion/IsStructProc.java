// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionStruct.isStruct;
import com.amazon.ion.IonStruct;

final class IsStructProc
    extends Procedure1
{
    IsStructProc()
    {
        //    "                                                                               |
        super("Determines whether `value` is a struct, returning true or false.",
              "value");
    }

    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        boolean result = (isStruct(eval, arg) || arg instanceof IonStruct);
        return eval.newBool(result);
    }
}
