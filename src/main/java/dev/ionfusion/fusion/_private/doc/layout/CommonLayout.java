// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.layout;

import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.HtmlLayout;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The base HTML layout for all pages, providing common structure and style.
 *
 * @param <E> the type of entity providing page content.
 */
public abstract class CommonLayout <E>
    implements HtmlLayout<E>
{
    /** HTML content for the masthead links */
    private static final String HEADER_LINKS =
        "<div class='indexlink'>" +
        "<a href='index.html'>Top</a> " +
        "<a href='binding-index.html'>Binding Index</a> " +
        "(<a href='permuted-index.html'>Permuted</a>)" +
        "</div>\n";


    protected abstract static class Context
    {
        /**
         * Collects the URLs of CSS style sheets to include in the HTML header.
         */
        void addCssUrls(ArrayList<String> urls)
            throws IOException
        {
            urls.add("common.css");
        }

        abstract String getTitle()
            throws IOException;

        abstract void renderContent(StreamWriter out)
            throws IOException;
    }


    protected abstract Context makeContext(Artifact<E> artifact)
        throws IOException;


    @Override
    public final void render(Artifact<E> artifact, StreamWriter out)
        throws IOException
    {
        Context ctx = makeContext(artifact);

        String title = ctx.getTitle();
        if (title == null || title.isEmpty()) title = "Ion Fusion Documentation";

        ArrayList<String> cssUrls = new ArrayList<>();
        ctx.addCssUrls(cssUrls);

        HtmlWriter writer = new HtmlWriter(out);
        writer.openHtml();
        {
            writer.renderHead(title,
                              artifact.getPathToBase().toString(),
                              cssUrls.toArray(new String[0]));

            writer.openBody();
            {
                writer.append(HEADER_LINKS);

                ctx.renderContent(out);
            }
            writer.closeBody();
        }
        writer.closeHtml();
    }
}
