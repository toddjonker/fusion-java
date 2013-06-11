// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.DocIndex.buildDocIndex;
import static com.amazon.fusion.ModuleDoc.buildDocTree;
import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;
import com.amazon.fusion.ModuleDoc.Filter;
import com.amazon.ion.Timestamp;
import com.petebevin.markdown.MarkdownProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * NOT FOR APPLICATION USE
 */
public final class _Private_ModuleDocumenter
{
    private _Private_ModuleDocumenter() {}


    public static FusionRuntime standardDocumentingRuntime()
    {
        FusionRuntimeBuilder rb = FusionRuntimeBuilder.standard();
        rb.setDocumenting(true);
        return rb.build();
    }

    public static void writeHtmlTree(File outputDir,
                                     File repoDir)
        throws IOException, FusionException
    {
        FusionRuntime runtime = standardDocumentingRuntime();
        Filter filter = new Filter();

        log("Building module docs");
        ModuleDoc doc = buildDocTree(runtime, filter, repoDir);

        log("Writing module docs");
        writeModuleTree(filter, outputDir, ".", doc);

        log("Building indices");
        DocIndex index = buildDocIndex(doc);

        log("Writing indices");
        writeIndexFile(filter, outputDir, index);
        writePermutedIndexFile(filter, outputDir, index);

        log("Writing Markdown pages");
        writeMarkdownPages(outputDir, repoDir);

        log("DONE writing HTML docs to " + outputDir);
    }


    private static void writeModuleTree(Filter filter,
                                        File outputDir,
                                        String baseUrl,
                                        ModuleDoc doc)
        throws IOException
    {
        String name = doc.baseName();
        if (name != null)
        {
            writeModuleFile(filter, outputDir, baseUrl, doc);
            outputDir = new File(outputDir, name);
            baseUrl = baseUrl + "/..";
        }

        Collection<ModuleDoc> submodules = doc.submodules();
        for (ModuleDoc submodule : submodules)
        {
            writeModuleTree(filter, outputDir, baseUrl, submodule);
        }
    }


    private static void writeModuleFile(Filter filter,
                                        File outputDir,
                                        String baseUrl,
                                        ModuleDoc doc)
        throws IOException
    {
        File outputFile = new File(outputDir, doc.baseName() + ".html");

        try (ModuleWriter writer =
                 new ModuleWriter(filter, outputFile, baseUrl))
        {
            writer.renderModule(doc);
        }
    }


