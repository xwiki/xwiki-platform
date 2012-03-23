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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.DocumentInfoAPI;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.util.Util;

/**
 * XWiki Action responsible for importing XAR archives.
 * 
 * @version $Id$
 */
public class ImportAction extends XWikiAction
{
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
                String encoding = context.getWiki().getEncoding();
                response.setContentType("text/xml");
                response.setCharacterEncoding(encoding);
                XWikiAttachment packFile = doc.getAttachment(name);
                importer.Import(packFile.getContentInputStream(context));
                String xml = importer.toXml();
                byte[] result = xml.getBytes(encoding);
                response.setContentLength(result.length);
                response.getOutputStream().write(result);
                return null;
            } else if ("import".equals(action)) {
                // Do the actual import
                XWikiAttachment packFile = doc.getAttachment(name);
                importer.Import(packFile.getContentInputStream(context));
                String all = request.get("all");
                if (!"1".equals(all)) {
                    if (pages != null) {
                        List<DocumentInfoAPI> filelist = importer.getFiles();
                        for (DocumentInfoAPI dia : filelist) {
                            dia.setAction(DocumentInfo.ACTION_SKIP);
                        }

                        for (String pageName : pages) {
                            String language = Util.normalizeLanguage(request.get("language_" + pageName));
                            String actionName = "action_" + pageName;
                            if (!StringUtils.isBlank(language)) {
                                actionName += ("_" + language);
                            }
                            String defaultAction = request.get(actionName);
                            int iAction;
                            if (StringUtils.isBlank(defaultAction)) {
                                iAction = DocumentInfo.ACTION_OVERWRITE;
                            } else {
                                try {
                                    iAction = Integer.parseInt(defaultAction);
                                } catch (Exception e) {
                                    iAction = DocumentInfo.ACTION_SKIP;
                                }
                            }

                            String docName = pageName.replaceAll(":[^:]*$", "");
                            if (language == null) {
                                importer.setDocumentAction(docName, iAction);
                            } else {
                                importer.setDocumentAction(docName, language, iAction);
                            }
                        }
                    }
                    // Set the appropriate strategy to handle versions
                    if (StringUtils.equals(request.getParameter("historyStrategy"), "reset")) {
                        importer.setPreserveVersion(false);
                        importer.setWithVersions(false);
                    } else if (StringUtils.equals(request.getParameter("historyStrategy"), "replace")) {
                        importer.setPreserveVersion(false);
                        importer.setWithVersions(true);
                    } else {
                        importer.setPreserveVersion(true);
                        importer.setWithVersions(false);
                    }
                    // Set the backup pack option
                    if (StringUtils.equals(request.getParameter("importAsBackup"), "true")) {
                        importer.setBackupPack(true);
                    } else {
                        importer.setBackupPack(false);
                    }
                    // Import files
                    importer.install();
                    if (!StringUtils.isBlank(request.getParameter("ajax"))) {
                        // If the import is done from an AJAX request we don't want to return a whole HTML page,
                        // instead we return "inline" the list of imported documents,
                        // evaluating imported.vm template.
                        return "imported";
                    } else {
                        return "admin";
                    }
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                "Exception while importing", e);
        }
        return null;
    }
}
