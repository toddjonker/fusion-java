// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.mustache;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.petebevin.markdown.MarkdownProcessor;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.Generator;
import dev.ionfusion.fusion._private.doc.site.Template;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class MustacheTemplate <Entity>
    implements Template<Entity, Path>
{
    private static final MustacheFactory FACTORY = new DefaultMustacheFactory();

    private final Mustache myMustache;

    public MustacheTemplate(Path templateFile)
    {
        myMustache = FACTORY.compile(templateFile.toAbsolutePath().toString());
    }

    public MustacheTemplate(String templateFile)
    {
        this(Paths.get(templateFile));
    }


    /**
     * Variables we want to inject into all templates.
     */
    static class BaseScope
    {
        // Proper rendering relies on this processor being used through
        // the whole page. In particular, code-block detection fails
        // when it's not preceded by other content.
        private final MarkdownProcessor myMarkdownProcessor = new MarkdownProcessor();


        String nl()
        {
            return "\n";
        }

        Function<String, String> md()
        {
            return myMarkdownProcessor::markdown;
        }
    }


    @Override
    public Generator<Path> populate(Artifact<Entity> artifact)
    {
        return dest -> {
            List<Object> scopes = new ArrayList<>();
            scopes.add(new BaseScope());
            scopes.add(artifact);

            Files.createDirectories(dest.getParent());
            try (Writer writer = Files.newBufferedWriter(dest, UTF_8))
            {
                myMustache.execute(writer, scopes);
            }
        };
    }
}
