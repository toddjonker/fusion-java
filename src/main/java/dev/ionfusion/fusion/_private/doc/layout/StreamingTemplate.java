// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.layout;

import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.ArtifactGenerator;
import dev.ionfusion.fusion._private.doc.site.Generator;
import dev.ionfusion.fusion._private.doc.site.Template;
import java.io.IOException;
import java.nio.file.Path;

public class StreamingTemplate<E>
    implements Template<E, Path>
{
    private final Template<E, StreamWriter> myStreamTemplate;

    public StreamingTemplate(Template<E, StreamWriter> template)
    {
        myStreamTemplate = template;
    }


    @Override
    public Generator<Path> populate(Artifact<E> artifact)
    {
        return new StreamingGenerator(artifact);
    }


    private class StreamingGenerator
        extends ArtifactGenerator<E, Path>
    {
        public StreamingGenerator(Artifact<E> artifact)
        {
            super(artifact);
        }

        @Override
        public void generate(Path outFile)
            throws IOException
        {
            Generator<StreamWriter> streamer = myStreamTemplate.populate(getArtifact());

            try (StreamWriter writer = new StreamWriter(outFile))
            {
                streamer.generate(writer);
            }
        }
    }
}
