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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

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

/**
 * Exports in XAR, PDF, HTML and all output formats supported by *Office (when an *Office Server is running).
 *
 * @version $Id$
 */
public class ExportAction extends XWikiAction
{
    /**
     * Used to separate page arguments in excludes parameter
     */
    private static String PAGE_SEPARATOR = "&";

    /**
     * Define the different format supported by the export.
     */
    private enum ExportFormat
    {
        XAR,
        HTML,
        OTHER
    }

    /**
     * Manage the arguments of the export and provides them in an object to simplify their usage in the different
     * methods.
     *
     * @since 10.9
     */
    private class ExportArguments
    {
        /**
         * Represent the pages to be included and excluded: keys are pattern of pages to include, values are list of
         * pages to exclude
         */
        private Map<String, List<String>> exportPages;

        /**
         * Name of the export
         */
        private String name;

        /**
         * Description of the export
         */
        private String description;

        ExportArguments(XWikiContext context, ExportFormat format) throws XWikiException
        {
            XWikiRequest request = context.getRequest();

            this.description = request.get("description");

            this.name = request.get("name");

            String[] pages = request.getParameterValues("pages");
            String[] excludes = request.getParameterValues("excludes");

            if (StringUtils.isBlank(name) && !format.equals(ExportFormat.XAR)) {
                this.name = context.getDoc().getFullName();
            }

            this.exportPages = new LinkedHashMap<>();

            if (pages != null) {
                for (int i = 0; i < pages.length; i++) {
                    List<String> excludedPages;

                    if (excludes != null && i < excludes.length) {
                        excludedPages = this.decodePages(excludes[i], context);
                    } else {
                        excludedPages = Collections.emptyList();
                    }

                    this.exportPages.put(pages[i], excludedPages);
                }
            }
        }

        /**
         * Decode an URIEncoded String and split it based on the {@link #PAGE_SEPARATOR}. Returns a list of decoded
         * string.
         *
         * @param encodedString the string to decode.
         * @param context the context used to retrieved the right character encoding.
         * @return a list of decoded string.
         * @throws XWikiException when there is a problem with the decoding process.
         */
        private List<String> decodePages(String encodedString, XWikiContext context) throws XWikiException
        {
            List<String> listOfPages = new ArrayList<>();
            for (String page : encodedString.split(PAGE_SEPARATOR)) {
                try {
                    String decoded = URLDecoder.decode(page, context.getRequest().getCharacterEncoding());
                    if (!decoded.isEmpty()) {
                        listOfPages.add(decoded);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                        "Failed to resolve pages to export", e);
                }
            }

            return listOfPages;
        }
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
     * @since XWiki Platform 1.3M1
     */
    private String exportHTML(XWikiContext context) throws XWikiException, IOException
    {
        ExportArguments exportArguments = new ExportArguments(context, ExportFormat.HTML);

        Collection<DocumentReference> pageList = resolvePages(exportArguments, context);
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

    /**
     * Extract the name of the wiki from a given String representing a document. Returns the name contained in the
     * documentName or the wiki name of the current context.
     *
     * @param documentName a complete name of a document, which might contain the name of the wiki.
     * @param context in case the documentName does not contain a wiki name, returns the context wiki name.
     * @return the name of the documentName or by default the context wiki name.
     */
    private String extractWikiName(String documentName, XWikiContext context)
    {
        String wikiName;
        int index = documentName.indexOf(':');
        if (index > 0) {
            wikiName = documentName.substring(0, index);
        } else {
            wikiName = context.getWikiId();
        }
        return wikiName;
    }

    /**
     * Resolve the pages in the given context and return their references. This method uses the list of includedPages
     * and excludedPages given in the exportArguments to build a proper query and resolve the pages after checking the
     * user rights.
     *
     * @param arguments the arguments of the export to know the list of included/excluded pages to resolve.
     * @param context the context to use.
     * @return a collection of DocumentReference corresponding to the given criteria and which are viewable by the user.
     */
    private Collection<DocumentReference> resolvePages(ExportArguments arguments, XWikiContext context)
        throws XWikiException
    {
        List<DocumentReference> pageList = new ArrayList<>();

        // if there's no includedPages, the default is to return the current document
        if (arguments.exportPages.isEmpty()) {
            pageList.add(context.getDoc().getDocumentReference());

            // else we process the list of included/excluded pages
        } else {
            Map<String, Object[]> wikiQueries = new HashMap<>();

            for (Map.Entry<String, List<String>> export : arguments.exportPages.entrySet()) {
                String includePage = export.getKey();
                List<String> excludedPages = export.getValue();

                String wikiName = this.extractWikiName(includePage, context);

                // we only want the name of the document without its wikiName
                if (includePage.startsWith(wikiName + ":")) {
                    includePage = includePage.substring(wikiName.length() + 1);
                }

                StringBuffer where;
                List<QueryParameter> params;

                // we didn't already made a query for this wiki
                // so we create it
                if (!wikiQueries.containsKey(wikiName)) {
                    Object[] query = new Object[2];
                    query[0] = where = new StringBuffer("where ( ");
                    query[1] = params = new ArrayList<>();
                    wikiQueries.put(wikiName, query);

                    // we get back the query we started to continue it
                } else {
                    Object[] query = wikiQueries.get(wikiName);
                    where = (StringBuffer) query[0];
                    params = (List<QueryParameter>) query[1];

                    where.append("or ( ");
                }

                where.append("doc.fullName like ?");
                params.add(new DefaultQueryParameter(null).like(includePage));

                // if they exist we process the excludedPages associated with that include
                if (!excludedPages.isEmpty()) {
                    for (int j = 0; j < excludedPages.size(); j++) {
                        String excludePage = excludedPages.get(j);

                        // we check that the excludedPages are in the same wiki
                        String localwikiName = this.extractWikiName(excludePage, context);

                        if (!localwikiName.equals(wikiName)) {
                            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                                XWikiException.ERROR_XWIKI_APP_EXPORT,
                                String.format("The excludes argument [%s] makes reference to another wiki than its "
                                    + "attached pages argument [%s]", excludePage, includePage));
                        }

                        if (excludePage.startsWith(wikiName + ":")) {
                            excludePage = excludePage.substring(wikiName.length() + 1);
                        }

                        where.append(" and doc.fullName not like ?");
                        params.add(new DefaultQueryParameter(null).like(excludePage));
                    }
                }

                // don't forget to close the query statement
                where.append(" ) ");
            }

            DocumentReferenceResolver<String> resolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");

            QueryManager queryManager = Utils.getComponent(QueryManager.class);
            AuthorizationManager authorizationManager = Utils.getComponent(AuthorizationManager.class);

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
                        WikiReference wikiReference = new WikiReference(wikiName);
                        DocumentReference pageReference = resolver.resolve(docName, wikiReference);

                        if (authorizationManager.hasAccess(Right.VIEW, context.getUserReference(), pageReference)) {
                            pageList.add(pageReference);
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

        ExportArguments exportArguments = new ExportArguments(context, ExportFormat.XAR);

        boolean all = exportArguments.exportPages.isEmpty();

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
                Collection<DocumentReference> pageList = resolvePages(exportArguments, context);
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
                Collection<DocumentReference> pageList = resolvePages(exportArguments, context);
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
