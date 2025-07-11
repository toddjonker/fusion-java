// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import java.nio.file.Path;

public class MarkdownPageEntity
    extends PageEntity
{
    private final Path myPath;

    public MarkdownPageEntity(Path path)
    {
        myPath = path;
    }
}
