// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Models a collection of web pages and handles their rendering.
 */
public class Website
{
    private final Map<Path, ArticleEntity> myPages = new HashMap<>();


    /**
     * Walks a directory tree and adds any {@code .md} files into this site.

     * @param srcDir  the tree of source documents to walk.
     * @param siteDir the path in this site where the pages will be placed.
     */
    public void discoverMarkdownPages(Path srcDir, Path siteDir)
        throws IOException
    {
        for (Path path : Files.newDirectoryStream(srcDir, "*.md"))
        {
            String fileName = path.getFileName().toString();
            String docName = fileName.substring(0, fileName.length() - 2);

            ArticleEntity page     = new MarkdownArticleEntity(path);
            Path          pagePath = siteDir.resolve(docName);
            addPage(pagePath, page);
        }

    }


    public void addPage(Path path, ArticleEntity page)
    {
        assert !myPages.containsKey(path);
        myPages.put(path, page);
    }

    public ArticleEntity getPage(Path path)
    {
        return myPages.get(path);
    }
}
