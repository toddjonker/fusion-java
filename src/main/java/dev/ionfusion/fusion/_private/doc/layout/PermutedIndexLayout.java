// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.layout;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.tool.DocIndex;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Predicate;

public final class PermutedIndexLayout
    extends CommonLayout<DocIndex>
{
    private final Predicate<ModuleIdentity> myFilter;

    public PermutedIndexLayout(Predicate<ModuleIdentity> filter)
    {
        myFilter = filter;
    }


    @Override
    protected Context makeContext(Artifact<DocIndex> artifact)
        throws IOException
    {
        DocIndex index = artifact.getEntity();

        return new Context()
        {
            @Override
            void addCssUrls(ArrayList<String> urls)
                throws IOException
            {
                super.addCssUrls(urls);
                urls.add("index.css");
            }

            @Override
            String getTitle()
                throws IOException
            {
                return "Fusion Binding Index (Permuted)";
            }

            @Override
            void renderContent(StreamWriter out)
                throws IOException
            {
                new PermutedIndexWriter(myFilter, index, out).renderIndex();
            }
        };
    }
}
