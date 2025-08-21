// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.layout;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.Generator;
import dev.ionfusion.fusion._private.doc.site.Template;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * The HTML layout used for module reference pages.
 */
public final class ModuleLayout
    extends CommonLayout<ModuleEntity>
{
    private final Predicate<ModuleIdentity> myFilter;

    public ModuleLayout(Artifact<ModuleEntity> artifact, Predicate<ModuleIdentity> filter)
    {
        super(artifact);
        myFilter = filter;
    }

    public static Template<ModuleEntity, StreamWriter> template(Predicate<ModuleIdentity> filter)
    {
        return artifact -> new ModuleLayout(artifact, filter);
    }

    @Override
    void addCssUrls(ArrayList<String> urls)
        throws IOException
    {
        super.addCssUrls(urls);
        urls.add("module.css");
    }

    @Override
    String getTitle()
    {
        return getEntity().getIdentity().absolutePath();
    }

    @Override
    Generator<Void> contentGenerator(StreamWriter out)
    {
        return new ModuleWriter(myFilter, out, getEntity());
    }
}
