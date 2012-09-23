// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.io.IOException;


/**
 * Indicates a failure applying a procedure with the wrong number of arguments.
 */
@SuppressWarnings("serial")
final class ArityFailure
    extends ContractFailure
{
    enum Variability { EXACT, AT_LEAST }

    private final Procedure myProc;
    private final int myArity;
    private final Variability myVariability;
    private final Object[] myActuals;

    /**
     * @param proc must not be null
     */
    ArityFailure(Procedure proc, int arity, Variability variability,
                 Object... actuals)
    {
        super("arity failure");
        assert proc != null && actuals != null;
        myProc = proc;
        myArity = arity;
        myVariability = variability;
        myActuals = actuals;
    }

    @Override
    public String getMessage()
    {
        StringBuilder b = new StringBuilder();
        try {
            myProc.identify(b);
            b.append(" expects ");
            if (myVariability == Variability.AT_LEAST)
            {
                b.append("at least ");
            }
            b.append(myArity);
            b.append(" argument");
            if (myArity > 1) b.append("s");
            b.append(", given ");
            b.append(myActuals.length);
            if (myActuals.length != 0)
            {
                b.append(":\n  ");
                FusionValue.writeMany(b, myActuals, "\n  ");
            }
        }
        catch (IOException e) {}
        return b.toString();
    }
}
