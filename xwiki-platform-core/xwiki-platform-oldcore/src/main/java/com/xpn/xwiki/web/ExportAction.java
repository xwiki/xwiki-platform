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
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.export.html.HtmlPackager;
import com.xpn.xwiki.internal.export.DocumentSelectionResolver;
import com.xpn.xwiki.internal.export.OfficeExporter;
import com.xpn.xwiki.internal.export.OfficeExporterURLFactory;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.pdf.api.PdfExport.ExportType;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.util.Util;

/**
 * Exports in XAR, PDF, HTML and all output formats supported by *Office (when an *Office Server is running).
 *
 * @version $Id$
 */
@Component
@Named("export")
@Singleton
public class ExportAction extends XWikiAction
{
    /**
     * Define the different format supported by the export.
     */
    private enum ExportFormat
    {
        XAR,
        HTML,
        OTHER
    }

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
                defaultPage = export(format, context);
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
     * @since 1.3M1
     */
    private String exportHTML(XWikiContext context) throws XWikiException, IOException
    {
        DocumentSelectionResolver documentSelectionResolver = getDocumentSelectionResover();
        // Fallback on the current document if there's no selection specified on the request.
        Collection<DocumentReference> pageList =
            documentSelectionResolver.isSelectionSpecified() ? documentSelectionResolver.getSelectedDocuments()
                : Collections.singleton(context.getDoc().getDocumentReference());

        // HTML export can be done by simple users so we need to check view right.
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        pageList = pageList.stream().filter(documentReference -> authorization.hasAccess(Right.VIEW, documentReference))
            .collect(Collectors.toSet());

        if (pageList.isEmpty()) {
            return null;
        }

        HtmlPackager packager = new HtmlPackager();

        String name = context.getRequest().getParameter("name");
        if (StringUtils.isBlank(name)) {
            name = context.getDoc().getFullName();
        }
        packager.setName(name);

        String description = context.getRequest().get("description");
        if (description != null) {
            packager.setDescription(description);
        }

        packager.addPageReferences(pageList);

        packager.export(context);

        return null;
    }

    private String export(String format, XWikiContext context) throws XWikiException, IOException
    {
        // Put the specified document revision in the context early so that it can be used even when the export format
        // is unknown (e.g. when performing client-side PDF export).
        handleRevision(context);

        // We currently use the PDF export infrastructure but we have to redesign the export code.
        XWikiURLFactory urlFactory = new OfficeExporterURLFactory();
        PdfExport exporter = new OfficeExporter();
        // Check if the office exporter supports the specified format.
        ExportType exportType = ((OfficeExporter) exporter).getExportType(format);
        // Note 1: exportType will be null if no office server is started or it doesn't support the passed format
        // Note 2: we don't use the office server for PDF exports since it doesn't work OOB. Instead we use FOP.
        if ("pdf".equalsIgnoreCase(format)) {
            urlFactory = new ExternalServletURLFactory();
            exporter = new PdfExportImpl();
            exportType = ExportType.PDF;
        } else if (exportType == null) {
            context.put("message", "core.export.formatUnknown");
            return "exception";
        }

        urlFactory.init(context);
        context.setURLFactory(urlFactory);

        XWikiDocument doc = context.getDoc();
        context.getResponse().setContentType(exportType.getMimeType());

        // Compute the name of the export. Since it's gong to be saved on the user's file system it needs to be a valid
        // File name. Thus we use the "path" serializer but replace the "/" separator by "_" since we're not computing
        // a directory hierarchy but a file name
        EntityReferenceSerializer<String> serializer =
            Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "path");
        String filename = serializer.serialize(doc.getDocumentReference()).replaceAll("/", "_");
        // Make sure we don't go over 255 chars since several filesystems don't support filename longer than that!
        filename = StringUtils.abbreviateMiddle(filename, "__", 255);
        context.getResponse().addHeader("Content-disposition",
            String.format("inline; filename=%s.%s", filename, exportType.getExtension()));
        exporter.export(doc, context.getResponse().getOutputStream(), exportType, context);

