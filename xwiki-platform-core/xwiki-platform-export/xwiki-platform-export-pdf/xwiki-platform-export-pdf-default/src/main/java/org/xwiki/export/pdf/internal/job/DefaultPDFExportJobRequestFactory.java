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
package org.xwiki.export.pdf.internal.job;

import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobRequestFactory;
import org.xwiki.model.internal.reference.comparator.DocumentReferenceComparator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.context.XWikiContextContextStore;
import com.xpn.xwiki.internal.export.DocumentSelectionResolver;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Default implementation of {@link PDFExportJobRequestFactory}.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Singleton
public class DefaultPDFExportJobRequestFactory implements PDFExportJobRequestFactory
{
    private static final String EXPORT = "export";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentSelectionResolver documentSelectionResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localStringEntityReferenceSerializer;

    @Inject
    private ContextStoreManager contextStoreManager;

    @Inject
    private PDFExportConfiguration configuration;

    private DocumentReferenceComparator documentReferenceComparator = new DocumentReferenceComparator(true);

    @Override
    public PDFExportJobRequest createRequest() throws Exception
    {
        String suffix = new Date().getTime() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        return createRequest(suffix);
    }

    protected PDFExportJobRequest createRequest(String suffix) throws Exception
    {
        PDFExportJobRequest request = new PDFExportJobRequest();
        request.setId(EXPORT, "pdf", suffix);
        request.setServerSide(this.configuration.isServerSide());

        setRightsProperties(request);
        setContextProperties(request);
        readPDFExportOptionsFromHTTPRequest(request);

        return request;
    }

    @Override
    public void setRightsProperties(PDFExportJobRequest request)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        request.setCheckRights(true);
        request.setUserReference(xcontext.getUserReference());
        request.setAuthorReference(xcontext.getAuthorReference());
    }

    private void setContextProperties(PDFExportJobRequest request) throws Exception
    {
        Map<String, Serializable> pdfExportContext =
            this.contextStoreManager.save(this.contextStoreManager.getSupportedEntries());

        URL printPreviewURL = getPrintPreviewURL(request.getId());
        pdfExportContext.put(XWikiContextContextStore.PROP_ACTION, EXPORT);
        pdfExportContext.put(XWikiContextContextStore.PROP_REQUEST_PARAMETERS,
            (Serializable) getRequestParameters(printPreviewURL.getQuery()));
        pdfExportContext.put(XWikiContextContextStore.PROP_REQUEST_URL, printPreviewURL);

        request.setContext(pdfExportContext);
    }

    private void readPDFExportOptionsFromHTTPRequest(PDFExportJobRequest request)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiRequest httpRequest = xcontext.getRequest();
        String template = httpRequest.get("pdftemplate");
        if (StringUtils.isEmpty(template)) {
            template = "XWiki.PDFExport.Template";
        }
        request.setTemplate(this.currentDocumentReferenceResolver.resolve(template));

        request.setWithCover(!"0".equals(httpRequest.get("pdfcover")));
        request.setWithToc(!"0".equals(httpRequest.get("pdftoc")));
        request.setWithHeader(!"0".equals(httpRequest.get("pdfheader")));
        request.setWithFooter(!"0".equals(httpRequest.get("pdffooter")));
        request.setDocuments(this.documentSelectionResolver.getSelectedDocuments(true).stream()
            .sorted(this.documentReferenceComparator).collect(Collectors.toList()));

        request.setFileName(xcontext.getDoc().getRenderedTitle(Syntax.PLAIN_1_0, xcontext) + ".pdf");
    }

    private Map<String, String[]> getRequestParameters(String queryString)
    {
        Map<String, List<String>> params = new LinkedHashMap<>();
        for (NameValuePair pair : URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8)) {
            List<String> values = params.getOrDefault(pair.getName(), new ArrayList<>());
            values.add(pair.getValue());
            params.put(pair.getName(), values);
        }

        Map<String, String[]> parameters = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            parameters.put(entry.getKey(), entry.getValue().toArray(new String[entry.getValue().size()]));
        }

        return parameters;
    }

    private URL getPrintPreviewURL(List<String> jobId)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiRequest httpRequest = xcontext.getRequest();

        String originalQueryString = Objects.toString(httpRequest.get("pdfQueryString"), "");
        String queryString = getPrintPreviewQueryString(jobId, originalQueryString);

        // The request URL hash (fragment identifier) is not sent to the server but in the case of server-side PDF
        // export we need it as it can influence the behavior of the JavaScript code when the PDF template is loaded in
        // the headless web browser. In order to overcome this we receive the hash as a request parameter.
        String hash = Objects.toString(httpRequest.get("pdfHash"), "");

        // We want the documents to be rendered with the same parameters (query string) and hash (anchor or fragment
        // identifier) as in print preview mode (what is used to generate the PDF in the end). For this the saved
        // request URL should use the specified query string and hash.
        DocumentReference documentReference = xcontext.getDoc().getDocumentReference();
        return xcontext.getURLFactory().createExternalURL(
            this.localStringEntityReferenceSerializer.serialize(documentReference.getLastSpaceReference()),
            documentReference.getName(), EXPORT, queryString, hash, documentReference.getWikiReference().getName(),
            xcontext);
    }

    private String getPrintPreviewQueryString(List<String> jobId, String originalQueryString)
    {
        List<NameValuePair> printPreviewParams = Arrays.asList(
            new BasicNameValuePair("format", "html-print"),
            new BasicNameValuePair("xpage", "get"),
            new BasicNameValuePair("outputSyntax", "plain"),
            // Asynchronous rendering is disabled by default on the export action so we need to force it.
            new BasicNameValuePair("async", "true"),
            new BasicNameValuePair("sheet", "XWiki.PDFExport.Sheet"),
            new BasicNameValuePair("jobId", StringUtils.join(jobId, '/'))
        );
        return URLEncodedUtils.format(printPreviewParams, StandardCharsets.UTF_8) + '&' + originalQueryString;
    }
}
