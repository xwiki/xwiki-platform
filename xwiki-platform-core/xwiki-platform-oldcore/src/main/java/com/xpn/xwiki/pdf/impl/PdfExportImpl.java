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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fop.apps.EnvironmentProfile;
import org.apache.fop.apps.EnvironmentalProfileFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.PageSequenceResults;
import org.apache.velocity.VelocityContext;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;
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
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

import io.sf.carte.doc.dom4j.CSSStylableElement;
import io.sf.carte.doc.dom4j.XHTMLDocument;
import io.sf.carte.doc.dom4j.XHTMLDocumentFactory;
import io.sf.carte.doc.xml.dtd.DefaultEntityResolver;

/**
 * Default implementation for the PDF Export process, which uses XSLT transformations and Apache FOP to convert a
 * Document into PDF, passing through HTML, valid XHTML, styled XHTML, and XSL-FO.
 *
 * @version $Id$
 */
public class PdfExportImpl implements PdfExport
{
    /** The location where fonts to be used during PDF export should be placed. */
    private static final String FONTS_PATH = "/WEB-INF/fonts/";

    /** The name of the default XHTML2FOP transformation file. */
    private static final String DEFAULT_XHTML2FOP_XSLT = "xhtml2fo.xsl";

    /** The name of the default FOP post-processing transformation file. */
    private static final String DEFAULT_CLEANUP_XSLT = "fop.xsl";

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExportImpl.class);

    /** Document name resolver. */
    private static DocumentReferenceResolver<String> referenceResolver =
        Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /** Document name serializer. */
    private static EntityReferenceSerializer<String> referenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);

    /** Provides access to document properties. */
    private static DocumentAccessBridge dab = Utils.getComponent(DocumentAccessBridge.class);

    /** Velocity engine manager, used for interpreting velocity. */
    private static VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);

    /** XSLT transformer factory. */
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();

    /** The Apache FOP instance used for XSL-FO processing. */
    private static FopFactory fopFactory;

    /**
     * Used to get the temporary directory.
     */
    private Environment environment = Utils.getComponent((Type) Environment.class);

    // Fields initialization
    static {
        // ----------------------------------------------------------------------
        // FOP configuration
        // ----------------------------------------------------------------------

        EnvironmentProfile environmentProfile = EnvironmentalProfileFactory.createDefault(new File(".").toURI(),
            Utils.getComponent(PDFResourceResolver.class));
        FopFactoryBuilder builder = new FopFactoryBuilder(environmentProfile);

        // Load configuration
        Configuration configuration = null;
        try (InputStream fopConfigurationFile = PdfExportImpl.class.getResourceAsStream("/fop-config.xml")) {
            if (fopConfigurationFile != null) {
                configuration = new DefaultConfigurationBuilder().build(fopConfigurationFile);
            }
        } catch (Exception e) {
            LOGGER.warn("Wrong FOP configuration: " + ExceptionUtils.getRootCauseMessage(e));
        }

        if (configuration != null) {
            // Get a writable configuration instance
            if (!(configuration instanceof DefaultConfiguration)) {
                try {
                    configuration = new DefaultConfiguration(configuration, true);
                } catch (ConfigurationException e) {
                    // Should never happen
                    LOGGER.error("Failed to copy configuration", e);
                }
            }

            if (configuration instanceof DefaultConfiguration) {
                DefaultConfiguration writableConfiguration = (DefaultConfiguration) configuration;

                // Add XWiki fonts folder to the configuration
                try {
                    Environment environment = Utils.getComponent(Environment.class);
                    String fontsPath = environment.getResource(FONTS_PATH).getPath();
                    Execution execution = Utils.getComponent(Execution.class);
                    XWikiContext xcontext = (XWikiContext) execution.getContext().getProperty("xwikicontext");
                    if (xcontext != null) {
                        XWikiRequest request = xcontext.getRequest();
                        if (request != null && request.getSession() != null) {
                            fontsPath = request.getSession().getServletContext().getRealPath(FONTS_PATH);
                        }
                    }

                    // <renderers>
                    DefaultConfiguration renderersConfiguration =
                        (DefaultConfiguration) writableConfiguration.getChild("renderers", false);
                    if (renderersConfiguration == null) {
                        renderersConfiguration = new DefaultConfiguration("renderers");
                        writableConfiguration.addChild(renderersConfiguration);
                    }

                    // <renderer mime="application/pdf">
                    DefaultConfiguration pdfRenderer = null;
                    for (Configuration renderer : renderersConfiguration.getChildren()) {
                        if ("application/pdf".equals(renderer.getAttribute("mime"))) {
                            pdfRenderer = (DefaultConfiguration) renderer;
                        }
                    }
                    if (pdfRenderer == null) {
                        pdfRenderer = new DefaultConfiguration("renderer");
                        pdfRenderer.setAttribute("mime", "application/pdf");
                        renderersConfiguration.addChild(pdfRenderer);
                    }

                    // <fonts>
                    DefaultConfiguration fontsConfiguration =
                        (DefaultConfiguration) pdfRenderer.getChild("fonts", false);
                    if (fontsConfiguration == null) {
                        fontsConfiguration = new DefaultConfiguration("fonts");
                        pdfRenderer.addChild(fontsConfiguration);
                    }

                    // <directory>fontdirectory</directory>
                    DefaultConfiguration directoryConfiguration = new DefaultConfiguration("directory");
                    directoryConfiguration.setValue(fontsPath);
                    fontsConfiguration.addChild(directoryConfiguration);
                } catch (Throwable ex) {
                    LOGGER.warn("Starting with 1.5, XWiki uses the WEB-INF/fonts/ directory as the font directory, "
                        + "and it should contain the FreeFont (http://savannah.gnu.org/projects/freefont/) fonts. "
                        + "FOP cannot access this directory. If this is an upgrade from a previous version, "
                        + "make sure you also copy the WEB-INF/fonts directory from the new distribution package.");
                }
            }

            builder.setConfiguration(configuration);
        }

        fopFactory = builder.build();
    }

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
        File tempdir = new File(dir, RandomStringUtils.randomAlphanumeric(8));
        try {
            tempdir.mkdirs();
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
        List<HTMLFilter> filters = new ArrayList<HTMLFilter>(config.getFilters());
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
     * <li>A base transformation which converts the XHTML into a temporary XSL-FO; it uses the <tt>xhtml2fo.xsl</tt>
     * file, or the <tt>xhtmlxsl</tt> property of the applied PDFTemplate.</li>
     * <li>An eventual post-processing transformation which cleans up the temporary XSL-FO in order to avoid FOP bugs;
     * it uses the <tt>fop.xsl</tt> file, or the <tt>fopxsl</tt> property of the applied PDFTemplate.</li>
     * </ol>
     *
     * @param xhtml the XHTML document to convert
     * @param context the current request context
     * @return the resulting XML-FO document
     * @throws XWikiException if the conversion fails for any reason
     */
    private String convertXHtmlToXMLFO(String xhtml, XWikiContext context) throws XWikiException
    {
        String xmlfo = applyXSLT(xhtml, getXhtml2FopXslt(context));
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
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

            // Transform FOP fatal errors into warnings so that the PDF export isn't stopped
            foUserAgent.getEventBroadcaster().addEventListener(new XWikiFOPEventListener());

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(type.getMimeType(), foUserAgent, out);

            // Identity transformer
            Transformer transformer = transformerFactory.newTransformer();

            // Setup input stream
            Source source = new StreamSource(new StringReader(xmlfo));

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(source, res);

            // Result processing
            FormattingResults foResults = fop.getResults();
            if (foResults != null && LOGGER.isDebugEnabled()) {
                @SuppressWarnings("unchecked")
                java.util.List<PageSequenceResults> pageSequences = foResults.getPageSequences();
                for (PageSequenceResults pageSequenceResults : pageSequences) {
                    LOGGER.debug("PageSequence " + StringUtils.defaultIfEmpty(pageSequenceResults.getID(), "<no id>")
                        + " generated " + pageSequenceResults.getPageCount() + " pages.");
                }
                LOGGER.debug("Generated " + foResults.getPageCount() + " pages in total.");
            }
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
     * <li>the <tt>pdf.css</tt> skin file</li>
     * <li>and the <tt>style</tt> property of the applied PDFTemplate</li>
     * </ol>
     * The content found in these locations is concatenated. The CSS rules are applied on the document, and the
     * resulting style properties are embedded in the document, inside <tt>style</tt> attributes. The resulting XHTML
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
     * in <tt>style</tt> attributes.
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
            //System.setProperty("org.w3c.css.sac.parser", "org.apache.batik.css.parser.Parser");

            // Prepare the input
            Reader re = new StringReader(html);
            InputSource source = new InputSource(re);
            SAXReader reader = new SAXReader(XHTMLDocumentFactory.getInstance());
            reader.setEntityResolver(new DefaultEntityResolver());
            XHTMLDocument document = (XHTMLDocument) reader.read(source);

            // Set the base URL so that CSS4J can resolve URLs in CSS. Use the current document in the XWiki Context
            document.setBaseURL(new URL(context.getDoc().getExternalURL("view", context)));

            // Apply the style sheet
            document.addStyleSheet(new org.w3c.css.sac.InputSource(new StringReader(css)));
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
            // Debug output
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTML with CSS applied [{}]", result);
            }
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
        for (int i = 0; i < element.nodeCount(); i++) {
            org.dom4j.Node node = element.node(i);
            if (node instanceof CSSStylableElement) {
                CSSStylableElement styleElement = (CSSStylableElement) node;
                CSSStyleDeclaration style = styleElement.getComputedStyle();
                if (style != null && StringUtils.isNotEmpty(style.getCssText())) {
                    styleElement.addAttribute("style", styleElement.getComputedStyle().getCssText());
                }
            }
            if (node instanceof Element) {
                applyInlineStyle((Element) node);
            }
        }
    }

    /**
     * Get the XSLT for converting (valid) XHTML to XSL-FO. The content is searched in:
     * <ol>
     * <li>the <tt>xhtmlxsl</tt> property of the current PDFTemplate</li>
     * <li>the <tt>xhtml2fo.xsl</tt> resource (usually a file inside xwiki-core-*.jar)</li>
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
     * <li>the <tt>fopxsl</tt> property of the current PDFTemplate</li>
     * <li>the <tt>fop.xsl</tt> resource (usually a file inside xwiki-core-*.jar)</li>
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
            try {
                return IOUtils.toInputStream(xsl, context.getWiki().getEncoding());
            } catch (IOException e) {
                // This really shouldn't happen since it would mean that the encoding is either invalid or doesn't
                // exist in the JVM.
                LOGGER.error("Couldn't get XSLT for PDF exporting. Invalid or not existing encoding [{}]",
                    context.getWiki().getEncoding(), e);
            }
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
            templateReference = referenceResolver.resolve(pdftemplate);
            classReference = new DocumentReference(templateReference.getWikiReference().getName(), "XWiki", "PDFClass");
        } else {
            templateReference = dab.getCurrentDocumentReference();
            String currentWiki = dab.getCurrentDocumentReference().getRoot().getName();
            classReference = new DocumentReference(currentWiki, "XWiki", "PDFClass");
        }

        String result = (String) dab.getProperty(templateReference, classReference, propertyName);
        if (StringUtils.isBlank(result)) {
            return "";
        }
        String templateName = referenceSerializer.serialize(templateReference);
        try {
            StringWriter writer = new StringWriter();
            VelocityContext vcontext = velocityManager.getVelocityContext();
            velocityManager.getVelocityEngine().evaluate(vcontext, writer, templateName, result);
            result = writer.toString();
        } catch (XWikiVelocityException e) {
            LOGGER.warn("Error applying Velocity to the [{}] property of the [{}] document. Using the property's value "
                + "without applying Velocity.", propertyName, templateName, ExceptionUtils.getRootCauseMessage(e));
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
