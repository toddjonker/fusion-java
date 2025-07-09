// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates a physical artifact from an entity.
 *
 * @param <E> the type of entity consumed by the generator.
 */
public interface ArtifactGenerator <E>
{
    /**
     * Produces an artifact from an entity at a given path.
     *
     * @param artifact the artifact to be generated. Must not be null.
     * @param file an absolute path to the file to be generated. Must not be null.
     */
    void generate(Artifact<E> artifact, Path file)
        throws IOException;
}
