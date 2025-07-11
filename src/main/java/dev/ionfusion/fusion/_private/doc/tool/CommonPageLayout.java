// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._private.doc.tool.DocGenerator.HEADER_LINKS;

import dev.ionfusion.fusion._private.HtmlWriter;
import java.io.IOException;

/**
 * Transforms a single Markdown file into HTML.
 * <p>
 * The page title is taken from the first H1, assumed to be authored using
 * the atx syntax: {@code # <Title content>}.
 */
class CommonPageLayout
{
    private final String   myTitle;
    private final String   myBaseUrl;
    private final String[] myCssUrls;


    CommonPageLayout(String title, String baseUrl, String... cssUrls)
    {
        myTitle = title;
        myBaseUrl = baseUrl.isEmpty() ? "." : baseUrl;
        myCssUrls = cssUrls;
    }


    void render(HtmlWriter writer, Renderer<HtmlWriter> contentWriter)
        throws IOException
    {
        writer.openHtml();
        {
            writer.renderHead(myTitle, myBaseUrl, myCssUrls);
            writer.openBody();
            {
                writer.append(HEADER_LINKS);

                contentWriter.render(writer);
            }
            writer.closeBody();
        }
        writer.closeHtml();
    }
}