        return null;
    }

    private boolean getBooleanProperty(String requestProperty, String cfgProperty, boolean def, XWikiContext context)
    {
        boolean value;

        String valueString = context.getRequest().get(requestProperty);
        if (valueString == null) {
            if (def) {
                value = context.getWiki().ParamAsLong(cfgProperty, 1) != 0;
            } else {
                value = context.getWiki().ParamAsLong(cfgProperty, 0) == 1;
            }
        } else {
            value = Boolean.parseBoolean(valueString);
        }

        return value;
    }

    private String exportXAR(XWikiContext context) throws XWikiException, IOException, FilterException
    {
        if (!context.getWiki().getRightService().hasWikiAdminRights(context)) {
            context.put("message", "needadminrights");
            return "exception";
        }

        XWikiRequest request = context.getRequest();

        String name = request.get("name");
        String description = request.get("description");
        boolean history = Boolean.valueOf(request.get("history"));
        boolean attachmentJRCS =
            getBooleanProperty("attachment_jrcs", "xwiki.action.export.xar.attachment.jrcs", true, context);
        boolean optimized = getBooleanProperty("optimized", "xwiki.action.export.xar.optimized", true, context);
        boolean backup = Boolean.valueOf(request.get("backup"));
        String author = request.get("author");
        String licence = request.get("licence");
        String version = request.get("version");

        DocumentSelectionResolver documentSelectionResolver = getDocumentSelectionResover();
        boolean all = !documentSelectionResolver.isSelectionSpecified();

        if (StringUtils.isEmpty(name)) {
            name = all ? "backup" : "export";
        }

        if (context.getWiki().ParamAsLong("xwiki.action.export.xar.usefilter", 1) == 1) {
            // Create input wiki stream
            DocumentInstanceInputProperties inputProperties = new DocumentInstanceInputProperties();

            // We don't want to log the details
            inputProperties.setVerbose(false);

            inputProperties.setWithJRCSRevisions(history);
            inputProperties.setWithRevisions(false);
            // Retro compatibility: by default attachment history is serialized as JRCS but we disabled it if standard
            // attachment revisions is enabled
            if (history) {
                inputProperties.setWithWikiAttachmentsRevisions(!attachmentJRCS);
                inputProperties.setWithWikiAttachmentJRCSRevisions(attachmentJRCS);
            }

            EntityReferenceSet entities = new EntityReferenceSet();

            if (all) {
                entities.includes(new WikiReference(context.getWikiId()));
            } else {
                // We don't check access rights for each selected document because XAR export requires administration
                // right at the moment.
                documentSelectionResolver.getSelectedDocuments().forEach(entities::includes);
            }

            inputProperties.setEntities(entities);

            InputFilterStreamFactory inputFilterStreamFactory =
                Utils.getComponent(InputFilterStreamFactory.class, FilterStreamType.XWIKI_INSTANCE.serialize());

            try (InputFilterStream inputFilterStream
                 = inputFilterStreamFactory.createInputFilterStream(inputProperties))
            {
                // Create output wiki stream
                XAROutputProperties xarProperties = new XAROutputProperties();

                // We don't want to log the details
                xarProperties.setVerbose(false);
                if (optimized) {
                    xarProperties.setOptimized(optimized);
                }

                XWikiResponse response = context.getResponse();

                xarProperties.setTarget(new DefaultOutputStreamOutputTarget(response.getOutputStream()));
                xarProperties.setPackageName(name);
                if (description != null) {
                    xarProperties.setPackageDescription(description);
                }
                if (licence != null) {
                    xarProperties.setPackageLicense(licence);
                }
                if (author != null) {
                    xarProperties.setPackageAuthor(author);
                }
                if (version != null) {
                    xarProperties.setPackageVersion(version);
                }
                xarProperties.setPackageBackupPack(backup);
                xarProperties.setPreserveVersion(backup || history);

                BeanOutputFilterStreamFactory<XAROutputProperties> xarFilterStreamFactory = Utils
                    .getComponent((Type) OutputFilterStreamFactory.class,
                        FilterStreamType.XWIKI_XAR_CURRENT.serialize());

                try (OutputFilterStream outputFilterStream
                         = xarFilterStreamFactory.createOutputFilterStream(xarProperties))
                {
                    // Export
                    response.setContentType("application/zip");
                    response.addHeader("Content-disposition",
                        "attachment; filename=" + Util.encodeURI(name, context) + ".xar");

                    inputFilterStream.read(outputFilterStream.getFilter());
                }

                // Flush
                response.getOutputStream().flush();
            }

            // Indicate that we are done with the response so no need to add anything
            context.setFinished(true);
        } else {
            PackageAPI export = ((PackageAPI) context.getWiki().getPluginApi("package", context));
            if (export == null) {
                // No Packaging plugin configured
                return "exception";
            }

            export.setWithVersions(history);

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

            export.setBackupPack(backup);

            export.setName(name);

            if (all) {
                export.backupWiki();
            } else {
                // We don't check access rights for each selected document because XAR export requires administration
                // right at the moment.
                for (DocumentReference documentReference : documentSelectionResolver.getSelectedDocuments()) {
                    String defaultAction = request.get("action_" + documentReference.getName());
                    int iAction;
                    try {
                        iAction = Integer.parseInt(defaultAction);
                    } catch (Exception e) {
                        iAction = 0;
                    }
                    export.add(documentReference.getName(), iAction);
                }
                export.export();
            }
        }

        return null;
    }

    private DocumentSelectionResolver getDocumentSelectionResover()
    {
        return Utils.getComponent(DocumentSelectionResolver.class);
    }
}