    private static void writeIndexFile(Filter filter,
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


    private static void writePermutedIndexFile(Filter filter,
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


    //========================================================================


    private static void writeMarkdownPage(File outputFile, File inputFile)
        throws IOException
    {
        String markdownContent;
        try (FileInputStream inStream = new FileInputStream(inputFile))
        {
            try (Reader inReader = new InputStreamReader(inStream, UTF8))
            {
                markdownContent = FusionUtils.loadReader(inReader);
            }
        }

        final MarkdownProcessor markdowner = new MarkdownProcessor();
        String html = markdowner.markdown(markdownContent);

        try (FileOutputStream outStream = new FileOutputStream(outputFile))
        {
            try (Writer writer = new OutputStreamWriter(outStream, UTF8))
            {
                writer.write(html);
            }
        }
    }


    /**
     * Recursively discover {@code .md} files and transform to {@code .html}.
     */
    private static void writeMarkdownPages(File outputDir, File repoDir)
        throws IOException
    {
        String[] fileNames = repoDir.list();

        for (String fileName : fileNames)
        {
            File repoFile = new File(repoDir, fileName);

            if (fileName.endsWith(".md"))
            {
                String docName = fileName.substring(0, fileName.length() - 2);
                File outputFile = new File(outputDir, docName + "html");
                writeMarkdownPage(outputFile, repoFile);
            }
            else if (repoFile.isDirectory())
            {
                File subOutputDir = new File(outputDir, fileName);
                writeMarkdownPages(subOutputDir, repoFile);
            }
        }
    }


    //========================================================================


    private static final class ModuleWriter
        extends HtmlWriter
    {
        private final Filter myFilter;
        private final String myBaseUrl;
        private final MarkdownProcessor myMarkdown = new MarkdownProcessor();

        public ModuleWriter(Filter filter, File outputFile, String baseUrl)
            throws IOException
        {
            super(outputFile);
            myFilter  = filter;
            myBaseUrl = baseUrl;
        }

        private static final String STYLE =
            "<style type='text/css'>" +
            " .indexlink {" +
            "   float: right;" +
            " }" +
            " .submodules .oneliner p {" +
            "   display: inline;" +
            " }" +
            " .exports {" +
            "   display: block; width: 100%;" +
            "   margin-left: 1em; margin-bottom: 1em" +
            " }" +
            " .binding {" +
            "   display: block;" +
            "   margin-left: 1em;" +
            " }" +
            " .binding .name {" +
            "   font-size: 1.17em;" +
            "   font-weight: bold;" +
            "   margin-left: -1em;" +
            " }" +
            " .binding .kind {" +
            "   float: right; font-style: italic;" +
            " }" +
            " .binding .oneliner {" +
//            "   margin-left: 1em;" +
            " }" +
            " .binding .nodoc {" +
            "   font-style: italic;" +
            " }" +
            " .binding .body {" +
//            "   margin-left: 1em;" +
            " }" +
            " .binding .also {" +
//            "   margin-left: 1em;" +
            " }" +
            "</style>\n";

        void renderModule(ModuleDoc doc)
            throws IOException
        {
            String modulePath = doc.myModuleId.internString();
            renderHead(modulePath, myBaseUrl, STYLE);

            append("<div class='indexlink'>" +
                   "<a href='index.html'>Top</a> " +
                   "<a href='binding-index.html'>Binding Index</a> " +
                   "(<a href='permuted-index.html'>Permuted</a>)" +
                   "</div>\n");

            renderHeader1("Module " + modulePath);

            renderModuleIntro(doc);

            renderSubmoduleLinks(doc);

            String[] names = doc.sortedExportedNames();

            renderBindings(doc, names);
        }


        private void renderModuleIntro(ModuleDoc doc)
            throws IOException
        {
            if (doc.myIntroDocs != null)
            {
                markdown(doc.myIntroDocs);
            }
        }

        private void renderSubmoduleLinks(ModuleDoc doc)
            throws IOException
        {
            Map<String, ModuleDoc> submodules = doc.submoduleMap();
            if (submodules == null) return;

            renderHeader2("Submodules");

            String[] names = submodules.keySet().toArray(new String[0]);
            Arrays.sort(names);

            append("<ul class='submodules'>");
            for (String name : names)
            {
                ModuleDoc sub = submodules.get(name);

                String escapedName = escapeString(name);
                append("<li>");
                linkToModule(sub.myModuleId, escapedName);

                String oneLiner = sub.oneLiner();
                if (oneLiner != null)
                {
                    append(" &ndash; <span class='oneliner'>");
                    markdown(oneLiner);
                    append("</span>");
                }
                append("</li>\n");
            }
            append("</ul>\n");
        }


        private void renderBindingIndex(ModuleDoc doc, String[] names)
            throws IOException
        {
            if (names.length == 0) return;

            append("<div class='exports'>\n");
            for (String name : names)
            {
                String escapedName = escapeString(name);
                linkToBindingAsName(doc.myModuleId, escapedName);
                append("&nbsp;&nbsp;\n");
            }
            append("</div>\n");
        }


        private void renderBindings(ModuleDoc doc, String[] names)
            throws IOException
        {
            Map<String, BindingDoc> bindings = doc.bindingMap();
            if (bindings == null) return;

            renderHeader2("Exported Bindings");

            renderBindingIndex(doc, names);

            for (String name : names)
            {
                // May be null:
                BindingDoc binding = bindings.get(name);
                renderBinding(doc, name, binding);
            }
        }


        /* CSS hierarchy:
         *
         *  binding
         *    name
         *    kind
         *    oneliner
         *    body
         *    also
         */
        private void renderBinding(ModuleDoc moduleDoc,
                                   String name, BindingDoc doc)
            throws IOException
        {
            String escapedName = escapeString(name);

            append("\n<div class='binding'><span class='name'><a name='");
            append(escapedName);
            append("'>");
            append(escapedName);
            append("</a></span>");   // binding div is still open

            if (doc == null)
            {
                append("<p class='nodoc'>No documentation available.<p>\n\n");
            }
            else
            {
                if (doc.getKind() != null)
                {
                    append("<span class='kind'>");
                    // Using enum toString() allows display name to be changed
                    append(doc.getKind().toString().toLowerCase());
                    append("</span>\n");
                }

                StringBuilder buf = new StringBuilder();

                if (doc.getUsage() != null)
                {
                    buf.append("    ");
                    buf.append(doc.getUsage());
                    buf.append('\n');

                    append("<div class='oneliner'>");
                    markdown(buf.toString());
                    append("</div>\n");

                    buf.setLength(0);
                }

                if (doc.getBody() != null)
                {
                    buf.append('\n');
                    buf.append(doc.getBody());
                    buf.append('\n');

                    append("<div class='body'>");
                    markdown(buf.toString());
                    append("</div>\n");

                    buf.setLength(0);
                }

                append('\n');


                ModuleIdentity[] ids =
                    doc.getProvidingModules().toArray(new ModuleIdentity[0]);
                Arrays.sort(ids);

                boolean printedOne = false;
                for (ModuleIdentity id : ids)
                {
                    if (id != moduleDoc.myModuleId && myFilter.accept(id))
                    {
                        if (printedOne)
                        {
                            append(", ");
                        }
                        else
                        {
                            append("<p class='also'><em>Also provided by ");
                        }

                        linkToBindingAsModulePath(id, escapedName);
                        printedOne = true;
                    }
                }
                if (printedOne)
                {
                    append("</em></p>\n");
                }
            }

            append("</div>\n"); // binding
        }


        private void markdown(String text)
            throws IOException
        {
            String md = myMarkdown.markdown(text);
            append(md);
        }
    }


    //========================================================================


    private static final class IndexWriter
        extends HtmlWriter
    {
        private final Filter myFilter;


        public IndexWriter(Filter filter, File outputFile)
            throws IOException
        {
            super(outputFile);
            myFilter = filter;
        }


        private static final String STYLE =
            "<style type='text/css'>" +
            " .indexlink {" +
            "   float: right;" +
            " }" +
            " td.bound {" +
            "   font-family: monospace;" +
            " }" +
            "</style>\n";

        void renderIndex(DocIndex index)
            throws IOException
        {
            renderHead("Fusion Binding Index", null, STYLE);

            append("<div class='indexlink'>" +
                   "<a href='index.html'>Top</a> " +
                   "<a href='permuted-index.html'>Permuted Index</a>" +
                   "</div>\n");

            renderHeader1("Binding Index");

            append("<table>");
            for (Entry<String, Set<ModuleIdentity>> entry
                    : index.getNameMap().entrySet())
            {
                String escapedName = escapeString(entry.getKey());
                append("<tr><td class='bound'>");
                append(escapedName);
                append("</td><td>");

                boolean printedOne = false;
                for (ModuleIdentity id : entry.getValue())
                {
                    if (myFilter.accept(id))
                    {
                        if (printedOne)
                        {
                            append(", ");
                        }
                        linkToBindingAsModulePath(id, escapedName);
                        printedOne = true;
                    }
                }

                append("</td>\n");
            }
            append("</table>\n");
        }
    }


    //========================================================================


    private static final class PermutedIndexWriter
        extends HtmlWriter
    {
        private final Filter   myFilter;
        private final DocIndex myIndex;

        /** Maps keywords to the lines in which they exist. */
        private final TreeSet<Line> myLines;


        /** An index line. */
        private static final class Line
            implements Comparable<Line>
        {
            private final String myPrefix;
            private final String myKeyword;
            private final Entry<String, Set<ModuleIdentity>> myEntry;


            Line(Entry<String, Set<ModuleIdentity>> entry,
                 int keywordStartPos,
                 int keywordLimitPos)
            {
                String name = entry.getKey();
                myPrefix  = name.substring(0, keywordStartPos);
                myKeyword = name.substring(keywordStartPos, keywordLimitPos);
                myEntry   = entry;
            }

            String bindingName()
            {
                return myEntry.getKey();
            }

            String prefix()
            {
                return myPrefix;
            }

            String keyword()
            {
                return myKeyword;
            }

            String suffix()
            {
                int pos = myPrefix.length() + myKeyword.length();
                return bindingName().substring(pos);
            }

            Set<ModuleIdentity> modules()
            {
                return myEntry.getValue();
            }

            @Override
            public int compareTo(Line that)
            {
                int result = myKeyword.compareTo(that.myKeyword);
                if (result == 0)
                {
                    result = myPrefix.compareTo(that.myPrefix);
                    if (result == 0)
                    {
                        // We shouldn't get this far often, so we spend time to
                        // get the suffix rather that memory to cache it.
                        result = suffix().compareTo(that.suffix());
                    }
                }
                return result;
            }
        }


        public PermutedIndexWriter(Filter filter, DocIndex index,
                                   File outputFile)
            throws IOException
        {
            super(outputFile);

            myFilter = filter;
            myIndex  = index;
            myLines  = new TreeSet<>();
        }


        private void permute()
        {
            for (Entry<String, Set<ModuleIdentity>> entry
                     : myIndex.getNameMap().entrySet())
            {
                String name = entry.getKey();

                int pos = 0;
                while (true)
                {
                    int underscorePos = name.indexOf('_', pos);
                    if (underscorePos == -1)
                    {
                        myLines.add(new Line(entry, pos, name.length()));
                        break;
                    }
                    else if (pos < underscorePos)
                    {
                        myLines.add(new Line(entry, pos, underscorePos));
                    }
                    pos = underscorePos + 1;
                }
            }
        }


        private static final String STYLE =
            "<style type='text/css'>" +
            " .indexlink {" +
            "   float: right;" +
            " }" +
            " td.prefix {" +
            "   font-family: monospace;" +
            "   text-align: right;" +
            "   padding-right: 0;" +
            " }" +
            " td.tail {" +
            "   font-family: monospace;" +
            "   padding-left: 0;" +
            " }" +
            " td .keyword {" +
            "   font-weight: bold;" +
            " }" +
            "</style>\n";

        void renderIndex()
            throws IOException
        {
            permute();

            renderHead("Fusion Binding Index (Permuted)", null, STYLE);

            append("<div class='indexlink'>" +
                   "<a href='index.html'>Top</a> " +
                   "<a href='binding-index.html'>Alphabetical Index</a>" +
                   "</div>\n");

            renderHeader1("Permuted Binding Index");

            append("<table>");
            for (Line line : myLines)
            {
                String escapedName = escapeString(line.bindingName());

                append("<tr><td class='prefix'>");
                escape(line.prefix());
                append("</td><td class='tail'><span class='keyword'>");
                escape(line.keyword());
                append("</span>");
                escape(line.suffix());
                append("</td><td>");

                boolean printedOne = false;
                for (ModuleIdentity id : line.modules())
                {
                    if (myFilter.accept(id))
                    {
                        if (printedOne)
                        {
                            append(", ");
                        }
                        linkToBindingAsModulePath(id, escapedName);
                        printedOne = true;
                    }
                }

                append("</td>\n");
            }
            append("</table>\n");
        }
    }


    private static void log(String message)
    {
        System.out.print(Timestamp.now());
        System.out.print(" ");
        System.out.println(message);
    }
}
