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
package com.xpn.xwiki.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

/**
 * Handle URL generation in rendered wiki pages. This implementation makes sure URL will be local URL for exported
 * content (like skin, attachment and pages).
 * 
 * @version $Id$
 */
public class ExportURLFactory extends XWikiServletURLFactory
{
    /**
     * Logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(ExportURLFactory.class);

    /** The encoding to use when reading text resources from the filesystem and when sending css/javascript responses. */
    private static final String ENCODING = "UTF-8";

    private static final SkinAction SKINACTION = new SkinAction();

    // TODO: use real css parser
    private static Pattern CSSIMPORT = Pattern.compile("^\\s*@import\\s*\"(.*)\"\\s*;$", Pattern.MULTILINE);

    /**
     * Pages for which to convert URL to local.
     */
    protected Set<String> exportedPages = new HashSet<String>();

    /**
     * Directory where to export attachment.
     */
    protected File exportDir;

    /**
     * Names of skins needed by rendered page(s).
     */
    private Set<String> neededSkins = new HashSet<String>();

    Set<String> exporteSkinFiles = new HashSet<String>();

    /**
     * ExportURLFactory constructor.
     */
    public ExportURLFactory()
    {
    }

    /**
     * @return the list skins names used.
     */
    public Collection<String> getNeededSkins()
    {
        return this.neededSkins;
    }

    /**
     * @return the list of custom skin files.
     */
    public Collection<String> getExportedSkinFiles()
    {
        return this.exporteSkinFiles;
    }

    /**
     * Init the url factory.
     * 
     * @param exportedPages the pages that will be exported.
     * @param exportDir the directory where to copy exported objects (attachments).
     * @param context the XWiki context.
     */
    public void init(Collection<String> exportedPages, File exportDir, XWikiContext context)
    {
        super.init(context);

        if (exportDir != null) {
            this.exportDir = exportDir;
        }

        if (exportedPages != null) {
            XWikiDocument doc = new XWikiDocument();

            for (String pageName : exportedPages) {
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

    @Override
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
            LOGGER.error("Failed to create skin URL", e);
        }

        return super.createSkinURL(filename, skin, context);
    }

    @Override
    public URL createSkinURL(String fileName, String web, String name, String wikiId, XWikiContext context)
    {
        URL skinURL = super.createSkinURL(fileName, web, name, wikiId, context);

        if (!"skins".equals(web)) {
            return skinURL;
        }

        try {
            getNeededSkins().add(name);

            StringBuffer filePathBuffer = new StringBuffer();
            filePathBuffer.append("skins/");
            filePathBuffer.append(name);
            addFileName(filePathBuffer, fileName, false, context);

            String filePath = filePathBuffer.toString();

            if (!this.exporteSkinFiles.contains(filePath)) {
                this.exporteSkinFiles.add(filePath);

                File file = new File(this.exportDir, filePath);

                // Make sure the folder exists
                File folder = file.getParentFile();
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(file);
                String database = context.getDatabase();

                try {
                    XWikiServletResponseStub response = new XWikiServletResponseStub();
                    response.setOutpuStream(fos);
                    context.setResponse(response);
                    context.setDatabase(wikiId);

                    SKINACTION.render(skinURL.getPath(), context);
                } finally {
                    fos.close();
                    context.setDatabase(database);
                }

                followCssImports(file, web, name, wikiId, context);
            }

            StringBuffer newpath = new StringBuffer();
            newpath.append("file://");
            newpath.append(filePath);

            skinURL = new URL(newpath.toString());
        } catch (Exception e) {
            LOGGER.error("Failed to create skin URL", e);
        }

        return skinURL;
    }

    /**
     * Resolve CSS <code>@import</code> targets.
     */
    private void followCssImports(File file, String web, String name, String wikiId, XWikiContext context)
        throws IOException
    {
        // TODO: find better way to know it's css file (not sure it's possible, we could also try to find @import
        // whatever the content)
        if (file.getName().endsWith(".css")) {
            FileInputStream fis = new FileInputStream(file);

            try {
                String content = IOUtils.toString(fis, ENCODING);

                // TODO: use real css parser
                Matcher matcher = CSSIMPORT.matcher(content);

                while (matcher.find()) {
                    String fileName = matcher.group(1);

                    createSkinURL(fileName, web, name, wikiId, context);
                }
            } finally {
                fis.close();
            }
        }
    }

    @Override
    public URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context)
    {
        try {
            StringBuffer newpath = new StringBuffer();

            newpath.append("file://");

            newpath.append("resources");

            addFileName(newpath, filename, false, context);

            return new URL(newpath.toString());
        } catch (Exception e) {
            LOGGER.error("Failed to create skin URL", e);
        }

        return super.createResourceURL(filename, forceSkinAction, context);
    }

