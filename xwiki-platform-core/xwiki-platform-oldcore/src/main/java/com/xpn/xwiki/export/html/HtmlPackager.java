/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.export.html;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Provider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.url.URLContextManager;
import org.xwiki.url.filesystem.FilesystemExportContext;
import org.xwiki.url.internal.filesystem.FilesystemExportContextProvider;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.ExportURLFactory;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletResponseStub;

/**
 * Create a ZIP package containing a range of HTML pages with skin and attachment dependencies.
 *
 * @version $Id$
 * @since 1.3M1
 */
public class HtmlPackager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlPackager.class);

    /**
     * A point.
     */
    private static final String POINT = ".";

    /**
     * Name of the context property containing the document.
     */
    private static final String CONTEXT_TDOC = "tdoc";

    /**
     * The separator in an internal zip path.
     */
    private static final String ZIPPATH_SEPARATOR = "/";

    /**
     * The name of the package for which packager append ".zip".
     */
    private String name = "html.export";

    /**
     * A description of the package.
     */
    private String description = "";

    /**
     * The references to the pages to export.
     */
    private Set<DocumentReference> pageReferences = new HashSet<>();

    /**
     * Used to get the temporary directory.
     */
    private Environment environment = Utils.getComponent((Type) Environment.class);

    private EntityReferenceSerializer<String> pathEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "path");

    /**
     * Modify the name of the package for which packager append ".zip".
     *
     * @param name the name of the page.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the name of the package for which packager append ".zip".
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Modify the description of the package.
     *
     * @param description the description of the package.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the description of the package.
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Add a page to export.
     *
     * @param page the name of the page to export.
     * @deprecated since 8.4.5/9.0, use {@link #addPageReference(DocumentReference)} instead
     */
    @Deprecated
    public void addPage(String page)
    {
        this.pageReferences.add(resolvePage(page));
    }

    /**
     * Add a range of pages to export.
     *
     * @param pages a range of pages to export.
     * @deprecated since 8.4.5/9.0, use {@link #addPageReferences(Collection)} instead
     */
    @Deprecated
    public void addPages(Collection<String> pages)
    {
        for (String page : pages) {
            this.pageReferences.add(resolvePage(page));
        }
    }

    /**
     * Add a page to export.
     *
     * @param pageReference the reference of the page to export.
     * @since 8.4.5
     * @since 9.0
     */
    public void addPageReference(DocumentReference pageReference)
    {
        this.pageReferences.add(pageReference);
    }

    /**
     * Add a range of pages to export.
     *
     * @param pageReferences a range of page references to export.
     * @since 8.4.5
     * @since 9.0
     */
    public void addPageReferences(Collection<DocumentReference> pageReferences)
    {
        this.pageReferences.addAll(pageReferences);
    }

    private DocumentReference resolvePage(String pageName)
    {
        DocumentReferenceResolver<String> resolver =
            Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        return resolver.resolve(pageName);
    }

    /**
     * Add rendered document to ZIP stream.
     *
     * @param pageReference the reference of the page to render.
     * @param zos the ZIP output stream.
     * @param exportContext the context object for the export
     * @param context the clean XWiki context for rendering
     * @throws XWikiException error when rendering document.
     * @throws IOException error when rendering document.
     */
    private void renderDocument(DocumentReference pageReference, ZipOutputStream zos,
        FilesystemExportContext exportContext, XWikiContext context) throws XWikiException, IOException
    {
        XWikiDocument doc = context.getWiki().getDocument(pageReference, context);

        if (doc.isNew()) {
            // Skip non-existing documents.
            return;
        }

        // Compute the location of the page inside the zip. We put pages inside directories for scalability as
        // otherwise on some OS we wouldn't be able to unzip if there are pages having a path longer than 255 chars...
        String zipname = "pages/" + this.pathEntityReferenceSerializer.serialize(pageReference);
        String language = doc.getLanguage();
        if (language != null && language.length() != 0) {
            zipname += POINT + language;
        }
        zipname += ".html";

        ZipEntry zipentry = new ZipEntry(zipname);
        zos.putNextEntry(zipentry);

        try {
            context.setWikiId(doc.getDocumentReference().getWikiReference().getName());
            context.setDoc(doc);

            XWikiDocument tdoc = doc.getTranslatedDocument(context);
            context.put(CONTEXT_TDOC, tdoc);

            // Since we're putting the document in subdirectories inside the zip (under the top level "pages" directory
            // and since we're also putting all resources used by that document inside some top level directory, we
            // need to adjust all the computed relative URLs
            exportContext.setDocParentLevels(computeDocumentDepth(doc.getDocumentReference()));

            String content = evaluateDocumentContent(context);

            zos.write(content.getBytes(context.getWiki().getEncoding()));
            zos.closeEntry();
        } catch (Exception e) {
            throw new IOException(String.format("Failed to render document [%s] for HTML export", pageReference), e);
        }
    }

    private int computeDocumentDepth(EntityReference reference)
    {
        int depth = 0;
        EntityReference currentReference = reference;
        while (currentReference != null) {
            currentReference = currentReference.getParent();
            depth++;
        }
        return depth;
    }

    private String evaluateDocumentContent(XWikiContext context) throws IOException
    {
        context.getWiki().getPluginManager().beginParsing(context);
        Utils.enablePlaceholders(context);
        String content;
        try {
            content = context.getWiki().evaluateTemplate("view.vm", context);
            content = Utils.replacePlaceholders(content, context);
        } finally {
            Utils.disablePlaceholders(context);
        }
        content = context.getWiki().getPluginManager().endParsing(content.trim(), context);
        return content;
    }

    /**
     * Init provided {@link ExportURLFactory} and add rendered documents to ZIP stream.
     *
     * @param zos the ZIP output stream.
     * @param urlf the {@link com.xpn.xwiki.web.XWikiURLFactory} used to render the documents.
     * @param context the XWiki context.
     * @throws XWikiException error when render documents.
     * @throws IOException error when render documents.
     */
    private void renderDocuments(ZipOutputStream zos, ExportURLFactory urlf, FilesystemExportContext exportContext,
        XWikiContext context) throws XWikiException, IOException
    {
        ExecutionContextManager ecm = Utils.getComponent(ExecutionContextManager.class);

        for (DocumentReference pageReference : this.pageReferences) {
            try {
                // Isolate and initialize Contexts
                XWikiContext renderContext = initializeContexts(ecm, urlf, exportContext, context);

                renderDocument(pageReference, zos, urlf.getFilesystemExportContext(), renderContext);
            } catch (ExecutionContextException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT, XWikiException.ERROR_XWIKI_INIT_FAILED,
                    "Failed to initialize Execution Context", e);
            } finally {
                // Clean up context
                ecm.popContext();
            }
        }
    }

    private XWikiContext initializeContexts(ExecutionContextManager ecm, ExportURLFactory urlf,
        FilesystemExportContext exportContext, XWikiContext originalContext) throws ExecutionContextException
    {
        XWikiContext renderContext = originalContext.clone();

        ExecutionContext executionContext = new ExecutionContext();

        // Bridge with old XWiki Context, required for legacy code.
        renderContext.declareInExecutionContext(executionContext);

        // Push a clean new Execution Context since we don't want the main Execution Context to be used for
        // rendering the HTML pages to export. It's cleaner to isolate it as we do.
        ecm.pushContext(executionContext, false);

        // Set the export context lost by the context push
        FilesystemExportContextProvider.set(executionContext, exportContext);

        // Override the current action to ensure we always render a view action.
        renderContext.put("action", "view");

        // Set the URL Factories/Serializer to use
        renderContext.setURLFactory(urlf);

        // Use the filesystem URL format for all code using the url module to generate URLs (webjars, etc).
        Utils.getComponent(URLContextManager.class).setURLFormatId("filesystem");

        // We don't want rendering of pages to output anything in the current servlet response
        // since we'll send the zip in that response.
        // for example, if the page does a redirect, it would write to the servlet output stream and ruin it
        // since we can't write to a stream in which a redirect has been done (it's committed) and thus
        // we won't be able to return our zip in this case.
        renderContext.setResponse(new XWikiServletResponseStub());

        return renderContext;
    }

    /**
     * Apply export and create the ZIP package.
     *
     * @param context the XWiki context used to render pages.
     * @throws IOException error when creating the package.
     * @throws XWikiException error when render the pages.
     */
    public void export(XWikiContext context) throws IOException, XWikiException
    {
        context.getResponse().setContentType("application/zip");
        context.getResponse().addHeader("Content-disposition",
            "attachment; filename=" + Util.encodeURI(this.name, context) + ".zip");
        context.setFinished(true);

        File dir = this.environment.getTemporaryDirectory();
        File tempdir = new File(dir, RandomStringUtils.secure().nextAlphanumeric(8));
        tempdir.mkdirs();

        try {
            File attachmentDir = new File(tempdir, "attachment");
            attachmentDir.mkdirs();

            // Create and initialize a custom URL factory
            ExportURLFactory urlf = new ExportURLFactory();
            Provider<FilesystemExportContext> exportContextProvider =
                Utils.getComponent(new DefaultParameterizedType(null, Provider.class, FilesystemExportContext.class));
            // Note that the following line will set a FilesystemExportContext instance in the Execution Context
            // and this Execution Context will be cloned for each document rendered below.
            // TODO: to be cleaner we should set the FilesystemExportContext in the EC used to render each document.
            // However we also need to initialize the ExportURLFactory.
            FilesystemExportContext exportContext = exportContextProvider.get();
            urlf.init(this.pageReferences, tempdir, exportContext, context);

            ZipOutputStream zos = new ZipOutputStream(context.getResponse().getOutputStream());

            // Render pages to export
            renderDocuments(zos, urlf, exportContext, context);

            // Add required skins to ZIP file
            for (String skinName : urlf.getFilesystemExportContext().getNeededSkins()) {
                addSkinToZip(skinName, zos, urlf.getFilesystemExportContext().getExportedSkinFiles(), context);
            }

            // Copy generated files in the ZIP file.
            addDirToZip(tempdir, TrueFileFilter.TRUE, zos, "", null);

            // Generate an index page
            generateIndexPage(zos, context);

            zos.setComment(this.description);

            // Finish ZIP file
            zos.finish();
            zos.flush();
        } finally {
            // Delete temporary directory
            deleteDirectory(tempdir);
        }
    }

    private void generateIndexPage(ZipOutputStream zos, XWikiContext context) throws IOException
    {
        StringBuilder builder = new StringBuilder();
        builder.append("""
            <!DOCTYPE HTML>
            <html lang="en-US">
                <head>
                    <meta charset="UTF-8">
                    <title>Export Index</title>
                </head>
                <body>
                  <ul>
            """);

        for (DocumentReference reference : this.pageReferences) {
            builder.append("        <li><a href=\"");
            builder.append("pages/");
            // Compute the relative URL corresponding to the path.
            String relativeURL = new File("").toURI()
                .relativize(new File(this.pathEntityReferenceSerializer.serialize(reference)).toURI()).toString();
            builder.append(relativeURL);
            builder.append(".html");
            builder.append("\">");
            builder.append(reference.toString());
            builder.append("</a></li>\n");
        }

        builder.append("""
                  </ul>
                </body>
            </html>
            """);

        ZipEntry zipentry = new ZipEntry("index.html");
        zos.putNextEntry(zipentry);
        zos.write(builder.toString().getBytes(context.getWiki().getEncoding()));
        zos.closeEntry();
    }

    /**
     * Delete a directory and all with all it's content.
     *
     * @param directory the directory to delete.
     */
    private static void deleteDirectory(File directory)
    {
        if (!directory.isDirectory()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.error("Failed to delete HTML export temporary directory", e);
        }
    }

    /**
     * Add skin to the package in sub-directory "skins".
     *
     * @param skinName the name of the skin.
     * @param out the ZIP output stream where to put the skin.
     * @param context the XWiki context.
     * @throws IOException error when adding the skin to package.
     */
    private static void addSkinToZip(String skinName, ZipOutputStream out, Collection<String> exportedSkinFiles,
        XWikiContext context) throws IOException
    {
        // Protect against non-existing skins.
        String realPath = context.getWiki().getEngineContext().getRealPath("/skins/" + skinName);
        if (realPath != null) {
            File file = new File(realPath);

            // Don't include vm and LESS files by default
            FileFilter filter =
                new NotFileFilter(new SuffixFileFilter(new String[]{ ".vm", ".less", "skin.properties" }));

            addDirToZip(file, filter, out, "skins" + ZIPPATH_SEPARATOR + skinName + ZIPPATH_SEPARATOR,
                exportedSkinFiles);
        }
    }

    /**
     * Add a directory and all its sub-directories to the package.
     *
     * @param directory the directory to add.
     * @param filter the files to include or exclude from the copy
     * @param out the ZIP output stream where to put the skin.
     * @param basePath the path where to put the directory in the package.
     * @throws IOException error when adding the directory to package.
     */
    private static void addDirToZip(File directory, FileFilter filter, ZipOutputStream out, String basePath,
        Collection<String> exportedSkinFiles) throws IOException
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Adding dir [" + directory.getPath() + "] to the Zip file being generated.");
        }

        if (!directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles(filter);

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                addDirToZip(file, filter, out, basePath + file.getName() + ZIPPATH_SEPARATOR, exportedSkinFiles);
            } else {
                String path = basePath + file.getName();

                if (exportedSkinFiles != null && exportedSkinFiles.contains(path)) {
                    continue;
                }

                // Starts a new Zip entry. It automatically closes the previous entry if present.
                out.putNextEntry(new ZipEntry(path));

                try {
                    FileUtils.copyFile(file, out);
                } finally {
                    out.closeEntry();
                }
            }
        }
    }
}
