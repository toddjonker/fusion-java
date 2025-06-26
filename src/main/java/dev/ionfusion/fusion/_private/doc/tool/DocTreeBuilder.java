// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._Private_Trampoline.discoverModulesInRepository;
import static dev.ionfusion.fusion._Private_Trampoline.instantiateModuleDocs;
import static dev.ionfusion.fusion._Private_Trampoline.loadModule;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion.TopLevel;
import dev.ionfusion.fusion._private.doc.model.DocTreeNode;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

final class DocTreeBuilder
{
    private final TopLevel                  myTopLevel;
    private final Predicate<ModuleIdentity> myModuleFilter;

    DocTreeBuilder(TopLevel topLevel, Predicate<ModuleIdentity> filter)
    {
        myTopLevel = topLevel;
        myModuleFilter = filter;
    }

    DocTreeNode build(Path repoDir)
        throws FusionException, IOException
    {
        // Discover everything first; the Consumer won't propagate exceptions.
        Set<ModuleIdentity> moduleIds = new HashSet<>();
        discoverModulesInRepository(repoDir, myModuleFilter, moduleIds::add);

        DocTreeNode root = new DocTreeNode(null);

        for (ModuleIdentity id : moduleIds)
        {
            // TODO Handle exceptions.
            ModuleIdentity loadedId = loadModule(myTopLevel, id.absolutePath());
            assert id.equals(loadedId);

            ModuleDocs docs = instantiateModuleDocs(myTopLevel, id);
            assert docs != null;

            root.addAtPath(id.iterate(), docs);
        }

        return root;
    }
}
