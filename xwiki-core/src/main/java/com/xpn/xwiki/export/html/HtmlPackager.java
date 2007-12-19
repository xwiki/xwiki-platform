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
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.web.ExportURLFactory;

/**
 * Create a zip package containing a range of HTML pages with skin and attachment dependencies.
 * 
 * @version $Id: $
 */
public class HtmlPackager
{
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
     * Apply export and create the ZIP package.
     * 
     * @param context the XWiki context used to render pages.
     * @throws IOException error when creating the package.
     * @throws XWikiException error when render the pages.
     */
    public void export(XWikiContext context) throws IOException, XWikiException
    {
        // ////////////////////////////////////////////
        // Create custom URL factory
        // ////////////////////////////////////////////

        ExportURLFactory urlf = new ExportURLFactory();
        File dir =
            (File) context.getEngineContext().getAttribute("javax.servlet.context.tempdir");
        File tempdir = new File(dir, RandomStringUtils.randomAlphanumeric(8));
        tempdir.mkdirs();
        File attachmentDir = new File(tempdir, "attachment");
        attachmentDir.mkdirs();

        // ////////////////////////////////////////////
        // Configure response
        // ////////////////////////////////////////////

        context.getResponse().setContentType("application/zip");
        context.getResponse().addHeader("Content-disposition",
            "attachment; filename=" + context.getWiki().getURLEncoded(name) + ".zip");
        context.setFinished(true);

        // ////////////////////////////////////////////
        // Render pages to export
        // ////////////////////////////////////////////

        ZipOutputStream zos = new ZipOutputStream(context.getResponse().getOutputStream());

        VelocityContext vcontext = (VelocityContext)context.get("vcontext");
        
        Document currentDocument = (Document)vcontext.get("cdoc");
        Document currentCDocument = (Document)vcontext.get("cdoc");
        Document currentTDocument = (Document)vcontext.get("tdoc");
        
        try {
            XWikiContext renderContext = (XWikiContext) context.clone();

            vcontext = XWikiVelocityRenderer.prepareContext(renderContext);

            urlf.init(this.pages, tempdir, renderContext);
            renderContext.setURLFactory(urlf);

            renderContext.put("action", "view");

            for (Iterator it = this.pages.iterator(); it.hasNext();) {
                String pageName = (String) it.next();

                XWikiDocument doc = renderContext.getWiki().getDocument(pageName, renderContext);

                String zipname = doc.getDatabase() + "." + doc.getSpace() + "." + doc.getName();
                String language = doc.getLanguage();
                if ((language != null) && (!language.equals(""))) {
                    zipname += "." + language;
                }

                zipname += ".html";

                ZipEntry zipentry = new ZipEntry(zipname);
                zos.putNextEntry(zipentry);

                renderContext.setDatabase(doc.getDatabase());
                renderContext.setDoc(doc);
                vcontext.put("doc", doc.newDocument(renderContext));
                vcontext.put("cdoc", vcontext.get("doc"));

                XWikiDocument tdoc = doc.getTranslatedDocument(renderContext);
                renderContext.put("tdoc", tdoc);
                vcontext.put("tdoc", tdoc.newDocument(renderContext));

                String content = renderContext.getWiki().parseTemplate("view.vm", renderContext);

                zos.write(content.getBytes(renderContext.getWiki().getEncoding()));
                zos.closeEntry();
            }
        } finally {
            // Clean velocity context
            vcontext = XWikiVelocityRenderer.prepareContext(context);
            
            vcontext.put("doc", currentDocument);
            vcontext.put("cdoc", currentCDocument);
            vcontext.put("tdoc", currentTDocument);
        }

        // ////////////////////////////////////////////
        // Add required skins to zip file
        // ////////////////////////////////////////////
        for (Iterator it = urlf.getNeededSkins().iterator(); it.hasNext();) {
            String skinName = (String) it.next();
            addSkinToZip(skinName, zos, context);
        }

        // ////////////////////////////////////////////
        // Add resources files to zip file
        // ////////////////////////////////////////////
        addDirToZip(tempdir, zos, "");

        zos.setComment(description);

        // Finish zip file
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
        addDirToZip(file, out, "skins/" + skinName + "/");
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
                addDirToZip(file, out, basePath + file.getName() + "/");
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
