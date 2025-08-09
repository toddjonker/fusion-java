// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.doc.model.ArticleEntity;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArticleArtifact
    extends HtmlArtifact
{
    private final ArticleEntity myArticle;

    /**
     * @param file is relative to the site root.
     */
    public ArticleArtifact(Path file, ArticleEntity article)
    {
        super(file);
        myArticle = article;
    }


    @Override
    void render(HtmlWriter out)
        throws IOException
    {
        // basePath leads up to the baseDir
        Path parent   = getParentDir();
        Path basePath = parent.relativize(Paths.get(""));

        CommonPageLayout layout = new CommonPageLayout(myArticle.getTitle(),
                                                       basePath.toString(),
                                                       "common.css", "doc.css");
        layout.render(out, myArticle::render);
    }
}
