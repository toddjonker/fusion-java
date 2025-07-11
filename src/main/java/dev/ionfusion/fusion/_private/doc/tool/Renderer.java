// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import java.io.IOException;

public interface Renderer <T>
{
    void render(T writer)
        throws IOException;
}
