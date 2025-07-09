// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import dev.ionfusion.fusion._private.StreamWriter;
import java.io.IOException;

public interface HtmlLayout <E>
{
    void render(Artifact<E> artifact, StreamWriter writer)
        throws IOException;
}
