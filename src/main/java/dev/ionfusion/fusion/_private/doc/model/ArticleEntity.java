// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import dev.ionfusion.fusion._private.HtmlWriter;
import java.io.IOException;

/**
 * Represents a generic article, sourced as a file.
 * <p>
 * Entities are independent of the path at which they are rendered.
 */
public class ArticleEntity
    // implements Titled
{
    public String getTitle()
        throws IOException
    {
        return null;
    }

    public void render(HtmlWriter htmlWriter)
        throws IOException
    {
        // nothing to render
    }
}
