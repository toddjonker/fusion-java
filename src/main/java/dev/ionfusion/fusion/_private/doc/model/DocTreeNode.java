// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Collects a tree of documentation nodes, generally for a module hierarchy.
 */
public final class DocTreeNode
{
    private final String                   myName;
    private       ModuleDocs               myModuleDocs;
    private final Map<String, DocTreeNode> mySubmodules;


    //========================================================================


    public DocTreeNode(String name)
    {
        myName = name;
        myModuleDocs = null;
        mySubmodules = new HashMap<>();
    }


    public String baseName()
    {
        return myName;
    }

    public ModuleDocs getModuleDocs()
    {
        return myModuleDocs;
    }

    public Map<String, DocTreeNode> submoduleMap()
    {
        return mySubmodules;
    }

    public Collection<DocTreeNode> submodules()
    {
        return mySubmodules.values();
    }


    public void addAtPath(Iterator<String> path, ModuleDocs docs)
    {
        if (path.hasNext())
        {
            String childName = path.next();
            mySubmodules.compute(childName, (n, child) -> {
                if (child == null)
                {
                    child = new DocTreeNode(childName);
                }
                child.addAtPath(path, docs);
                return child;
            });
        }
        else
        {
            assert myModuleDocs == null;
            myModuleDocs = docs;
        }
    }
}