    @Override
    public URL createURL(String web, String name, String action, String querystring, String anchor, String xwikidb,
        XWikiContext context)
    {
        String wikiname = xwikidb == null ? context.getDatabase().toLowerCase() : xwikidb.toLowerCase();

        try {
            if (this.exportedPages.contains(wikiname + XWikiDocument.DB_SPACE_SEP + web + XWikiDocument.SPACE_NAME_SEP
                + name)
                && "view".equals(action) && context.getLinksAction() == null) {
                StringBuffer newpath = new StringBuffer();

                newpath.append("file://");

                newpath.append(wikiname);
                newpath.append(".");
                newpath.append(web);
                newpath.append(".");
                newpath.append(name);

                newpath.append(".html");

                if (!StringUtils.isEmpty(anchor)) {
                    newpath.append("#");
                    newpath.append(anchor);
                }

                return new URL(newpath.toString());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create page URL", e);
        }

        return super.createURL(web, name, action, querystring, anchor, xwikidb, context);
    }

    /**
     * Generate an url targeting attachment in provided wiki page.
     * 
     * @param filename the name of the attachment.
     * @param space the space of the page containing the attachment.
     * @param name the name of the page containing the attachment.
     * @param xwikidb the wiki of the page containing the attachment.
     * @param context the XWiki context.
     * @return the generated url.
     * @throws XWikiException error when retrieving document attachment.
     * @throws IOException error when retrieving document attachment.
     * @throws URISyntaxException when retrieving document attachment.
     */
    private URL createAttachmentURL(String filename, String space, String name, String xwikidb, XWikiContext context)
        throws XWikiException, IOException, URISyntaxException
    {
        String db = (xwikidb == null ? context.getDatabase() : xwikidb);
        String path = "attachment/" + db + "." + space + "." + name + "." + filename;

        File file = new File(this.exportDir, path);
        if (!file.exists()) {
            XWikiDocument doc =
                context.getWiki().getDocument(
                    db + XWikiDocument.DB_SPACE_SEP + space + XWikiDocument.SPACE_NAME_SEP + name, context);
            XWikiAttachment attachment = doc.getAttachment(filename);
            FileOutputStream fos = new FileOutputStream(file);
            IOUtils.copy(attachment.getContentInputStream(context), fos);
            fos.close();
        }

        return new URI("file://" + path.replace(" ", "%20")).toURL();
    }

    @Override
    public URL createAttachmentURL(String filename, String web, String name, String action, String querystring,
        String xwikidb, XWikiContext context)
    {
        try {
            return createAttachmentURL(filename, web, name, xwikidb, context);
        } catch (Exception e) {
            LOGGER.error("Failed to create attachment URL", e);

            return super.createAttachmentURL(filename, web, name, action, null, xwikidb, context);
        }
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String web, String name, String revision, String xwikidb,
        XWikiContext context)
    {
        try {
            return createAttachmentURL(filename, web, name, xwikidb, context);
        } catch (Exception e) {
            LOGGER.error("Failed to create attachment URL", e);

            return super.createAttachmentRevisionURL(filename, web, name, revision, xwikidb, context);
        }
    }

    @Override
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
}
