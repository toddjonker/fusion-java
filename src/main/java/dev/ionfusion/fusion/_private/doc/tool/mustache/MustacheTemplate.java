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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class MustacheTemplate <Entity>
    implements Template<Entity, Path>
{
    private final Path                myTemplateFile;

    public MustacheTemplate(Path templateFile)
    {

        myTemplateFile = templateFile;
    }

    public MustacheTemplate(String templateFile)
    {
        myTemplateFile = Paths.get(templateFile);
    }


    @Override
    public Generator<Path> populate(Artifact<Entity> artifact)
    {
        return dest -> {

            // TODO Hoist these out
            MustacheFactory mf = new DefaultMustacheFactory();
            // The default factory caches compiled templates, so this isn't too bad:
            Mustache mustache = mf.compile(myTemplateFile.toAbsolutePath().toString());

            Map<String, Object> baseScope = new HashMap<>();
            baseScope.put("md", new Function<String, String>() {
                @Override
                public String apply(String s)
                {
                    return new MarkdownProcessor().markdown(s);
                }
            });

            // mustache HTML-encodes the results of {{entity.content}}
            // After invocation, the result is passed through
            //    ObjectHandler.coerce
            //  then ObjectHandler.stringify
            //  then, if ValueCode.encoded, Default[!!!]MustacheFactory.encode()
            //     which

            // TODO title is getting escaped wrongly

            List<Object> scopes = new ArrayList<>();
            scopes.add(baseScope);
            scopes.add(artifact);

            try (Writer writer = Files.newBufferedWriter(dest, UTF_8))
            {
                mustache.execute(writer, scopes);
            }
        };
    }
}
