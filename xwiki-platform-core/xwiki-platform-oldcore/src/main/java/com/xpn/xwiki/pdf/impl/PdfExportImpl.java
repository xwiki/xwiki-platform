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
package com.xpn.xwiki.pdf.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.sax.SAXSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.environment.Environment;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.xml.EntityResolver;
import org.xwiki.xml.XMLReaderFactory;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLUtils;
import org.xwiki.xml.html.filter.HTMLFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.pdf.XSLFORenderer;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.web.Utils;

import io.sf.carte.doc.dom4j.CSSStylableElement;
import io.sf.carte.doc.dom4j.XHTMLDocument;
import io.sf.carte.doc.dom4j.XHTMLDocumentFactory;
import io.sf.carte.doc.style.css.CSSStyleDeclaration;
import io.sf.carte.doc.xml.dtd.DefaultEntityResolver;

/**
 * Default implementation for the PDF Export process, which uses XSLT transformations and Apache FOP to convert a
 * Document into PDF, passing through HTML, valid XHTML, styled XHTML, and XSL-FO.
 *
 * @version $Id$
 */
public class PdfExportImpl implements PdfExport
{
    /** The name of the default XHTML2FOP transformation file. */
    private static final String DEFAULT_XHTML2FOP_XSLT = "xhtml2fo.xsl";

