// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;


/**
 * Generates an entire {@link Artifact} to some destination.
 *
 * @param <Destination> where/how to generate content.
 */
public abstract class ArtifactGenerator <Entity, Destination>
    implements Generator<Destination>
{
    private final Artifact<Entity> myArtifact;

    protected ArtifactGenerator(Artifact<Entity> artifact)
    {
        myArtifact = artifact;
    }

    public Artifact<Entity> getArtifact()
    {
        return myArtifact;
    }

    public Entity getEntity()
    {
        return myArtifact.getEntity();
    }
}
