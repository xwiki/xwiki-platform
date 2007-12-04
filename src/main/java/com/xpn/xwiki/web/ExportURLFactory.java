package com.xpn.xwiki.web;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

public class ExportURLFactory extends XWikiServletURLFactory
{
    /**
     * Pages for which to convert URL to local.
     */
    protected Set exportedPages = new HashSet();

    /**
     * Directory where to export attachment.
     */
    protected File exportDir;

    /**
     * Names of skins needed by rendered page(s).
     */
    private Set neededSkins = new HashSet();

    public ExportURLFactory()
    {
    }

    public void init(Collection exportedPages, File exportDir, XWikiContext context)
    {
        super.init(context);

        if (exportDir != null) {
            this.exportDir = exportDir;
        }

        if (exportedPages != null) {
            XWikiDocument doc = new XWikiDocument();

            for (Iterator it = exportedPages.iterator(); it.hasNext();) {
                String pageName = (String) it.next();

                doc.setDatabase(null);
                doc.setSpace(null);
                doc.setName(null);

                doc.setFullName(pageName);

                String absolutePageName = "";

                if (doc.getDatabase() != null) {
                    absolutePageName += doc.getDatabase().toLowerCase();
                } else {
                    absolutePageName += context.getDatabase().toLowerCase();
                }

                absolutePageName += XWikiDocument.DB_SPACE_SEP;

                absolutePageName += doc.getFullName();

                this.exportedPages.add(absolutePageName);
            }
        }
    }

    public URL createSkinURL(String filename, String skin, XWikiContext context)
    {
        try {
            getNeededSkins().add(skin);

            StringBuffer newpath = new StringBuffer();

            newpath.append("file://");

            newpath.append("skins/");
            newpath.append(skin);

            addFileName(newpath, filename, false, context);

            return new URL(newpath.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.createSkinURL(filename, skin, context);
    }

    public URL createSkinURL(String filename, String web, String name, String xwikidb,
        XWikiContext context)
    {
        if (!"skins".equals(web)) {
            return createSkinURL(filename, web, name, xwikidb, context);
        }
        
        try {
            getNeededSkins().add(name);

            StringBuffer newpath = new StringBuffer();

            newpath.append("file://");

            newpath.append("skins/");
            newpath.append(name);

            addFileName(newpath, filename, false, context);

            return new URL(newpath.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.createSkinURL(filename, web, name, xwikidb, context);
    }

    public URL createURL(String web, String name, String action, String querystring,
        String anchor, String xwikidb, XWikiContext context)
    {
        try {
            if (this.exportedPages.contains((xwikidb == null ? context.getDatabase()
                .toLowerCase() : xwikidb.toLowerCase())
                + XWikiDocument.DB_SPACE_SEP + web + XWikiDocument.SPACE_NAME_SEP)
                && !"view".equals(action) && context.getLinksAction() == null) {
                StringBuffer newpath = new StringBuffer(servletPath);

                newpath.append("file://");

                newpath.append(xwikidb.toLowerCase());
                newpath.append(".");
                newpath.append(web);
                newpath.append(".");
                newpath.append(name);

                if ((anchor != null) && (!anchor.equals(""))) {
                    newpath.append("#");
                    newpath.append(anchor);
                }

                newpath.append(".html");

                return new URL(newpath.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.createURL(web, name, action, querystring, anchor, xwikidb, context);
    }

    public URL createAttachmentURL(String filename, String web, String name, String action,
        String querystring, String xwikidb, XWikiContext context)
    {
        try {
            String path =
                "attachment/" + context.getDatabase() + "." + web + "." + name + "." + filename;

            File tempdir = exportDir;
            File file = new File(tempdir, path);
            if (!file.exists()) {
                XWikiDocument doc =
                    context.getWiki().getDocument(web + XWikiDocument.SPACE_NAME_SEP + name,
                        context);
                XWikiAttachment attachment = doc.getAttachment(filename);
                byte[] data = attachment.getContent(context);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            }

            return new URI("file://" + path).toURL();
        } catch (Exception e) {
            e.printStackTrace();
            return super.createAttachmentURL(filename, web, name, action, null, xwikidb, context);
        }
    }

    public URL createAttachmentRevisionURL(String filename, String web, String name,
        String revision, String xwikidb, XWikiContext context)
    {
        try {
            String path =
                "attachment/" + context.getDatabase() + "." + web + "." + name + "." + filename;

            File tempdir = exportDir;
            File file = new File(tempdir, path);
            if (!file.exists()) {
                XWikiDocument doc =
                    context.getWiki().getDocument(web + XWikiDocument.SPACE_NAME_SEP + name,
                        context);
                XWikiAttachment attachment =
                    doc.getAttachment(filename).getAttachmentRevision(revision, context);
                byte[] data = attachment.getContent(context);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            }

            return new URI("file://" + path).toURL();
        } catch (Exception e) {
            e.printStackTrace();
            return super.createAttachmentRevisionURL(filename, web, name, revision, xwikidb,
                context);
        }
    }

    public String getURL(URL url, XWikiContext context)
    {
        if (url == null) {
            return "";
        }

        String path = Util.escapeURL(url.toString());

        if (url.getProtocol().equals("file")) {
            path = path.substring("file://".length());
        }

        return path;
    }

    public Collection getNeededSkins()
    {
        return neededSkins;
    }
}
