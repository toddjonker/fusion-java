// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.layout;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.site.Artifact;
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

    public ModuleLayout(Predicate<ModuleIdentity> filter)
    {
        myFilter = filter;
    }


    @Override
    protected Context makeContext(Artifact<ModuleEntity> artifact)
        throws IOException
    {
        return new Context() {
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
                return artifact.getEntity().getIdentity().absolutePath();
            }

            @Override
            void renderContent(StreamWriter out)
                throws IOException
            {
                new ModuleWriter(myFilter, out, artifact.getEntity()).renderModule();
            }
        };
    }
}