    /** The name of the default FOP post-processing transformation file. */
    private static final String DEFAULT_CLEANUP_XSLT = "fop.xsl";

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExportImpl.class);

    /** Document name resolver. */
    private final DocumentReferenceResolver<String> referenceResolver =
        Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /** Document name serializer. */
    private final EntityReferenceSerializer<String> referenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);

    /** Provides access to document properties. */
    private final DocumentAccessBridge dab = Utils.getComponent(DocumentAccessBridge.class);

    /** Velocity engine manager, used for interpreting velocity. */
    private final VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);

    private final XMLReaderFactory xmlReaderFactory = Utils.getComponent(XMLReaderFactory.class);

    private final DocumentAuthorizationManager authorizationManager =
        Utils.getComponent(DocumentAuthorizationManager.class);

    private final AuthorExecutor authorExecutor = Utils.getComponent(AuthorExecutor.class);

    private final UserReferenceSerializer<DocumentReference> userReferenceSerializer =
        Utils.getComponent(UserReferenceSerializer.TYPE_DOCUMENT_REFERENCE, "document");

    /**
     * Used to get the temporary directory.
     */
    private final Environment environment = Utils.getComponent((Type) Environment.class);

    /**
     * Used to render XSL-FO to PDF.
     */
    private final XSLFORenderer xslFORenderer = Utils.getComponent(XSLFORenderer.class, "fop");

    @Override
    public void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException
    {
        export(doc, out, ExportType.PDF, context);
    }

    @Override
    public void export(XWikiDocument doc, OutputStream out, ExportType type, XWikiContext context) throws XWikiException
    {
        // Note: The passed document is not used currently since we're calling pdf.vm and that
        // velocity template uses the XWiki Context to get the current doc or its translations.
        // This could be improved by setting a specific context using the passed document but we
        // would also need to get the translations and set them too.

        File dir = this.environment.getTemporaryDirectory();
        File tempdir = new File(dir, RandomStringUtils.secure().nextAlphanumeric(8));
        try {
            FileUtils.forceMkdir(tempdir);
        } catch (IOException e) {
            throw new XWikiException(String.format("Failed to create PDF export temporary directory [%s]",
                tempdir), e);
        }

        try {
            context.put("pdfexportdir", tempdir);
            context.put("pdfexport-file-mapping", new HashMap<String, File>());
            boolean useLocalPlaceholders = !Utils.arePlaceholdersEnabled(context);
            if (useLocalPlaceholders) {
                Utils.enablePlaceholders(context);
            }
            String content = context.getWiki().parseTemplate("pdf.vm", context).trim();
            if (useLocalPlaceholders) {
                content = Utils.replacePlaceholders(content, context);
                Utils.disablePlaceholders(context);
            }
            exportHtml(content, out, type, context);
        } finally {
            try {
                FileUtils.deleteDirectory(tempdir);
            } catch (IOException ex) {
                // Should not happen, but it's nothing serious, just that temporary files are left on the disk.
                LOGGER.warn("Failed to cleanup temporary files after a PDF export", ex);
            }
        }
    }

    @Override
    public void exportHtml(String html, OutputStream out, ExportType type, XWikiContext context) throws XWikiException
    {
        exportXHTML(applyCSS(convertToStrictXHtml(html), context), out, type, context);
    }

    /**
     * Cleans up an HTML document, turning it into valid XHTML.
     *
     * @param input the source HTML to process
     * @return the cleaned up source
     */
    private String convertToStrictXHtml(String input)
    {
        LOGGER.debug("Cleaning HTML:\n{}", input);

        HTMLCleaner cleaner = Utils.getComponent(HTMLCleaner.class);
        HTMLCleanerConfiguration config = cleaner.getDefaultConfiguration();
        List<HTMLFilter> filters = new ArrayList<>(config.getFilters());
        filters.add(Utils.getComponent(HTMLFilter.class, "uniqueId"));
        config.setFilters(filters);
        String result = HTMLUtils.toString(cleaner.clean(new StringReader(input), config));
        LOGGER.debug("Cleaned XHTML:\n{}", result);
        return result;
    }

    /**
     * Convert a valid XHTML document into PDF. No further processing of the XHTML occurs.
     * <p>
     * Note: This method is protected just allow other exporters to hook their code and use the PDF export
     * infrastructure. This is just a temporary solution. The PDF export code needs to be redesigned because it has
     * parts than can be reused for other export formats.
     *
     * @param xhtml the source document to transform
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     */
    protected void exportXHTML(String xhtml, OutputStream out, ExportType type, XWikiContext context)
        throws XWikiException
    {
        LOGGER.debug("Final XHTML for export:\n{}", xhtml);

        // XSL Transformation to XML-FO
        String xmlfo = convertXHtmlToXMLFO(xhtml, context);

        // Debug output
        LOGGER.debug("Final XSL-FO source:\n{}", xmlfo);

        renderXSLFO(xmlfo, out, type, context);
    }

    /**
     * Convert a valid XHTML document into an XSL-FO document through XSLT transformations. Two transformations are
     * involved:
     * <ol>
     * <li>A base transformation which converts the XHTML into a temporary XSL-FO; it uses the {@code xhtml2fo.xsl}
     * file, or the {@code xhtmlxsl} property of the applied PDFTemplate.</li>
     * <li>An eventual post-processing transformation which cleans up the temporary XSL-FO in order to avoid FOP bugs;
     * it uses the {@code fop.xsl} file, or the {@code fopxsl} property of the applied PDFTemplate.</li>
     * </ol>
     *
     * @param xhtml the XHTML document to convert
     * @param context the current request context
     * @return the resulting XML-FO document
     * @throws XWikiException if the conversion fails for any reason
     */
    private String convertXHtmlToXMLFO(String xhtml, XWikiContext context) throws XWikiException
    {
        String xmlfo = null;
        try (InputStream stream = getXhtml2FopXslt(context)) {
            xmlfo = applyXSLT(xhtml, stream);
        } catch (IOException e) {
            LOGGER.error("Failed to close the XSLT stream", e);
        }

        LOGGER.debug("Intermediary XSL-FO:\n{}", xmlfo);

        return applyXSLT(xmlfo, getFopCleanupXslt(context));
    }

    /**
     * Convert an XSL-FO document into PDF.
     *
     * @param xmlfo the source FO to render
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the XWiki Context used by the custom URI Resolver we use to locate image attachment data
     * @throws XWikiException if the conversion fails for any reason
     */
    private void renderXSLFO(String xmlfo, OutputStream out, ExportType type, final XWikiContext context)
        throws XWikiException
    {
        try {
            this.xslFORenderer.render(new ByteArrayInputStream(xmlfo.getBytes(StandardCharsets.UTF_8)), out, type.getMimeType());
        } catch (IllegalStateException e) {
            throw createException(e, type, XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION);
        } catch (Exception e) {
            throw createException(e, type, XWikiException.ERROR_XWIKI_EXPORT_PDF_FOP_FAILED);
        }
    }

    /**
     * Applies an XSLT transformation to an XML document.
     *
     * @param xml the XML document to convert
     * @param xslt the XSLT to apply
     * @return the converted document
     * @throws XWikiException if the transformation fails for any reason
     */
    protected String applyXSLT(String xml, InputStream xslt) throws XWikiException
    {
        try {
            XMLReader xmlReader = Utils.getComponent(XMLReaderFactory.class).createXMLReader();
            xmlReader.setEntityResolver(Utils.getComponent(EntityResolver.class));
            SAXSource xmlSource = new SAXSource(xmlReader, new InputSource(new StringReader(xml)));
            SAXSource xsltSource = new SAXSource(xmlReader, new InputSource(xslt));
            return XMLUtils.transform(xmlSource, xsltSource);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT, XWikiException.ERROR_XWIKI_EXPORT_XSL_FAILED,
                "XSL Transformation Failed", e);
        }
    }

    /**
     * Apply CSS styling to an XHTML document. The style to apply is taken from:
     * <ol>
     * <li>the {@code pdf.css} skin file</li>
     * <li>and the {@code style} property of the applied PDFTemplate</li>
     * </ol>
     * The content found in these locations is concatenated. The CSS rules are applied on the document, and the
     * resulting style properties are embedded in the document, inside {@code style} attributes. The resulting XHTML
     * document with the inlined style is then serialized and returned.
     *
     * @param html the valid XHTML document to style
     * @param context the current request context
     * @return the document with inlined style
     */
    private String applyCSS(String html, XWikiContext context)
    {
        String css =
            (context == null || context.getWiki() == null) ? "" : context.getWiki().parseTemplate("pdf.css", context);
        String style = getPDFTemplateProperty("style", context);
        if (style != null) {
            css += style;
        }
        // Don't apply CSS if there's no CSS to apply!
        return StringUtils.isBlank(css) ? html : applyCSS(html, css, context);
    }

    /**
     * Apply a CSS style sheet to an XHTML document and return the document with the resulting style properties inlined
     * in {@code style} attributes.
     *
     * @param html the valid XHTML document to style
     * @param css the style sheet to apply
     * @param context the current request context
     * @return the document with inlined style
     */
    String applyCSS(String html, String css, XWikiContext context)
    {
        LOGGER.debug("Applying the following CSS [{}] to HTML [{}]", css, html);
        try {
            // Prepare the input
            Reader re = new StringReader(html);
            InputSource source = new InputSource(re);
            XHTMLDocumentFactory docFactory = XHTMLDocumentFactory.getInstance();
            SAXReader reader = new SAXReader(docFactory);

            // Dom4J 2.1.1 disables external DTDs by default, so we set our own XMLReader.
            // See https://github.com/dom4j/dom4j/issues/51
            XMLReader xmlReader = this.xmlReaderFactory.createXMLReader();
            reader.setXMLReader(xmlReader);

            reader.setEntityResolver(new DefaultEntityResolver());
            XHTMLDocument document = (XHTMLDocument) reader.read(source);

            // Set the base URL so that CSS4J can resolve URLs in CSS. Use the current document in the XWiki Context
            document.setBaseURL(new URL(context.getDoc().getExternalURL("view", context)));

            // Apply the style sheet.
            document.addStyleSheet(new io.sf.carte.doc.style.css.nsac.InputSource(new StringReader(css)));
            applyInlineStyle(document.getRootElement());

            OutputFormat outputFormat = new OutputFormat("", false);
            if ((context == null) || (context.getWiki() == null)) {
                outputFormat.setEncoding("UTF-8");
            } else {
                outputFormat.setEncoding(context.getWiki().getEncoding());
            }
            StringWriter out = new StringWriter();
            XMLWriter writer = new XMLWriter(out, outputFormat);
            writer.write(document);
            String result = out.toString();
            LOGGER.debug("HTML with CSS applied [{}]", result);
            return result;
        } catch (Exception e) {
            LOGGER.warn("Failed to apply CSS [{}] to HTML [{}]", css, html, e);
            return html;
        }
    }

    /**
     * Recursively inline the computed style that applies to a DOM Element into the {@code style} attribute of that
     * Element.
     *
     * @param element the Element whose style should be inlined
     */
    private void applyInlineStyle(Element element)
    {
        int nodeCount = element.nodeCount();
        for (int i = 0; i < nodeCount; i++) {
            org.dom4j.Node node = element.node(i);
            if (node.getNodeType() == org.dom4j.Node.ELEMENT_NODE) {
                // Element node are always also CSSStylableElement elements
                CSSStylableElement styleElement = (CSSStylableElement) node;
                CSSStyleDeclaration style = styleElement.getComputedStyle();
                if (style.getLength() != 0) {
                    styleElement.addAttribute("style", style.getCssText());
                }
                applyInlineStyle(styleElement);
            }
        }
    }

    /**
     * Get the XSLT for converting (valid) XHTML to XSL-FO. The content is searched in:
     * <ol>
     * <li>the {@code xhtmlxsl} property of the current PDFTemplate</li>
     * <li>the {@code xhtml2fo.xsl} resource (usually a file inside xwiki-core-*.jar)</li>
     * </ol>
     *
     * @param context the current request context
     * @return the content of the XSLT as a byte stream
     */
    private InputStream getXhtml2FopXslt(XWikiContext context)
    {
        return getXslt("xhtmlxsl", DEFAULT_XHTML2FOP_XSLT, context);
    }

    /**
     * Get the XSLT for post-processing the XSL-FO file. The content is searched in:
     * <ol>
     * <li>the {@code fopxsl} property of the current PDFTemplate</li>
     * <li>the {@code fop.xsl} resource (usually a file inside xwiki-core-*.jar)</li>
     * </ol>
     *
     * @param context the current request context
     * @return the content of the XSLT as a byte stream
     */
    private InputStream getFopCleanupXslt(XWikiContext context)
    {
        return getXslt("fopxsl", DEFAULT_CLEANUP_XSLT, context);
    }

    /**
     * Get an XSLT file.
     *
     * @param propertyName the name of the xproperty from which to read the XSLT file.
     *                     See {@link #getPDFTemplateProperty(String, XWikiContext)} for details on how this property
     *                     is resolved. If the property doesn't point to any XSLT file then the fallback file parameter
     *                     is used instead
     * @param fallbackFile the name of a resource file to use when no XSLT content was found using the passed
     *                     {@code propertyName}
     * @param context the current request context
     * @return the content of the XSLT as a byte stream
     */
    protected InputStream getXslt(String propertyName, String fallbackFile, XWikiContext context)
    {
        String xsl = getPDFTemplateProperty(propertyName, context);
        if (!StringUtils.isBlank(xsl)) {
            return IOUtils.toInputStream(xsl, context.getWiki().getEncoding());
        }
        return getClass().getClassLoader().getResourceAsStream(fallbackFile);
    }

    /**
     * Extract XSLT file content using the following algorithm:
     * <ul>
     *   <li>Check if a query string named {@code pdftemplate} exists and if so use its value as the reference to
     *       a document containing a XWiki.PDFClass xobject from which to extract the XSLT data. If not defined
     *       (or if empty) then use the current document as the document having the XWiki.PDFClass xobject.</li>
     *   <li>Read the value of the xproperty named after the passed {@code propertyName} parameter. If the document
     *       or the property don't exist, then return an empty String. Otherwise execute Velocity on the xproperty's
     *       value and return this.</li>
     * </ul>
     *
     * @param propertyName the xproperty containing the XSLT to return
     * @param context the current request context
     * @return the content of the xproperty, velocity-parsed, or an empty string if there's no such property
     */
    private String getPDFTemplateProperty(String propertyName, XWikiContext context)
    {
        String pdftemplate = context.getRequest().getParameter("pdftemplate");

        DocumentReference templateReference;
        DocumentReference classReference;
        if (StringUtils.isNotEmpty(pdftemplate)) {
            templateReference = this.referenceResolver.resolve(pdftemplate);
            classReference = new DocumentReference(templateReference.getWikiReference().getName(), "XWiki", "PDFClass");
        } else {
            templateReference = this.dab.getCurrentDocumentReference();
            String currentWiki = this.dab.getCurrentDocumentReference().getRoot().getName();
            classReference = new DocumentReference(currentWiki, "XWiki", "PDFClass");
        }

        String templateContent = (String) this.dab.getProperty(templateReference, classReference, propertyName);
        if (StringUtils.isBlank(templateContent)) {
            return "";
        }

        String templateName = this.referenceSerializer.serialize(templateReference);
        DocumentReference templateAuthorReference;
        String result = templateContent;
        try {
             templateAuthorReference = this.userReferenceSerializer.serialize(
                 this.dab.getDocumentInstance(templateReference).getAuthors().getEffectiveMetadataAuthor());
        } catch (Exception e) {
            LOGGER.warn("Error fetching the author of template [{}] during PDF conversion. Using the [{}] property of "
                + "the document's value without applying Velocity.", templateName, propertyName);
            return result;
        }

        if (this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, templateAuthorReference,
            templateReference))
        {
            try {
                result = this.authorExecutor.call(() -> {
                    StringWriter writer = new StringWriter();
                    VelocityContext vcontext = this.velocityManager.getVelocityContext();
                    this.velocityManager.getVelocityEngine().evaluate(vcontext, writer, templateName,
                        templateContent);
                    return writer.toString();
                }, templateAuthorReference, templateReference);
            } catch (Exception e) {
                LOGGER.warn("Failed to run Velocity engine in author executor. Using the [{}] property of the [{}] "
                    + "document's value without applying Velocity. Reason: [{}]",
                    propertyName, templateName, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return result;
    }

    /**
     * Create an XWikiException object with the given source, export type and error type.
     *
     * @param source the source exception that is forwarded
     * @param exportType the type of the export performed while the exception occurred, PDF or RTF
     * @param errorType the type of error that occurred, one of the constants in {@link XWikiException}
     * @return a new XWikiException object
     */
    private XWikiException createException(Throwable source, ExportType exportType, int errorType)
    {
        return new XWikiException(XWikiException.MODULE_XWIKI_EXPORT, errorType,
            "Exception while exporting " + exportType.getExtension(), source);
    }
}
