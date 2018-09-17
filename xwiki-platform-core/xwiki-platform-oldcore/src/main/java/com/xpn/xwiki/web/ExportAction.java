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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.rendering.internal.wiki.XWikiWikiModel;
import org.xwiki.tree.Tree;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.export.html.HtmlPackager;
import com.xpn.xwiki.internal.export.OfficeExporter;
import com.xpn.xwiki.internal.export.OfficeExporterURLFactory;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.pdf.api.PdfExport.ExportType;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.pdf.impl.PdfURLFactory;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.util.Util;

import static com.xpn.xwiki.XWikiException.ERROR_XWIKI_UNKNOWN;
import static com.xpn.xwiki.XWikiException.MODULE_XWIKI_EXPORT;

/**
 * Exports in XAR, PDF, HTML and all output formats supported by *Office (when an *Office Server is running).
 *
 * @version $Id$
 */
public class ExportAction extends XWikiAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportAction.class);
    private static final String TREE_DOCUMENT_PREFIX = "document:";
    private static String PAGE_SEPARATOR = "&";

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

    private class ExportArguments {
        private String[] checkedPages;
        private String[] uncheckedPages;
        private boolean otherPages;
        private String name;
        private String description;

        ExportArguments(XWikiContext context)
        {
            XWikiRequest request = context.getRequest();

            this.description = request.get("description");

            this.name = request.get("name");
            if (StringUtils.isBlank(name)) {
                this.name = context.getDoc().getFullName();
            }

            String[] pagesParam = request.getParameterValues("pages");
            String checkedPagesParam = request.get("checked-pages");

            if (!StringUtils.isEmpty(checkedPagesParam)) {
                this.checkedPages = checkedPagesParam.split("&");
            } else {
                this.checkedPages = pagesParam;
            }

            String otherPagesRequestValue = request.get("other-pages");
            this.otherPages = Boolean.valueOf(otherPagesRequestValue);

            this.uncheckedPages = null;
            if (!StringUtils.isEmpty(request.get("unchecked-pages"))) {
                this.uncheckedPages = request.get("unchecked-pages").split(PAGE_SEPARATOR);
            }
        }
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

        ExportArguments exportArguments = new ExportArguments(context);

        Collection<DocumentReference> pageList =
            resolvePagesToExport(exportArguments, context);
        if (pageList.isEmpty()) {
            return null;
        }

        HtmlPackager packager = new HtmlPackager();

        if (exportArguments.name != null && exportArguments.name.trim().length() > 0) {
            packager.setName(exportArguments.name);
        }

        if (exportArguments.description != null) {
            packager.setDescription(exportArguments.description);
        }

        packager.addPageReferences(pageList);

        packager.export(context);

        return null;
    }

    private Collection<DocumentReference> resolvePages(String[] pages, XWikiContext context) throws XWikiException
    {
        List<DocumentReference> pageList = new ArrayList<>();
        if (pages == null || pages.length == 0) {
            pageList.add(context.getDoc().getDocumentReference());
        } else {
            Map<String, Object[]> wikiQueries = new HashMap<>();
            for (int i = 0; i < pages.length; ++i) {
                String pattern = null;
                try {
                    pattern = URLDecoder.decode(pages[i], context.getRequest().getCharacterEncoding());
                } catch (UnsupportedEncodingException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                        "Failed to resolve pages to export", e);
                }

                String wikiName;
                if (pattern.contains(":")) {
                    int index = pattern.indexOf(':');
                    wikiName = pattern.substring(0, index);
                    pattern = pattern.substring(index + 1);
                } else {
                    wikiName = context.getWikiId();
                }

                StringBuffer where;
                List<QueryParameter> params;

                if (!wikiQueries.containsKey(wikiName)) {
                    Object[] query = new Object[2];
                    query[0] = where = new StringBuffer("where ");
                    query[1] = params = new ArrayList<>();
                    wikiQueries.put(wikiName, query);
                } else {
                    Object[] query = wikiQueries.get(wikiName);
                    where = (StringBuffer) query[0];
                    params = (List<QueryParameter>) query[1];
                }

                if (i > 0) {
                    where.append(" or ");
                }

                where.append("doc.fullName like ?");
                params.add(new DefaultQueryParameter(null).like(pattern));
            }

            DocumentReferenceResolver<String> resolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");

            QueryManager queryManager = Utils.getComponent(QueryManager.class);

            String database = context.getWikiId();
            try {
                for (Map.Entry<String, Object[]> entry : wikiQueries.entrySet()) {
                    String wikiName = entry.getKey();
                    Object[] query = entry.getValue();
                    String where = query[0].toString();
                    List<Object> params = (List<Object>) query[1];

                    Query dbQuery = queryManager.createQuery(where, Query.HQL);
                    List<String> docsNames = dbQuery.setWiki(wikiName).bindValues(params).execute();

                    for (String docName : docsNames) {
                        String pageReference = wikiName + XWikiDocument.DB_SPACE_SEP + docName;
                        if (context.getWiki().getRightService().hasAccessLevel(
                            "view", context.getUser(), pageReference, context))
                        {
                            pageList.add(resolver.resolve(pageReference));
                        }
                    }
                }
            } catch (QueryException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                    "Failed to resolve pages to export", e);
            } finally {
                context.setWikiId(database);
            }
        }
        return pageList;
    }

    /**
     * Resolve the set of pages to export based on choices made in a paginated tree
     *
     * @param arguments the arguments regarding checked/unchecked pages and if "other pages" checkbox is checked.
     *  See {@link ExportArguments}.
     * @param context the context of the request
     * @return the collection of document reference to export
     */
    private Collection<DocumentReference> resolvePagesToExport(ExportArguments arguments, XWikiContext context)
        throws XWikiException
    {
        if (arguments.otherPages) {
            // if other pages is selected, we select all but the unchecked pages
            XWikiDocument doc = context.getDoc();

            // use the document tree to get all the pages
            Tree nestedPages = Utils.getComponent(Tree.class, "nestedPages");
            String nodeId = TREE_DOCUMENT_PREFIX + doc.getDocumentReference().toString();
            int childCount = nestedPages.getChildCount(nodeId);

            // TODO: Need to check how it works for the nested pages
            List<String> children = nestedPages.getChildren(nodeId, 0, childCount);

            // result as obtained as nodeId with "document:" as prefix
            List<String> formattedReferences = new ArrayList<>();
            int index = TREE_DOCUMENT_PREFIX.length();

            DocumentReferenceResolver<String> resolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");
            for (String child : children) {
                try {
                    formattedReferences.add(URLEncoder.encode(child.substring(index), context.getRequest().getCharacterEncoding()));
                } catch (UnsupportedEncodingException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                        "Failed to resolve pages to export", e);
                }
            }

            // we need to add the current document also
            formattedReferences.add(doc.getDocumentReference().toString());
            // get the document references
            Collection<DocumentReference> childrenReferences =
                this.resolvePages(formattedReferences.toArray(new String[0]), context);

            // if we have unchecked pages, we remove them from the list of references
            if (arguments.uncheckedPages != null && arguments.uncheckedPages.length > 0) {
                Collection<DocumentReference> uncheckedDocuments = this.resolvePages(arguments.uncheckedPages, context);
                childrenReferences.removeAll(uncheckedDocuments);
            }

            return childrenReferences;
        } else {
            // if "other pages" checkbox is not selected, we select only the checked pages
            return this.resolvePages(arguments.checkedPages, context);
        }
    }

    private String export(String format, XWikiContext context) throws XWikiException, IOException
    {
        // We currently use the PDF export infrastructure but we have to redesign the export code.
        XWikiURLFactory urlFactory = new OfficeExporterURLFactory();
        PdfExport exporter = new OfficeExporter();
        // Check if the office exporter supports the specified format.
        ExportType exportType = ((OfficeExporter) exporter).getExportType(format);
        // Note 1: exportType will be null if no office server is started or it doesn't support the passed format
        // Note 2: we don't use the office server for PDF exports since it doesn't work OOB. Instead we use FOP.
        if ("pdf".equalsIgnoreCase(format)) {
            // The export format is PDF or the office converter can't be used (either it doesn't support the specified
            // format or the office server is not started).
            urlFactory = new PdfURLFactory();
            exporter = new PdfExportImpl();
            exportType = ExportType.PDF;
        } else if (exportType == null) {
            context.put("message", "core.export.formatUnknown");
            return "exception";
        }

        urlFactory.init(context);
        context.setURLFactory(urlFactory);
        handleRevision(context);

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
        context.getResponse().addHeader(
            "Content-disposition",
            String.format("inline; filename=%s.%s", filename, exportType.getExtension()));
        exporter.export(doc, context.getResponse().getOutputStream(), exportType, context);

        return null;
    }

    private String exportXAR(XWikiContext context) throws XWikiException, IOException, FilterException
    {
        XWikiRequest request = context.getRequest();

        boolean history = Boolean.valueOf(request.get("history"));
        boolean backup = Boolean.valueOf(request.get("backup"));
        String author = request.get("author");
        String licence = request.get("licence");
        String version = request.get("version");

        ExportArguments exportArguments = new ExportArguments(context);

        boolean all = ArrayUtils.isEmpty(exportArguments.checkedPages);

        if (!context.getWiki().getRightService().hasWikiAdminRights(context)) {
            context.put("message", "needadminrights");
            return "exception";
        }

        if (StringUtils.isEmpty(exportArguments.name)) {
            if (all) {
                exportArguments.name = "backup";
            } else {
                exportArguments.name = "export";
            }
        }

        if (context.getWiki().ParamAsLong("xwiki.action.export.xar.usefilter", 1) == 1) {
            // Create input wiki stream
            DocumentInstanceInputProperties inputProperties = new DocumentInstanceInputProperties();

            // We don't want to log the details
            inputProperties.setVerbose(false);

            inputProperties.setWithJRCSRevisions(history);
            inputProperties.setWithRevisions(false);

            EntityReferenceSet entities = new EntityReferenceSet();

            if (all) {
                entities.includes(new WikiReference(context.getWikiId()));
            } else {
                // Find all page references and add them for processing
                Collection<DocumentReference> pageList = resolvePagesToExport(exportArguments, context);
                for (DocumentReference page : pageList) {
                    entities.includes(page);
                }
            }

            inputProperties.setEntities(entities);

            InputFilterStreamFactory inputFilterStreamFactory =
                Utils.getComponent(InputFilterStreamFactory.class, FilterStreamType.XWIKI_INSTANCE.serialize());

            InputFilterStream inputFilterStream = inputFilterStreamFactory.createInputFilterStream(inputProperties);

            // Create output wiki stream
            XAROutputProperties xarProperties = new XAROutputProperties();

            // We don't want to log the details
            xarProperties.setVerbose(false);

            XWikiResponse response = context.getResponse();

            xarProperties.setTarget(new DefaultOutputStreamOutputTarget(response.getOutputStream()));
            xarProperties.setPackageName(exportArguments.name);
            if (exportArguments.description != null) {
                xarProperties.setPackageDescription(exportArguments.description);
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

            BeanOutputFilterStreamFactory<XAROutputProperties> xarFilterStreamFactory =
                Utils.getComponent((Type) OutputFilterStreamFactory.class,
                    FilterStreamType.XWIKI_XAR_CURRENT.serialize());

            OutputFilterStream outputFilterStream = xarFilterStreamFactory.createOutputFilterStream(xarProperties);

            // Export
            response.setContentType("application/zip");
            response.addHeader("Content-disposition", "attachment; filename="
                + Util.encodeURI(exportArguments.name, context) + ".xar");

            inputFilterStream.read(outputFilterStream.getFilter());

            inputFilterStream.close();
            outputFilterStream.close();

            // Flush
            response.getOutputStream().flush();

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

            if (exportArguments.description != null) {
                export.setDescription(exportArguments.description);
            }

            if (licence != null) {
                export.setLicence(licence);
            }

            if (version != null) {
                export.setVersion(version);
            }

            export.setBackupPack(backup);

            export.setName(exportArguments.name);

            if (all) {
                export.backupWiki();
            } else {
                Collection<DocumentReference> pageList = resolvePagesToExport(exportArguments, context);
                for (DocumentReference pageReference : pageList) {
                    String defaultAction = request.get("action_" + pageReference.getName());
                    int iAction;
                    try {
                        iAction = Integer.parseInt(defaultAction);
                    } catch (Exception e) {
                        iAction = 0;
                    }
                    export.add(pageReference.getName(), iAction);
                }
                export.export();
            }
        }

        return null;
    }
}
