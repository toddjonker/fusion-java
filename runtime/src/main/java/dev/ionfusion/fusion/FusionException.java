// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents conditions raised within Fusion code, as opposed to failures
 * within the interpreter implementation.
 * <p>
 * Unlike Java's {@code throw} form, Fusion's {@code raise} procedure allows
 * one to throw arbitrary values, not just "exception" types.  Within the
 * FusionJava implementation, all such values are wrapped in
 * {@link FusionException}s.
 * <p>
 * In order to show Fusion stack traces when these Java exceptions are printed,
 * {@link #getMessage()} consists of two parts: the <em>base message</em>} (a
 * description of the exception) and the <em>context</em> (the Fusion stack
 * trace).
 * Rather than simply printing the Java exception, applications and tools may
 * produce better messages by getting the components individually via
 * {@link #getBaseMessage()} and {@link #getContext()}.
 */
@SuppressWarnings("serial")
public class FusionException
    extends Exception
{
    /**
     * The Fusion stack trace, aggregated by {@code catch} clauses in the
     * interpreter as the Java stack unwinds.
     */
    private List<SourceLocation> myContext;


    public FusionException(String message)
    {
        super(message);
    }

    public FusionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public FusionException(Throwable cause)
    {
        super(cause.getMessage(), cause);
    }


    /**
     * Prepends a location to the continuation trace of this exception.
     *
     * @param location can be null to indicate an unknown location.
     */
    public void addContext(SourceLocation location)
    {
        if (myContext == null)
        {
            myContext = new ArrayList<>(32);
            myContext.add(location);
        }
        else
        {
            // Collapse equal adjacent locations
            SourceLocation prev = myContext.get(myContext.size() - 1);
            if (! Objects.equals(prev, location))
            {
                myContext.add(location);
            }
        }
    }


    /**
     * Returns the Fusion stack trace of this exception.
     * The first element in the list is the deepest stack frame, normally the
     * site of the exception.
     * <p>
     * The list may contain null elements indicating notable gaps in the trace.
     * In the default stack display, these appear as {@code ...} lines without
     * locations.
     *
     * @return an immutable list; not null.
     */
    public List<SourceLocation> getContext()
    {
        return (myContext == null ? emptyList() : unmodifiableList(myContext));
    }


    // Before making this public, think about whether it needs Evaluator
    // and should throw FusionException
    void displayContinuation(Appendable out)
        throws IOException
    {
        if (myContext != null)
        {
            for (SourceLocation loc : myContext)
            {
                if (loc == null)
                {
                    out.append("\n  ...");
                }
                else
                {
                    out.append("\n  ...at ");
                    loc.display(out);
                }
            }
        }
    }

    /**
     * Gets the value that was passed to Fusion's {@code raise} procedure.
     * The result could be any Fusion value, so it must be handled carefully.
     * True Fusion exception values -- that is, the values raised by library
     * features like {@code assert} and {@code raise_argument_error} -- are
     * implemented as subclasses of this type, and this method will return
     * {@code this} object.
     *
     * @return the Fusion value raised by Fusion code.
     */
    public Object getRaisedValue()
    {
        return this;
    }

    /**
     * Returns the message string given to the exception constructor.
     * This should be used instead of {@link #getMessage()} since the latter is
     * overridden here to delegate to {@link #displayMessage}.
     *
     * @return the base message.
     */
    public String getBaseMessage()
    {
        return super.getMessage();
    }

    /** XXX Method uses package-private class Evaluator.
     * However, that's always `null` when invoked from getMessage()!
     * There's a core problem here that displaying the exception in tools
     * does not have access to the Evaluator, so anything we print needs to
     * be able to do so without running Fusion code.
     *
     * Most overrides need to write general FVs:
     *   * ArgumentException writes FVs
     *   * ArityFailure write FVs
     *   * CheckException has complex processing to display check frames
     *   * FusionUserException simply `write`s the raised FV
     *   * ResultFailure writes the FV results
     *
     * Some overrides write syntax objects:
     *   * FusionAssertionException writes its expression at compile time.
     *   * SyntaxException writes its expression lazily.
     *
     * Subclasses need to be able to display and/or write arbitrary FVs,
     * which we expect should generalize to running Fusion code.
     *
     * Do we really want to do that?  That sounds risky; it should probably
     * happen in a tight sandbox so we don't load code, perform arbitrary IO,
     * etc.  It at least needs to be have a continuation guard to trap exns.
     *
     * Perhaps we don't support that when called from Java code?  I guess that's
     * basically what we have here, implicitly.  And probably causes a crash.
     *
     * Use case: replacing our FExn subclasses with Fusion records that print
     * themselves from Fusion code.
     *
     * -> Racket exn message is a struct field, so printing is handled before
     *    construction.
     *
     * This loses the structure of the information, and IMO grants rendering
     * decisions to the wrong code. The component reporting the error, for
     * human or machine, should determine the layout and value display within
     * the overall report.  This would allow things like fully structured
     * logging, pretty UX components that don't require parsing and decoding
     * the message back to its constituents.
     *
     * ??? Add embedding APIs to
     *   * generate the message dynamically
     *   * get structured info out of the exn
     *
     * Options to consider for getMessage():
     *
     * PREWRITE the message in all cases (like Racket), and/or:
     * PRESERVE the arguments for tooling and custom displays.
     *
     * HACK the output if a callback is needed and there's no Evaluator.
     * Similar to `safeWrite` trapping exceptions, output placeholder text,
     * like a default rendering of records and JValues.
     *
     * CAPTURE the Evaluator in subclasses that need it.
     * Make some sense, its like doing call/cc, ensures a parameterized
     * context to render in. Violates the normal rules of engagement.
     */
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        String superMessage = getBaseMessage();
        if (superMessage != null)
        {
            out.append(superMessage);
        }
    }

    /**
     * @return the base message, followed by the Fusion continuation trace.
     */
    @Override
    public final String getMessage()
    {
        StringBuilder out = new StringBuilder();

        try
        {
            displayMessage(null, out);
            displayContinuation(out);
        }
        catch (IOException | FusionException e)
        {
            // Swallow these, we can't do anything with it at the moment.
        }

        return out.toString();
    }
}
