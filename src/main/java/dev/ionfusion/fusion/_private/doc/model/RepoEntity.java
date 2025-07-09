// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import static dev.ionfusion.fusion._Private_Trampoline.discoverModulesInRepository;
import static dev.ionfusion.fusion._Private_Trampoline.instantiateModuleDocs;
import static dev.ionfusion.fusion._Private_Trampoline.loadModule;
import static java.nio.file.Files.isDirectory;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion.TopLevel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Holds aggregated documentation and cross-reference for a repository.
 */
public class RepoEntity
{
    private final Path                              myRepoDir;
    private final Predicate<ModuleIdentity>         mySelector;
    private final TopLevel                          myTopLevel;
    private       Map<ModuleIdentity, ModuleEntity> myModules;

    /**
     * Path is relative to internal docroot, does not include extension (eg ".md")
     */
    private Map<Path, MarkdownArticle> myArticles;


    public RepoEntity(Path repoDir, Predicate<ModuleIdentity> selector, TopLevel top)
    {
        this.myRepoDir = repoDir;
        this.mySelector = selector;
        this.myTopLevel = top;
    }

    private void discoverModules()
        throws FusionException
    {
        // Discover everything first; the Consumer won't propagate exceptions.
        Set<ModuleIdentity> moduleIds = new HashSet<>();
        discoverModulesInRepository(myRepoDir, mySelector, moduleIds::add);

        for (ModuleIdentity id : moduleIds)
        {
            // TODO Handle exceptions.
            ModuleIdentity loadedId = loadModule(myTopLevel, id.absolutePath());
            assert id.equals(loadedId);

            ModuleDocs docs = instantiateModuleDocs(myTopLevel, id);
            assert docs != null;

            addModuleDocs(docs);
        }
    }

    private ModuleEntity ensureEntityForModule(ModuleIdentity id)
    {
        if (id == null) return null;

        ModuleEntity module = myModules.get(id);
        if (module != null) return module;

        ModuleEntity parent = ensureEntityForModule(id.parent());

        module = new ModuleEntity(id);
        if (parent != null) parent.addChild(module);
        myModules.put(id, module);

        return module;
    }

    private void addModuleDocs(ModuleDocs module)
    {
        ModuleIdentity id     = module.getIdentity();
        ModuleEntity   entity = ensureEntityForModule(id);
        entity.setModuleDocs(module);
    }

    public Set<ModuleEntity> getModules()
        throws FusionException
    {
        if (myModules == null)
        {
            myModules = new HashMap<>();
            discoverModules();
        }

        HashSet<ModuleEntity> set = new HashSet<>(myModules.values());
        assert set.size() == myModules.size();
        return set;
    }


    private void discoverArticles(Path repoDir, Path dir)
    {
        String[] fileNames = repoDir.toFile().list();
        if (fileNames == null) return;

        for (String fileName : fileNames)
        {
            Path child = repoDir.resolve(fileName);

            if (fileName.endsWith(".md"))
            {
                String baseName = fileName.substring(0, fileName.length() - 3);
                Path outPath = dir.resolve(baseName);

                myArticles.put(outPath, new MarkdownArticle(child));
            }
            else if (isDirectory(child))
            {
                discoverArticles(child, dir.resolve(fileName));
            }
        }
    }


    public Map<Path, MarkdownArticle> getArticles()
    {
        if (myArticles == null)
        {
            myArticles = new HashMap<>();

            // TODO Move articles to a separate directory.
            discoverArticles(myRepoDir.resolve("src"), Paths.get(""));
        }
        return myArticles;
    }
}
