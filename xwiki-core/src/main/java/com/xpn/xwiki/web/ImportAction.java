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
 *
 */
package com.xpn.xwiki.web;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.DocumentInfoAPI;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

/**
 * XWiki Action responsible for importing XAR archives.
 * 
 * @version $Id$
 */
public class ImportAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        try {
            XWikiRequest request = context.getRequest();
            XWikiResponse response = context.getResponse();
            XWikiDocument doc = context.getDoc();
            String name = request.get("name");
            String action = request.get("action");
            String[] pages = request.getParameterValues("pages");

            if (!context.getWiki().getRightService().hasAdminRights(context)) {
                context.put("message", "needadminrights");
                return "exception";
            }

            if (name == null) {
                return "admin";
            }

            PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi("package", context));

            if ("getPackageInfos".equals(action)) {
                // List the documents present in the selected archive
                response.setContentType("text/xml");
                XWikiAttachment packFile = doc.getAttachment(name);
                importer.Import(packFile.getContent(context));
                String xml = importer.toXml();
                response.setContentLength(xml.getBytes().length);
                response.getWriter().write(xml);
                return null;
            } else if ("import".equals(action)) {
                // Do the actual import
                XWikiAttachment packFile = doc.getAttachment(name);
                importer.Import(packFile.getContent(context));
                String all = request.get("all");
                if (!"1".equals(all)) {
                    if (pages != null) {
                        List<DocumentInfoAPI> filelist = importer.getFiles();
                        for (DocumentInfoAPI dia : filelist) {
                            dia.setAction(DocumentInfo.ACTION_SKIP);
                        }

                        for (String pageName : pages) {
                            String language = request.get("language_" + pageName);
                            String actionName = "action_" + pageName;
                            if (language != null) {
                                actionName = "_" + language;
                            }
                            String defaultAction = request.get(actionName);
                            int iAction;
                            if ((defaultAction == null) || (defaultAction.equals(""))) {
                                iAction = DocumentInfo.ACTION_OVERWRITE;
                            } else {
                                try {
                                    iAction = Integer.parseInt(defaultAction);
                                } catch (Exception e) {
                                    iAction = DocumentInfo.ACTION_SKIP;
                                }
                            }

                            String docName = pageName.replaceAll(":.*$", "");
                            if (language == null) {
                                importer.setDocumentAction(docName, iAction);
                            } else {
                                importer.setDocumentAction(docName, language, iAction);
                            }
                        }
                    }
                    // Import files
                    String withVersions = request.get("withversions");
                    importer.setWithVersions("1".equals(withVersions));
                    importer.install();
                    return "admin";
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                "Exception while importing", e);
        }
        return null;
    }
}
