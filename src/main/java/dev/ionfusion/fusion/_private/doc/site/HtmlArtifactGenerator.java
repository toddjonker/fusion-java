// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import dev.ionfusion.fusion._private.StreamWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates an HTML file from an entity, using a {@link HtmlLayout}.
 *
 * @param <E> the type of entity providing page content.
 */
public class HtmlArtifactGenerator <E>
    implements ArtifactGenerator<E>
{
    private final HtmlLayout<E> myLayout;

    public HtmlArtifactGenerator(HtmlLayout<E> layout)
    {
        myLayout = layout;
    }


    @Override
    public void generate(Artifact<E> artifact, Path outFile)
        throws IOException
    {
        try (StreamWriter writer = new StreamWriter(outFile))
        {
            myLayout.render(artifact, writer);
        }
    }
}
