// Copyright (c) 2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * A derived parameter that delays initialization until first use.
 * This avoids opening an IonReader over stdin unless we really need to.
 * <p>
 * TODO FUSION-160 This should be built as a derived parameter.
 */
final class CurrentIonReaderParameter
    extends DynamicParameter
{
    private Object myDefaultValue;

    CurrentIonReaderParameter()
    {
        super(null);
    }

    @Override
    <T> T currentValue(Evaluator eval)
    {
        Object result = super.currentValue(eval);
        if (result == null)
        {
            // There's no parameterization!  Determine the default value.
            synchronized (this)
            {
                if (myDefaultValue == null)
                {
                    // TODO use current-input-port?
                    // That may be hard to predict the result.
                    myDefaultValue = eval.getSystem().newReader(System.in);
                }
                result = myDefaultValue;
            }
        }

        return (T) result;
    }
}
