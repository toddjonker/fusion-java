// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.layout.ModuleLayout;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import dev.ionfusion.fusion._private.doc.site.HtmlArtifactGenerator;
import dev.ionfusion.fusion._private.doc.site.HtmlLayout;
import dev.ionfusion.fusion._private.doc.site.Site;
import java.nio.file.Path;
import java.nio.file.Paths;
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


    private <E> void placePage(Path path, HtmlLayout<E> layout, E entity)
    {
        mySite.addArtifact(path, new HtmlArtifactGenerator<>(layout), entity);
    }


    public void placeModules()
        throws FusionException
    {
        ModuleLayout layout = new ModuleLayout(myModuleSelector);

        for (ModuleEntity module : myRepo.getModules())
        {
            ModuleIdentity id = module.getIdentity();
            Path file = Paths.get(".", id.absolutePath() + ".html");

            placePage(file, layout, module);
        }
    }
}
