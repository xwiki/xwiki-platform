package com.xpn.xwiki.export.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.web.ExportURLFactory;

/**
 * Create a ZIP package containing a range of HTML pages with skin and attachment dependencies.
 * 
 * @version $Id: $
 * @since XWiki Platform 1.3M1
 */
public class HtmlPackager
{
    /**
     * A point.
     */
    private static final String POINT = ".";

    /**
     * Name of the context property containing the document.
     */
    private static final String CONTEXT_TDOC = "tdoc";

    /**
     * Name of the Velocity context property containing the document.
     */
    private static final String VCONTEXT_DOC = "doc";

    /**
     * Name of the Velocity context property containing the document.
     */
    private static final String VCONTEXT_CDOC = "cdoc";

    /**
     * Name of the Velocity context property containing the document.
     */
    private static final String VCONTEXT_TDOC = CONTEXT_TDOC;

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
     * The pages to export. A {@link Set} of page name.
     */
    private Set pages = new HashSet();

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
        return name;
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
        return description;
    }

    /**
     * Add a page to export.
     * 
     * @param page the name of the page to export.
     */
    public void addPage(String page)
    {
        this.pages.add(page);
    }

    /**
     * Add a range of pages to export.
     * 
     * @param pages a range od pages to export.
     */
    public void addPages(Collection pages)
    {
        for (Iterator it = pages.iterator(); it.hasNext();) {
            this.pages.add(it.next());
        }
    }

    /**
     * Add rendered document to ZIP stream.
     * 
     * @param pageName the name (used with
     *            {@link com.xpn.xwiki.XWiki.XWiki#getDocument(String, XWikiContext)}) of the page
     *            to render.
     * @param zos the ZIP output stream.
     * @param context the XWiki context.
     * @param vcontext the Velocity context.
     * @throws XWikiException error when rendering document.
     * @throws IOException error when rendering document.
     */
    private void renderDocument(String pageName, ZipOutputStream zos, XWikiContext context,
        VelocityContext vcontext) throws XWikiException, IOException
    {
        XWikiDocument doc = context.getWiki().getDocument(pageName, context);

        String zipname = doc.getDatabase() + POINT + doc.getSpace() + POINT + doc.getName();
        String language = doc.getLanguage();
        if (language != null && language.length() != 0) {
            zipname += POINT + language;
        }

        zipname += ".html";

        ZipEntry zipentry = new ZipEntry(zipname);
        zos.putNextEntry(zipentry);

        context.setDatabase(doc.getDatabase());
        context.setDoc(doc);
        vcontext.put(VCONTEXT_DOC, doc.newDocument(context));
        vcontext.put(VCONTEXT_CDOC, vcontext.get(VCONTEXT_DOC));

        XWikiDocument tdoc = doc.getTranslatedDocument(context);
        context.put(CONTEXT_TDOC, tdoc);
        vcontext.put(VCONTEXT_TDOC, tdoc.newDocument(context));

        String content = context.getWiki().parseTemplate("view.vm", context);

        zos.write(content.getBytes(context.getWiki().getEncoding()));
        zos.closeEntry();
    }

    /**
     * Init provided {@link ExportURLFactory} and add rendered documents to ZIP stream.
     * 
     * @param zos the ZIP output stream.
     * @param tempdir the directory where to copy attached files.
     * @param urlf the {@link com.xpn.xwiki.web.XWikiURLFactory.XWikiURLFactory} used to render the
     *            documents.
     * @param context the XWiki context.
     * @throws XWikiException error when render documents.
     * @throws IOException error when render documents.
     */
    private void renderDocuments(ZipOutputStream zos, File tempdir, ExportURLFactory urlf,
        XWikiContext context) throws XWikiException, IOException
    {
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        Document currentDocument = (Document) vcontext.get(VCONTEXT_DOC);
        Document currentCDocument = (Document) vcontext.get(VCONTEXT_CDOC);
        Document currentTDocument = (Document) vcontext.get(VCONTEXT_TDOC);

        try {
            XWikiContext renderContext = (XWikiContext) context.clone();
            renderContext.put("action", "view");

            vcontext = XWikiVelocityRenderer.prepareContext(renderContext);

            urlf.init(this.pages, tempdir, renderContext);
            renderContext.setURLFactory(urlf);

            for (Iterator it = this.pages.iterator(); it.hasNext();) {
                String pageName = (String) it.next();

                renderDocument(pageName, zos, renderContext, vcontext);
            }
        } finally {
            // Clean velocity context
            vcontext = XWikiVelocityRenderer.prepareContext(context);

            vcontext.put(VCONTEXT_DOC, currentDocument);
            vcontext.put(VCONTEXT_CDOC, currentCDocument);
            vcontext.put(VCONTEXT_TDOC, currentTDocument);
        }
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
            "attachment; filename=" + Util.encodeURI(name, context) + ".zip");
        context.setFinished(true);

        ZipOutputStream zos = new ZipOutputStream(context.getResponse().getOutputStream());

        File dir =
            (File) context.getEngineContext().getAttribute("javax.servlet.context.tempdir");
        File tempdir = new File(dir, RandomStringUtils.randomAlphanumeric(8));
        tempdir.mkdirs();
        File attachmentDir = new File(tempdir, "attachment");
        attachmentDir.mkdirs();

        // Create custom URL factory
        ExportURLFactory urlf = new ExportURLFactory();

        // Render pages to export
        renderDocuments(zos, tempdir, urlf, context);

        // Add required skins to ZIP file
        for (Iterator it = urlf.getNeededSkins().iterator(); it.hasNext();) {
            String skinName = (String) it.next();
            addSkinToZip(skinName, zos, context);
        }

        // Add resources files to ZIP file
        addDirToZip(tempdir, zos, "");

        zos.setComment(description);

        // Finish ZIP file
        zos.finish();
        zos.flush();

        // Delete temporary directory
        deleteDirectory(tempdir);
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

        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (int i = 0; i < files.length; ++i) {
            File file = files[i];

            if (file.isDirectory()) {
                deleteDirectory(file);
                continue;
            }

            file.delete();
        }

        directory.delete();
    }

    /**
     * Add skin to the package in sub-directory "skins".
     * 
     * @param skinName the name of the skin.
     * @param out the ZIP output stream where to put the skin.
     * @param context the XWiki context.
     * @throws IOException error when adding the skin to package.
     */
    private static void addSkinToZip(String skinName, ZipOutputStream out, XWikiContext context)
        throws IOException
    {
        File file =
            new File(context.getWiki().getEngineContext().getRealPath("/skins/" + skinName));
        addDirToZip(file, out, "skins" + ZIPPATH_SEPARATOR + skinName + ZIPPATH_SEPARATOR);
    }

    /**
     * Add a directory and all its sub-directories to the package.
     * 
     * @param directory the directory to add.
     * @param out the ZIP output stream where to put the skin.
     * @param basePath the path where to put the directory in the package.
     * @throws IOException error when adding the directory to package.
     */
    private static void addDirToZip(File directory, ZipOutputStream out, String basePath)
        throws IOException
    {
        if (!directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        byte[] tmpBuf = new byte[1024];

        for (int i = 0; i < files.length; ++i) {
            File file = files[i];
            if (file.isDirectory()) {
                addDirToZip(file, out, basePath + file.getName() + ZIPPATH_SEPARATOR);
                continue;
            }

            FileInputStream in = new FileInputStream(file);

            out.putNextEntry(new ZipEntry(basePath + file.getName()));

            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }

            out.closeEntry();
            in.close();
        }
    }
}
