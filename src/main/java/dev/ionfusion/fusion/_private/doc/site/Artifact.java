// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a physical artifact to be produced during site generation.
 * It ties an entity to a file and a generator that can produce it.
 *
 * @param <E> the type of entity used to generate the artifact.
 */
public final class Artifact <E>
{
    private final Path                 myFile;
    private final E                    myEntity;
    private final ArtifactGenerator<E> myGenerator;

    /**
     * Creates a new artifact.
     *
     * @param file must be relative to the site root.
     * @param generator must not be null.
     * @param entity must not be null.
     */
    public Artifact(Path file, ArtifactGenerator<E> generator, E entity)
    {
        if (file.isAbsolute())
        {
            throw new IllegalArgumentException("File path must be relative to site root");
        }

        myFile = requireNonNull(file);
        myGenerator = requireNonNull(generator);
        myEntity = requireNonNull(entity);
    }


    /**
     * Provides the entity from which to generate an artifact.
     *
     * @return not null.
     */
    public E getEntity()
    {
        return myEntity;
    }


    /**
     * Returns the path to the file this Artifact will produce, relative to
     * the site root.
     */
    public Path getFile()
    {
        return myFile;
    }


    /**
     * Provides a relative path that leads "up" to the base; a series of {@code ".."} components of
     * the same depth as our file.
     *
     * @return not null.
     */
    public Path getPathToBase()
    {
        Path parent = myFile.getParent();
        if (parent == null) return Paths.get("");

        return parent.relativize(Paths.get(""));
    }


    /**
     * Produces the physical artifact, interpreting this {@linkplain #getFile() file}
     * relative to the given base directory.
     *
     * @param baseDir is full path under which to generate the artifact.
     */
    public void generate(Path baseDir)
        throws IOException
    {
        myGenerator.generate(this, baseDir.resolve(getFile()));
    }
}
