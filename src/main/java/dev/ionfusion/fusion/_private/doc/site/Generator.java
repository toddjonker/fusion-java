// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import java.io.IOException;

/**
 * Generates content to some destination.
 *
 * @param <Destination> where/how to generate content.
 */
@FunctionalInterface
public interface Generator <Destination>
{
    /**
     * Renders content to the destination.
     *
     * @param dest additional context needed to generate the artifact.
     */
    void generate(Destination dest)
        throws IOException;
}
