// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class Artifact
{
    private final Path myFile;

    public Artifact(Path file)
    {
        myFile = file;
    }


    /**
     * Returns the path to the file this Artifact will produce, relative to
     * the site root.
     */
    public Path getFile()
    {
        return myFile;
    }

    public Path getParentDir()
    {
        Path parent = myFile.getParent();
        return (parent != null) ? parent : Paths.get("");
    }


    public abstract void generate(Path baseDir)
        throws IOException;
}
