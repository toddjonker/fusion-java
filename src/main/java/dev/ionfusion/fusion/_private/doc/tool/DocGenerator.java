// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._private.doc.tool.DocIndex.buildDocIndex;

import com.amazon.ion.Timestamp;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.doc.model.ArticleEntity;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * NOT FOR APPLICATION USE
 */
public final class DocGenerator
{
    /** HTML content for the masthead links */
    static final String HEADER_LINKS =
        "<div class='indexlink'>" +
        "<a href='index.html'>Top</a> " +
        "<a href='binding-index.html'>Binding Index</a> " +
        "(<a href='permuted-index.html'>Permuted</a>)" +
        "</div>\n";

    private DocGenerator() {}


    public static void writeHtmlTree(FusionRuntime runtime,
                                     File outputDir,
                                     File repoDir,
                                     Predicate<ModuleIdentity> filter)
        throws IOException, FusionException
    {
        log("Building module docs");
        RepoEntity repo = new RepoEntity(repoDir.toPath(), filter, runtime.makeTopLevel());

        log("Writing module docs");
        writeModules(repo.getModules(), filter, outputDir);

        log("Building indices");
        DocIndex index = buildDocIndex(repo.getModules());

        log("Writing indices");
        writeIndexFile(filter, outputDir, index);
        writePermutedIndexFile(filter, outputDir, index);

        log("Writing Markdown pages");
        renderArticles(outputDir.toPath(), repo.getArticles());

        log("DONE writing HTML docs to " + outputDir);
    }


    private static void writeModules(Set<ModuleEntity> modules,
                                     Predicate<ModuleIdentity> filter,
                                     File siteDir)
        throws IOException
    {
        for (ModuleEntity module : modules)
        {
            ModuleIdentity id = module.getIdentity();

            StringBuilder baseUrl   = new StringBuilder(".");
            File          moduleDir = siteDir;
            for (Iterator<String> i = id.iterate(); i.hasNext(); )
            {
                String name = i.next();

                if (i.hasNext())
                {
                    baseUrl.append("/..");
                    moduleDir = new File(moduleDir, name);
                }
            }

            writeModuleFile(filter, moduleDir, baseUrl.toString(), module);
        }
    }


    private static void writeModuleFile(Predicate<ModuleIdentity> filter,
                                        File outputDir,
                                        String baseUrl,
                                        ModuleEntity doc)
        throws IOException
    {
        ModuleIdentity id = doc.getIdentity();
        File outputFile = new File(outputDir, id.baseName() + ".html");

        try (ModuleWriter writer =
                 new ModuleWriter(filter, outputFile, baseUrl, doc))
        {
            writer.renderModule();
        }
    }


    /**
     * Renders a set of articles to their corresponding locations in a baseDir.
     *
     * @param baseDir the file-system directory in which to write files.
     * @param articles the entities to render as files, keyed by their relative path.
     */
    private static void renderArticles(Path baseDir, Map<Path, ArticleEntity> articles)
        throws IOException
    {
        for (Map.Entry<Path, ArticleEntity> entry : articles.entrySet())
        {
            renderArticle(baseDir, entry.getKey(), entry.getValue());
        }
    }

    private static void renderArticle(Path baseDir, Path location, ArticleEntity article)
        throws IOException
    {
        Path emptyPath = Paths.get("");

        String fileName = location.getFileName().toString() + ".html";
        Path   parent   = location.getParent();
        if (parent == null)
        {
            parent = emptyPath;
        }

        // basePath leads up to the baseDir
        Path basePath = parent.relativize(emptyPath);
        Path outDir   = baseDir.resolve(parent);

        try (HtmlWriter writer = new HtmlWriter(outDir.toFile(), fileName))
        {
            CommonPageLayout layout = new CommonPageLayout(article.getTitle(),
                                                           basePath.toString(),
                                                           "common.css", "doc.css"
            );
            layout.render(writer, article::render);
        }
    }


    private static void writeIndexFile(Predicate<ModuleIdentity> filter,
                                       File outputDir,
                                       DocIndex index)
        throws IOException
    {
        File outputFile = new File(outputDir, "binding-index.html");

        try (IndexWriter writer = new IndexWriter(filter, outputFile))
        {
            writer.renderIndex(index);
        }
    }


    private static void writePermutedIndexFile(Predicate<ModuleIdentity> filter,
                                               File outputDir,
                                               DocIndex index)
        throws IOException
    {
        File outputFile = new File(outputDir, "permuted-index.html");

        try (PermutedIndexWriter writer =
                 new PermutedIndexWriter(filter, index, outputFile))
        {
            writer.renderIndex();
        }
    }


    private static void log(String message)
    {
        System.out.print(Timestamp.now());
        System.out.print(" ");
        System.out.println(message);
    }
}
