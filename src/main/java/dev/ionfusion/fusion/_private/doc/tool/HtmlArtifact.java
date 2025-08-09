// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion._private.HtmlWriter;
import java.io.IOException;
import java.nio.file.Path;


public abstract class HtmlArtifact
    extends Artifact
{
    /**
     * @param file is relative to the site root.
     */
    public HtmlArtifact(Path file)
    {
        super(file);
    }


    @Override
    public void generate(Path baseDir)
        throws IOException
    {
        Path outFile = baseDir.resolve(getFile());
        try (HtmlWriter writer = new HtmlWriter(outFile.toFile()))
        {
            render(writer);
        }
    }

    abstract void render(HtmlWriter out)
        throws IOException;
}
