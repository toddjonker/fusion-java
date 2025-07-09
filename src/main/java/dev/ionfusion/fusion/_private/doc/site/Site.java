// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Entity that represents a collection of artifacts to be generated as a documentation site.
 * <p>
 * This acts rather like a file system, and perhaps would be better as a tree of {@link Artifact}s.
 */
public class Site
{
    private final Map<Path, Artifact<?>> myArtifacts = new HashMap<>();

    public Site()
    {
    }


    Collection<Artifact<?>> getArtifacts()
    {
        return myArtifacts.values();
    }

    public <E> void addArtifact(Path path, ArtifactGenerator<E> generator, E entity)
    {
        if (myArtifacts.containsKey(path))
        {
            throw new IllegalArgumentException("Duplicate artifact path: " + path);
        }

        // Asserts path is relative:
        Artifact<?> artifact = new Artifact<>(path, generator, entity);
        myArtifacts.put(path, artifact);
    }

    /**
     * Convenience method for adding an artifact with a file path relative to the site root.
     *
     * @param file is turned into a path as by {@link Paths#get(String, String...)}.
     */
    public <E> void addArtifact(String file, ArtifactGenerator<E> generator, E entity)
    {
        addArtifact(Paths.get(file), generator, entity);
    }


    public void generate(Path siteDir)
        throws IOException
    {
        assert siteDir.isAbsolute();
        // TODO check that we aren't commingling with old data.

        for (Map.Entry<Path, Artifact<?>> entry : myArtifacts.entrySet())
        {
            Path        path     = entry.getKey();
            Artifact<?> artifact = entry.getValue();

            artifact.generate(siteDir);
        }
    }
}
