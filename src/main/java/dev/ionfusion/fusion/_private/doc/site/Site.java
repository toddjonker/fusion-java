// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


/**
 * Entity that represents a collection of artifacts to be generated as a documentation site.
 * <p>
 * This acts rather like a file system, and perhaps would be better as a tree of {@link Artifact}s.
 */
public class Site
{
    private static final class Placement <E>
    {
        private final Artifact<E>       myArtifact;
        private final Template<E, Path> myTemplate;

        private Placement(Artifact<E> artifact, Template<E, Path> template)
        {
            myArtifact = artifact;
            myTemplate = template;
        }

        void generate(Path siteDir)
            throws IOException
        {
            Path file = siteDir.resolve(myArtifact.getRelativePath());
            myTemplate.populate(myArtifact).generate(file);
        }
    }

    private final Map<Path, Placement<?>> myArtifacts = new HashMap<>();


    // We care about Entities so that we can compute links between them.
    public <E> void addArtifact(E entity, Path path, Template<E, Path> template)
    {
        if (myArtifacts.containsKey(path))
        {
            throw new IllegalArgumentException("Duplicate artifact path: " + path);
        }

        Artifact<E> artifact = new Artifact<>(entity, path);
        myArtifacts.put(path, new Placement<>(artifact, template));
    }


    /**
     * Convenience method for adding an artifact with a file path relative to the site root.
     *
     * @param path is turned into a path as by {@link Paths#get(String, String...)}.
     */
    public <E> void addArtifact(E entity, String path, Template<E, Path> template)
    {
        addArtifact(entity, Paths.get(path), template);
    }


    public void generate(Path siteDir)
        throws IOException
    {
        assert siteDir.isAbsolute();
        // TODO check that we aren't commingling with old data.

        for (Placement<?> placement : myArtifacts.values())
        {
            placement.generate(siteDir);
        }
    }
}
