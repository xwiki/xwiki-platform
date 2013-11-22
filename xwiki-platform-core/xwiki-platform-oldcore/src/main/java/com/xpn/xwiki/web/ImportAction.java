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
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MediaType;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.wikistream.instance.output.InstanceOutputProperties;
import org.xwiki.wikistream.internal.input.BeanInputWikiStream;
import org.xwiki.wikistream.internal.output.BeanOutputWikiStream;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.xar.input.XARInputProperties;
import org.xwiki.wikistream.xar.internal.XARPackage;

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

            if (!context.getWiki().getRightService().hasWikiAdminRights(context)) {
                context.put("message", "needadminrights");
                return "exception";
            }

            if (name == null) {
                return "admin";
            }

            if ("getPackageInfos".equals(action)) {
                getPackageInfos(doc.getAttachment(name), response, context);
            } else if ("import".equals(action)) {
                importPackage(doc.getAttachment(name), request, context);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                "Exception while importing", e);
        }
        return null;
    }

    private void getPackageInfos(XWikiAttachment packFile, XWikiResponse response, XWikiContext xcontext)
        throws IOException, XWikiException, WikiStreamException
    {
        String encoding = xcontext.getWiki().getEncoding();
        response.setContentType(MediaType.APPLICATION_XML.toString());
        response.setCharacterEncoding(encoding);

        if (xcontext.getWiki().ParamAsLong("xwiki.action.import.xar.usewikistream", 1) == 1) {
            XARPackage xarPackage = new XARPackage();
            XARInputProperties properties = new XARInputProperties();
            properties.setReferencesOnly(true);

            BeanInputWikiStreamFactory<XARInputProperties> inputWikiStreamFactory =
                Utils.getComponent((Type) InputWikiStreamFactory.class, WikiStreamType.XWIKI_XAR_11.serialize());
            inputWikiStreamFactory.createInputWikiStream(properties).read(xarPackage);

            xarPackage.write(response.getOutputStream());
        } else {
            PackageAPI importer = ((PackageAPI) xcontext.getWiki().getPluginApi("package", xcontext));
            importer.Import(packFile.getContentInputStream(xcontext));
            String xml = importer.toXml();
            byte[] result = xml.getBytes(encoding);
            response.setContentLength(result.length);
            response.getOutputStream().write(result);
        }
    }

    private String importPackage(XWikiAttachment packFile, XWikiRequest request, XWikiContext context)
        throws IOException, XWikiException, WikiStreamException
    {
        String[] pages = request.getParameterValues("pages");

        String all = request.get("all");
        if (!"1".equals(all)) {
            if (context.getWiki().ParamAsLong("xwiki.action.import.xar.usewikistream", 1) == 1) {
                XARInputProperties xarProperties = new XARInputProperties();
                DocumentInstanceOutputProperties instanceProperties = new DocumentInstanceOutputProperties();

                if (pages != null) {
                    EntityReferenceSet entities = new EntityReferenceSet();

                    EntityReferenceResolver<String> resolver =
                        Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "relative");

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
                        if (iAction == DocumentInfo.ACTION_SKIP) {
                            entities.excludes(new LocalDocumentReference(
                                resolver.resolve(docName, EntityType.DOCUMENT), LocaleUtils.toLocale(language)));
                        }
                    }

                    xarProperties.setEntities(entities);
                }

                // Set the appropriate strategy to handle versions
                if (StringUtils.equals(request.getParameter("historyStrategy"), "reset")) {
                    instanceProperties.setPreviousDeleted(true);
                    instanceProperties.setVersionPreserved(false);
                    xarProperties.setWithHistory(false);
                } else if (StringUtils.equals(request.getParameter("historyStrategy"), "replace")) {
                    instanceProperties.setPreviousDeleted(true);
                    instanceProperties.setVersionPreserved(true);
                    xarProperties.setWithHistory(true);
                } else {
                    instanceProperties.setPreviousDeleted(false);
                    instanceProperties.setVersionPreserved(false);
                    xarProperties.setWithHistory(false);
                }

                // Set the backup pack option
                if (StringUtils.equals(request.getParameter("importAsBackup"), "true")) {
                    instanceProperties.setAuthorPreserved(true);
                } else {
                    instanceProperties.setAuthorPreserved(false);
                }

                BeanInputWikiStreamFactory<XARInputProperties> xarWikiStreamFactory =
                    Utils.getComponent((Type) InputWikiStreamFactory.class, WikiStreamType.XWIKI_XAR_11.serialize());
                BeanInputWikiStream<XARInputProperties> xarWikiStream =
                    xarWikiStreamFactory.createInputWikiStream(xarProperties);

                BeanOutputWikiStreamFactory<InstanceOutputProperties> instanceWikiStreamFactory =
                    Utils.getComponent((Type) OutputWikiStreamFactory.class, WikiStreamType.XWIKI_INSTANCE.serialize());
                BeanOutputWikiStream<InstanceOutputProperties> instanceWikiStream =
                    instanceWikiStreamFactory.createOutputWikiStream(instanceProperties);

                xarWikiStream.read(instanceWikiStream);
            } else {
                PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi("package", context));
                importer.Import(packFile.getContentInputStream(context));
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
            }

            if (!StringUtils.isBlank(request.getParameter("ajax"))) {
                // If the import is done from an AJAX request we don't want to return a whole HTML page,
                // instead we return "inline" the list of imported documents,
                // evaluating imported.vm template.
                return "imported";
            } else {
                return "admin";
            }
        }

        return null;
    }
}
