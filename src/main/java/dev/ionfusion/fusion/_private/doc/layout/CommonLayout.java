// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.layout;

import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.ArtifactGenerator;
import dev.ionfusion.fusion._private.doc.site.Generator;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The base HTML layout for all pages, providing common structure and style.
 *
 * @param <E> the type of entity providing page content.
 */
public abstract class CommonLayout <E>
    extends ArtifactGenerator<E, StreamWriter>
{
    /** HTML content for the masthead links */
    private static final String HEADER_LINKS =
        "<div class='indexlink'>" +
        "<a href='index.html'>Top</a> " +
        "<a href='binding-index.html'>Binding Index</a> " +
        "(<a href='permuted-index.html'>Permuted</a>)" +
        "</div>\n";


    protected CommonLayout(Artifact<E> artifact)
    {
        super(artifact);
    }


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

    void renderContent(StreamWriter out)
        throws IOException
    {
        contentGenerator(out).generate(null);
    }

    Generator<Void> contentGenerator(StreamWriter out)
    {
        throw new UnsupportedOperationException("No content generator for " + getClass());
    }


    @Override
    public final void generate(StreamWriter out)
        throws IOException
    {
        String title = getTitle();
        if (title == null || title.isEmpty()) title = "Ion Fusion Documentation";

        ArrayList<String> cssUrls = new ArrayList<>();
        addCssUrls(cssUrls);

        HtmlWriter writer = new HtmlWriter(out);
        writer.openHtml();
        {
            writer.renderHead(title,
                              getArtifact().getPathToBase().toString(),
                              cssUrls.toArray(new String[0]));

            writer.openBody();
            {
                writer.append(HEADER_LINKS);

                renderContent(out);
            }
            writer.closeBody();
        }
        writer.closeHtml();
    }
}
