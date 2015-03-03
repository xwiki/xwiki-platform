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
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.BeanInputFilterStream;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStream;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.LoggerManager;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.event.LoggerListener;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarPackage;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.DocumentInfoAPI;
import com.xpn.xwiki.plugin.packaging.Package;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.util.Util;

/**
 * XWiki Action responsible for importing XAR archives.
 *
 * @version $Id$
 */
public class ImportAction extends XWikiAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportAction.class);

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String result = null;

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
                result = importPackage(doc.getAttachment(name), request, context);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                "Exception while importing", e);
        }

        return result;
    }

    private void getPackageInfos(XWikiAttachment packFile, XWikiResponse response, XWikiContext xcontext)
        throws IOException, XWikiException, FilterException, XarException
    {
        String encoding = xcontext.getWiki().getEncoding();
        response.setContentType(MediaType.APPLICATION_XML.toString());
        response.setCharacterEncoding(encoding);

        if (xcontext.getWiki().ParamAsLong("xwiki.action.import.xar.usefilter", 1) == 0) {
            PackageAPI importer = ((PackageAPI) xcontext.getWiki().getPluginApi("package", xcontext));
            importer.Import(packFile.getContentInputStream(xcontext));
            String xml = importer.toXml();
            byte[] result = xml.getBytes(encoding);
            response.setContentLength(result.length);
            response.getOutputStream().write(result);
        } else {
            XarPackage xarPackage = new XarPackage(packFile.getContentInputStream(xcontext));

            xarPackage.write(response.getOutputStream(), encoding);
        }
    }

    private String importPackage(XWikiAttachment packFile, XWikiRequest request, XWikiContext context)
        throws IOException, XWikiException, FilterException
    {
        String all = request.get("all");
        if (!"1".equals(all)) {
            if (context.getWiki().ParamAsLong("xwiki.action.import.xar.usefilter", 1) == 0) {
                importPackageOld(packFile, request, context);
            } else {
                importPackageFilterStream(packFile, request, context);
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

    private String getLanguage(String pageName, XWikiRequest request)
    {
        return Util.normalizeLanguage(request.get("language_" + pageName));
    }

    private int getAction(String pageName, String language, XWikiRequest request)
    {
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

        return iAction;
    }

    private String getDocName(String pageName)
    {
        return pageName.replaceAll(":[^:]*$", "");
    }

    private void importPackageOld(XWikiAttachment packFile, XWikiRequest request, XWikiContext context)
        throws IOException, XWikiException
    {
        PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi("package", context));

        String[] pages = request.getParameterValues("pages");

        importer.Import(packFile.getContentInputStream(context));
        if (pages != null) {
            // Skip document by default
            List<DocumentInfoAPI> filelist = importer.getFiles();
            for (DocumentInfoAPI dia : filelist) {
                dia.setAction(DocumentInfo.ACTION_SKIP);
            }

            // Indicate with documents to import
            for (String pageName : pages) {
                String language = getLanguage(pageName, request);
                int iAction = getAction(pageName, language, request);

                String docName = getDocName(pageName);
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

    private void importPackageFilterStream(XWikiAttachment packFile, XWikiRequest request, XWikiContext context)
        throws IOException, XWikiException, FilterException
    {
        String[] pages = request.getParameterValues("pages");

        XARInputProperties xarProperties = new XARInputProperties();
        DocumentInstanceOutputProperties instanceProperties = new DocumentInstanceOutputProperties();
        instanceProperties.setSaveComment("Imported from XAR");

        if (pages != null) {
            EntityReferenceSet entities = new EntityReferenceSet();

            EntityReferenceResolver<String> resolver =
                Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "relative");

            for (String pageName : pages) {
                if (StringUtils.isNotEmpty(pageName)) {
                    String language = getLanguage(pageName, request);
                    int iAction = getAction(pageName, language, request);

                    String docName = getDocName(pageName);
                    if (iAction == DocumentInfo.ACTION_OVERWRITE) {
                        entities.includes(new LocalDocumentReference(resolver.resolve(docName, EntityType.DOCUMENT),
                            LocaleUtils.toLocale(language)));
                    }
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

        BeanInputFilterStreamFactory<XARInputProperties> xarFilterStreamFactory =
            Utils.getComponent((Type) InputFilterStreamFactory.class, FilterStreamType.XWIKI_XAR_11.serialize());
        BeanInputFilterStream<XARInputProperties> xarFilterStream =
            xarFilterStreamFactory.createInputFilterStream(xarProperties);

        BeanOutputFilterStreamFactory<InstanceOutputProperties> instanceFilterStreamFactory =
            Utils.getComponent((Type) OutputFilterStreamFactory.class, FilterStreamType.XWIKI_INSTANCE.serialize());
        BeanOutputFilterStream<InstanceOutputProperties> instanceFilterStream =
            instanceFilterStreamFactory.createOutputFilterStream(instanceProperties);

        // Notify all the listeners about import
        ObservationManager observation = Utils.getComponent(ObservationManager.class);

        InputStream source = packFile.getContentInputStream(context);
        xarProperties.setSource(new DefaultInputStreamInputSource(source));

        // Setup log
        xarProperties.setVerbose(true);
        instanceProperties.setVerbose(true);
        instanceProperties.setStoppedWhenSaveFail(false);
        LoggerManager loggerManager = Utils.getComponent(LoggerManager.class);
        LogQueue logger = new LogQueue();
        if (loggerManager != null) {
            // Isolate log
            loggerManager.pushLogListener(new LoggerListener(UUID.randomUUID().toString(), logger));
        }

        observation.notify(new XARImportingEvent(), null, context);

        try {
            xarFilterStream.read(instanceFilterStream.getFilter());

            xarFilterStream.close();
            instanceFilterStream.close();
        } finally {
            if (loggerManager != null) {
                // Stop isolating log
                loggerManager.popLogListener();
            }

            // Print the import log
            if (LOGGER.isDebugEnabled()) {
                logger.log(LOGGER);
            }

            // Close the input source
            source.close();

            observation.notify(new XARImportedEvent(), null, context);
        }

        // Generate import report
        // Emulate old packager report (for retro compatibility)
        Package oldImporter = new Package();
        if (logger.containLogsFrom(LogLevel.ERROR)) {
            context.put("install_status", DocumentInfo.INSTALL_ERROR);
        } else {
            context.put("install_status", DocumentInfo.INSTALL_OK);
        }
        EntityReferenceSerializer<String> serializer =
            Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        for (LogEvent log : logger) {
            Marker marker = log.getMarker();
            if (marker != null) {
                if (marker.contains(WikiDocumentFilter.LOG_DOCUMENT_CREATED.getName())
                    || marker.contains(WikiDocumentFilter.LOG_DOCUMENT_UPDATED.getName())) {
                    oldImporter.getInstalled(context).add(
                        serializer.serialize((EntityReference) log.getArgumentArray()[0]));
                } else if (marker.contains(WikiDocumentFilter.LOG_DOCUMENT_SKIPPED.getName())) {
                    oldImporter.getSkipped(context).add(
                        serializer.serialize((EntityReference) log.getArgumentArray()[0]));
                } else if (marker.contains(WikiDocumentFilter.LOG_DOCUMENT_ERROR.getName())) {
                    Object entity = log.getArgumentArray()[0];
                    if (entity != null) {
                        oldImporter.getErrors(context).add(
                            entity instanceof EntityReference ? serializer.serialize((EntityReference) log
                                .getArgumentArray()[0]) : entity.toString());
                    }
                }
            }
        }
    }
}
