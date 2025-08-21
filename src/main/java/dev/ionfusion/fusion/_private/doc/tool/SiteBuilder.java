// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._private.doc.tool.DocIndex.buildDocIndex;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.layout.AlphabeticalIndexLayout;
import dev.ionfusion.fusion._private.doc.layout.MarkdownArticleLayout;
import dev.ionfusion.fusion._private.doc.layout.ModuleLayout;
import dev.ionfusion.fusion._private.doc.layout.PermutedIndexLayout;
import dev.ionfusion.fusion._private.doc.layout.StreamingTemplate;
import dev.ionfusion.fusion._private.doc.model.MarkdownArticle;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import dev.ionfusion.fusion._private.doc.site.Site;
import dev.ionfusion.fusion._private.doc.site.Template;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Predicate;

public class SiteBuilder
{
    private final Site                      mySite = new Site();
    private final RepoEntity                myRepo;
    private final Predicate<ModuleIdentity> myModuleSelector;

    public SiteBuilder(RepoEntity repo,
                       Predicate<ModuleIdentity> selector)
    {
        myModuleSelector = selector;
        myRepo = repo;
    }

    public Site build()
        throws FusionException
    {
        return mySite;
    }


    private <E> void placePage(E entity, Path path, Template<E, StreamWriter> template)
    {
        mySite.addArtifact(entity, path, new StreamingTemplate<E>(template));
    }

    private <E> void placePage(E entity, String path, Template<E, StreamWriter> template)
    {
        mySite.addArtifact(entity, path, new StreamingTemplate<E>(template));
    }


    public void placeModules()
        throws FusionException
    {
        Template<ModuleEntity, StreamWriter> factory = ModuleLayout.template(myModuleSelector);

        for (ModuleEntity module : myRepo.getModules())
        {
            ModuleIdentity id = module.getIdentity();
            Path file = Paths.get(".", id.absolutePath() + ".html");

            placePage(module, file, factory);
        }
    }


    public void placeArticles()
    {
        for (Map.Entry<Path, MarkdownArticle> entry : myRepo.getArticles().entrySet())
        {
            MarkdownArticle article  = entry.getValue();
            Path            location = entry.getKey();
            String          fileName = location.getFileName().toString() + ".html";
            Path            file     = location.resolveSibling(fileName);

            placePage(article, file, MarkdownArticleLayout::new);
        }
    }

    public void prepareIndexes()
        throws FusionException
    {
        // The two index artifacts are different layouts of the same entity.
        DocIndex docIndex = buildDocIndex(myRepo.getModules());

        placePage(docIndex, "binding-index.html", AlphabeticalIndexLayout.template(myModuleSelector));
        placePage(docIndex, "permuted-index.html", PermutedIndexLayout.template(myModuleSelector));
    }
}
