// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonSystem;
import com.amazon.ion.IonText;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import java.io.IOException;

/**
 * A {@link FusionValue} that contains an {@link IonValue}.
 */
final class DomValue
    extends FusionValue
    implements Writeable
{
    private final IonValue myDom;

    /**
     * @param dom must not be null.
     */
    DomValue(IonValue dom)
    {
        assert dom != null;
        myDom = dom;
    }


    @Override
    public boolean isIon()
    {
        return true;
    }


    /**
     * {@inheritDoc}
     *
     * @return not null.
     */
    @Override
    public IonValue ionValue(ValueFactory factory)
    {
        // TODO this isn't really the proper comparison
        if (myDom.getSystem() == factory && myDom.getContainer() == null)
        {
            return myDom;
        }

        // FIXME this is horrible hack
        return ((IonSystem)factory).clone(myDom);
    }


    /**
     * {@inheritDoc}
     *
     * @return not null.
     */
    @Override
    IonValue ionValue()
    {
        return myDom;
    }



    @Override
    public void write(Appendable out)
        throws IOException
    {
        FusionUtils.writeIon(out, myDom);
    }


    @Override
    public void display(Appendable out)
        throws IOException
    {
        if (myDom instanceof IonText)
        {
            String text = ((IonText) myDom).stringValue();
            out.append(text);
        }
        else
        {
            write(out);
        }
    }

    @Override
    public void write(IonWriter out)
    {
        myDom.writeTo(out);
    }
}
