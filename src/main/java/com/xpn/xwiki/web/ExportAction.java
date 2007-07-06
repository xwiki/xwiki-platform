/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

import java.io.IOException;

/**
 * Exports in XAR, PDF or RTF formats
 */
public class ExportAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     * @see XWikiAction#render(XWikiContext)
     */
    public String render(XWikiContext context) throws XWikiException
    {
        String defaultPage;

        try {
            XWikiRequest request = context.getRequest();
            String format = request.get("format");

            if ((format == null) || (format.equals("xar"))) {
                defaultPage = exportXAR(context);
            } else {
                defaultPage = exportPDFOrRTF(format, context);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_EXPORT,
                "Exception while exporting", e);
        }

        return defaultPage;
    }

    private String exportPDFOrRTF(String format, XWikiContext context)
        throws XWikiException, IOException
    {
        XWikiURLFactory urlf = context.getWiki().getURLFactoryService()
            .createURLFactory(XWikiContext.MODE_PDF, context);
        context.setURLFactory(urlf);
        PdfExportImpl pdfexport = new PdfExportImpl();
        XWikiDocument doc = context.getDoc();
        handleRevision(context);

        int type = PdfExportImpl.PDF;
        if (format.equals("rtf")) {
            type = PdfExportImpl.RTF;
        } else {
            format = "pdf";
        }

        context.getResponse().setContentType("application/" + format);
        context.getResponse().addHeader("Content-disposition", "inline; filename=" +
            Utils.encode(doc.getSpace(), context) + "_" +
            Utils.encode(doc.getName(), context) + "." + format);
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

        PackageAPI export =
            ((PackageAPI) context.getWiki().getPluginApi("package", context));
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
