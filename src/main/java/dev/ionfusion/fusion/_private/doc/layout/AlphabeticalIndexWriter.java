// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.layout;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.tool.DocIndex;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class AlphabeticalIndexWriter
    extends HtmlWriter
{
    private final Predicate<ModuleIdentity> myFilter;


    public AlphabeticalIndexWriter(Predicate<ModuleIdentity> filter, StreamWriter out)
    {
        super(out);
        myFilter = filter;
    }


    void renderIndex(DocIndex index)
        throws IOException
    {
        renderHeader1("Binding Index");

        append("<table><tbody>");
        for (Map.Entry<String, Set<ModuleIdentity>> entry : index.getNameMap().entrySet())
        {
            String escapedName = escapeString(entry.getKey());
            append("<tr><td class='bound'>");
            append(escapedName);
            append("</td><td>");

            boolean printedOne = false;
            for (ModuleIdentity id : entry.getValue())
            {
                if (myFilter.test(id))
                {
                    if (printedOne)
                    {
                        append(", ");
                    }
                    linkToBindingAsModulePath(id, escapedName);
                    printedOne = true;
                }
            }

            append("</td></tr>\n");
        }
        append("</tbody></table>\n");
    }
}
