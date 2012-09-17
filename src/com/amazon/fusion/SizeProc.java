// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonContainer;

final class SizeProc
    extends Procedure
{
    SizeProc()
    {
        //    "                                                                               |
        super("Returns the number of child elements contained in the CONTAINER.\n" +
              "The size of null.list (etc) is zero.",
              "container");
    }

    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(args);
        IonContainer c = checkContainerArg(0, args);
        return eval.newInt(c.size());
    }
}
