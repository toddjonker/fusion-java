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
import java.io.IOException;
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
    private final Map<ModuleIdentity, ModuleEntity> myModules  = new HashMap<>();

    /**
     * Path is relative to internal docroot, does not include extension (eg ".md")
     */
    private final Map<Path, ArticleEntity>          myArticles = new HashMap<>();


    public RepoEntity(Path repoDir, Predicate<ModuleIdentity> selector, TopLevel top)
        throws FusionException, IOException
    {
        // Discover everything first; the Consumer won't propagate exceptions.
        Set<ModuleIdentity> moduleIds = new HashSet<>();
        discoverModulesInRepository(repoDir, selector, moduleIds::add);

        for (ModuleIdentity id : moduleIds)
        {
            // TODO Handle exceptions.
            ModuleIdentity loadedId = loadModule(top, id.absolutePath());
            assert id.equals(loadedId);

            ModuleDocs docs = instantiateModuleDocs(top, id);
            assert docs != null;

            addModuleDocs(docs);
        }

        discoverArticles(repoDir);
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

    public ModuleEntity getModule(ModuleIdentity id)
    {
        return myModules.get(id);
    }

    public Set<ModuleEntity> getModules()
    {
        HashSet<ModuleEntity> set = new HashSet<>(myModules.values());
        assert set.size() == myModules.size();
        return set;
    }


    private void discoverArticles(Path repoDir)
    {
        discoverArticles(Paths.get(""),                     // empty path
                         repoDir.resolve("src"));

    }

    private void discoverArticles(Path outDir, Path repoDir)
    {
        String[] fileNames = repoDir.toFile().list();

        for (String fileName : fileNames)
        {
            Path child = repoDir.resolve(fileName);

            if (fileName.endsWith(".md"))
            {
                String baseName = fileName.substring(0, fileName.length() - 3);
                Path outPath = outDir.resolve(baseName);

                myArticles.put(outPath, new MarkdownArticleEntity(child));
            }
            else if (isDirectory(child))
            {
                discoverArticles(outDir.resolve(fileName), child);
            }
        }
    }


    public Map<Path, ArticleEntity> getArticles()
    {
        return myArticles;
    }
}
