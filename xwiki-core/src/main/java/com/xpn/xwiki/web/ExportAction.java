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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.export.html.HtmlPackager;
import com.xpn.xwiki.pdf.api.PdfExport.ExportType;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.util.Util;

/**
 * Exports in XAR, PDF, RTF or HTML formats.
 * 
 * @version $Id$
 */
public class ExportAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String defaultPage;

        try {
            XWikiRequest request = context.getRequest();
            String format = request.get("format");

            if ((format == null) || (format.equals("xar"))) {
                defaultPage = exportXAR(context);
            } else if (format.equals("html")) {
                defaultPage = exportHTML(context);
            } else {
                defaultPage = exportPDFOrRTF(format, context);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                "Exception while exporting", e);
        }

        return defaultPage;
    }

    /**
     * Create ZIP archive containing wiki pages rendered in HTML, attached files and used skins.
     * 
     * @param context the XWiki context.
     * @return always return null.
     * @throws XWikiException error when exporting HTML ZIP package.
     * @throws IOException error when exporting HTML ZIP package.
     * @since XWiki Platform 1.3M1
     */
    private String exportHTML(XWikiContext context) throws XWikiException, IOException
    {
        XWikiRequest request = context.getRequest();

        String description = request.get("description");
        String name = request.get("name");
        String[] pages = request.getParameterValues("pages");

        List<String> pageList = new ArrayList<String>();
        if (pages == null || pages.length == 0) {
            pageList.add(context.getDoc().getFullName());

            if (StringUtils.isBlank(name)) {
                name = context.getDoc().getFullName();
            }
        } else {
            Map<String, Object[]> wikiQueries = new HashMap<String, Object[]>();
            for (int i = 0; i < pages.length; ++i) {
                String pattern = pages[i];

                String wikiName;
                if (pattern.contains(":")) {
                    int index = pattern.indexOf(':');
                    wikiName = pattern.substring(0, index);
                    pattern = pattern.substring(index + 1);
                } else {
                    wikiName = context.getDatabase();
                }

                StringBuffer where;
                List<String> params;

                if (!wikiQueries.containsKey(wikiName)) {
                    Object[] query = new Object[2];
                    query[0] = where = new StringBuffer("where ");
                    query[1] = params = new ArrayList<String>();
                    wikiQueries.put(wikiName, query);
                } else {
                    Object[] query = wikiQueries.get(wikiName);
                    where = (StringBuffer) query[0];
                    params = (List<String>) query[1];
                }

                if (i > 0) {
                    where.append(" or ");
                }

                where.append("doc.fullName like ?");
                params.add(pattern);
            }

            String database = context.getDatabase();
            try {
                for (Map.Entry<String, Object[]> entry : wikiQueries.entrySet()) {
                    String wikiName = entry.getKey();
                    Object[] query = entry.getValue();
                    String where = ((StringBuffer) query[0]).toString();
                    @SuppressWarnings("unchecked")
                    List<String> params = (List<String>) query[1];

                    context.setDatabase(wikiName);
                    List<String> docsNames = context.getWiki().getStore().searchDocumentsNames(where, params, context);
                    for (String docName : docsNames) {
                        pageList.add(wikiName + XWikiDocument.DB_SPACE_SEP + docName);
                    }
                }
            } finally {
                context.setDatabase(database);
            }
        }

        if (pageList.size() == 0) {
            return null;
        }

        HtmlPackager packager = new HtmlPackager();

        if (name != null && name.trim().length() > 0) {
            packager.setName(name);
        }

        if (description != null) {
            packager.setDescription(description);
        }

        packager.addPages(pageList);

        packager.export(context);

        return null;
    }

    private String exportPDFOrRTF(String format, XWikiContext context) throws XWikiException, IOException
    {
        XWikiURLFactory urlf =
            context.getWiki().getURLFactoryService().createURLFactory(XWikiContext.MODE_PDF, context);
        context.setURLFactory(urlf);
        PdfExportImpl pdfexport = new PdfExportImpl();
        XWikiDocument doc = context.getDoc();
        handleRevision(context);

        ExportType type = ExportType.PDF;
        if (format.equals("rtf")) {
            type = ExportType.RTF;
        }

        context.getResponse().setContentType(type.mimeType);
        context.getResponse().addHeader(
            "Content-disposition",
            "inline; filename=" + Util.encodeURI(doc.getDocumentReference().getLastSpaceReference().getName(), context)
            + "_" + Util.encodeURI(doc.getDocumentReference().getName(), context) + "." + type.extension);
        pdfexport.export(doc, context.getResponse().getOutputStream(), type, context);

        return null;
    }

    private String exportXAR(XWikiContext context) throws XWikiException, IOException
    {
        XWikiRequest request = context.getRequest();

        String history = request.get("history");
        String backup = request.get("backup");
        String author = request.get("author");
        String description = request.get("description");
        String licence = request.get("licence");
        String version = request.get("version");
        String name = request.get("name");
        String[] pages = request.getParameterValues("pages");
        boolean isBackup = ((pages == null) || (pages.length == 0));

        if (!context.getWiki().getRightService().hasAdminRights(context)) {
            context.put("message", "needadminrights");
            return "exception";
        }

        if (name == null) {
            return "export";
        }

        PackageAPI export = ((PackageAPI) context.getWiki().getPluginApi("package", context));
        if ("true".equals(history)) {
            export.setWithVersions(true);
        } else {
            export.setWithVersions(false);
        }

        if (author != null) {
            export.setAuthorName(author);
        }

        if (description != null) {
            export.setDescription(description);
        }

        if (licence != null) {
            export.setLicence(licence);
        }

        if (version != null) {
            export.setVersion(version);
        }

        if (name.trim().equals("")) {
            if (isBackup) {
                name = "backup";
            } else {
                name = "export";
            }
        }

        if ("true".equals(backup)) {
            export.setBackupPack(true);
        }

        export.setName(name);

        if (isBackup) {
            export.backupWiki();
        } else {
            if (pages != null) {
                for (int i = 0; i < pages.length; i++) {
                    String pageName = pages[i];
                    String defaultAction = request.get("action_" + pageName);
                    int iAction;
                    try {
                        iAction = Integer.parseInt(defaultAction);
                    } catch (Exception e) {
                        iAction = 0;
                    }
                    export.add(pageName, iAction);
                }
            }
            export.export();
        }

        return null;
    }
}
