// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceName;
import java.io.File;
import java.net.URL;

/**
 * Abstract location of module source code.
 * <p>
 * These are produced by a {@link ModuleRepository} upon module discovery,
 * encapsulating the physical location of the module code.  They are consumed by
 * the {@link LoadHandler}, when loading the module.
 * <p>
 * "ModuleResource" might be a better name, or perhaps "ModuleCodeProvider".
 * <p>
 * TODO: {@link SourceName} should track the repository holding the module.
 *    This would enable better messaging, cover Jar-based resources nicely, and
 *    allow the coverage report to partition source-files by repo root.
 */
class ModuleLocation
{
    private final SourceName myName;

    private ModuleLocation(SourceName name)
    {
        assert name.getModuleIdentity() != null;
        assert name.getResourceId() != null;
        myName = name;
    }


    /**
     * @return not null.
     */
    final SourceName sourceName()
    {
        return myName;
    }


    @Override
    public String toString()
    {
        return myName.toString();
    }


    /**
     * @param id must not be null.
     * @param sourceFile must not be null.
     * @return a new {@link ModuleLocation}.
     */
    static ModuleLocation forFile(ModuleIdentity id, File sourceFile)
    {
        return new ModuleLocation(SourceName.forModule(id, sourceFile));
    }

    /**
     * @param id must not be null.
     * @param url must not be null.
     * @return a new {@link ModuleLocation}.
     */
    static ModuleLocation forUrl(ModuleIdentity id, URL url)
    {
        return new ModuleLocation(SourceName.forUrl(id, url));
    }
}
