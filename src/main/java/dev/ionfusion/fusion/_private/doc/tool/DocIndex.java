// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class DocIndex
{
    private final Predicate<ModuleIdentity>            myModuleSelector;
    private final Comparator<String>                   myBoundNameComparator;
    private final Map<ModuleIdentity, ModuleEntity>    myModules = new HashMap<>();
    private final TreeMap<String, Set<ModuleIdentity>> myNameMap;

    /**
     * Sorted by name, using {@link #myBoundNameComparator}.
     */
    private final Map<String, Set<ExportedBinding>>    myExportIndex;

    DocIndex(Predicate<ModuleIdentity> selector)
    {
        myModuleSelector = selector;
        myBoundNameComparator = new BindingComparator();
        myNameMap = new TreeMap<>();
        myExportIndex = new TreeMap<>(myBoundNameComparator);
    }

    public Comparator<String> getBoundNameComparator()
    {
        return myBoundNameComparator;
    }


    public TreeMap<String, Set<ModuleIdentity>> getNameMap()
    {
        return myNameMap;
    }


    public final class AlphaEntry
    {
        private final String                   myName;
        private final Iterable<ModuleIdentity> myModules;

        public AlphaEntry(Map.Entry<String, Set<ModuleIdentity>> entry)
        {
            myName = entry.getKey();
            myModules = entry.getValue();
        }

        public String bindingName()
        {
            return myName;
        }

        public Iterable<ModuleIdentity> modules()
        {
            return myModules;
        }
    }


    public Iterator<AlphaEntry> alphaEntries()
    {
        return myNameMap.entrySet().stream().map(AlphaEntry::new).iterator();
    }


    //========================================================================


    void addModule(ModuleEntity module)
    {
        assert !myModules.containsKey(module.getIdentity());

        myModules.put(module.getIdentity(), module);
        addEntriesForModule(module.getModuleDocs());

        module.exports().forEach(this::addExport);
    }


    private void addEntriesForModule(ModuleDocs model)
    {
        if (model == null) return;

        ModuleIdentity id = model.getIdentity();
        assert myModuleSelector.test(id);

        model.getProvidedNames().forEach(name -> addExport(name, id));
    }


    private void addExport(String name, ModuleIdentity exportingModule)
    {
        Set<ModuleIdentity> ids = myNameMap.computeIfAbsent(name, k -> new TreeSet<>());
        ids.add(exportingModule);
    }


    public Stream<ModuleIdentity> exportsOf(String exportName)
    {
        assert myNameMap.containsKey(exportName);
        return myNameMap.get(exportName).stream();
    }


    private void addExport(ExportedBinding export)
    {
        Set<ExportedBinding> ids =
            myExportIndex.computeIfAbsent(export.getName(),
                                          k -> new TreeSet<>(ExportedBinding.COMPARE_BY_MODULE));
        ids.add(export);
    }


    public PermutedIndex permute()
    {
        PermutedIndex permuted = new PermutedIndex();
        myNameMap.forEach(permuted::addEntries);
        return permuted;
    }

    public List<ExportedBinding> otherExportsOf(ExportedBinding binding)
    {
        assert binding.getName() != null;
        assert myExportIndex.containsKey(binding.getName());

        return myExportIndex.get(binding.getName())
                            .stream()
                            .filter(x -> x != binding)
                            .collect(Collectors.toList());
    }
}
