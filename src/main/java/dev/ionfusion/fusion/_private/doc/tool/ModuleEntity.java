// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static java.util.Comparator.comparing;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Holds aggregated documentation and cross-references for a module.
 */
public class ModuleEntity
{
    private final DocIndex                  myIndex;
    private final ModuleIdentity            myModuleIdentity;
    private       ModuleDocs                myModuleDocs;

    /** Sorted by {@class ModuleIdentity}. */
    private final SortedSet<ModuleEntity> mySubmodules;

    private final SortedMap<String, ExportedBinding> myExportedBindings;


    ModuleEntity(DocIndex index, ModuleIdentity id)
    {
        myIndex = index;
        myModuleIdentity = id;
        mySubmodules = new TreeSet<>(comparing(ModuleEntity::getIdentity));
        myExportedBindings = new TreeMap<>(index.getBoundNameComparator());
    }


    public DocIndex getIndex()
    {
        return myIndex;
    }

    public ModuleIdentity getIdentity()
    {
        return myModuleIdentity;
    }


    void setModuleDocs(ModuleDocs docs)
    {
        assert myModuleDocs == null;
        assert docs.getIdentity() == myModuleIdentity;
        myModuleDocs = docs;

        docs.getBindingDocs().forEach(this::addExport);
    }

    /**
     * @return not null.
     */
    public ModuleDocs getModuleDocs()
    {
        return (myModuleDocs != null ? myModuleDocs : new ModuleDocs(myModuleIdentity));
    }


    //=========================================================================
    // Submodules

    void addSubmodule(ModuleEntity child)
    {
        assert child.getIdentity().parent() == myModuleIdentity;
        mySubmodules.add(child);
    }

    /**
     * Sorted by name.
     *
     * @return not null.
     */
    public Collection<ModuleEntity> submodules()
    {
        return mySubmodules;
    }


    //=========================================================================
    // Exported bindings

    private void addExport(String name, BindingDoc export)
    {
        ExportedBinding eb = new ExportedBinding(this, name, export);
        myExportedBindings.put(name, eb);
    }

    /**
     * Gets the docs for exported bindings, sorted by name.
     *
     * @return not null
     */
    public Collection<ExportedBinding> exports()
    {
        return myExportedBindings.values();
    }
}
