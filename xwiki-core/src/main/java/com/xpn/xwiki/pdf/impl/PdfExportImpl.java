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
package com.xpn.xwiki.pdf.impl;

import info.informatica.doc.dom4j.CSSStylableElement;
import info.informatica.doc.dom4j.DOM4JCSSStyleSheet;
import info.informatica.doc.dom4j.XHTMLDocument;
import info.informatica.doc.dom4j.XHTMLDocumentFactory;
import info.informatica.doc.xml.dtd.DefaultEntityResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.PageSequenceResults;
import org.apache.velocity.VelocityContext;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.tidy.Tidy;
import org.xml.sax.InputSource;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager.ManagerState;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.xml.EntityResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Default implementation for the PDF Export process, which uses XSLT transformations and Apache FOP to convert a
 * Document into PDF, passing through HTML, valid XHTML, styled XHTML, and XSL-FO.
 * 
 * @version $Id$
 */
public class PdfExportImpl implements PdfExport
{
    /** Export type: PDF. */
    public static final int PDF = 0;

    /** Export type: RTF. */
    public static final int RTF = 1;

    /** The name of the default XHTML2FOP transformation file. */
    private static final String DEFAULT_XHTML2FOP_XSLT = "xhtml2fo.xsl";

    /** The name of the default FOP post-processing transformation file. */
    private static final String DEFAULT_CLEANUP_XSLT = "fop.xsl";

    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(PdfExportImpl.class);

    /** Tidy configuration. */
    private static final Properties TIDY_CONFIGURATION;

    /** Velocity engine manager, used for interpreting velocity. */
    private static VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);

    /** The OpenOffice manager used for generating RTF. */
    private static OpenOfficeManager oooManager = Utils.getComponent(OpenOfficeManager.class);

    /** DOM parser factory. */
    private static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    /** DOM Serializer factory. */
    private static DOMImplementationLS lsImpl;

    /** The Apache FOP instance used for XSL-FO processing. */
    private static FopFactory fopFactory;

    /** The JTidy instance used for cleaning up HTML documents. */
    private Tidy tidy;

    // Fields initialization
    static {
        // ----------------------------------------------------------------------
        // XML parser/serializer initialization
        // ----------------------------------------------------------------------
        dbFactory.setNamespaceAware(true);
        dbFactory.setValidating(false);

        try {
            lsImpl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        } catch (Exception ex) {
            LOG.warn("Cannot initialize the DomLS implementation needed by the PDF export: " + ex.getMessage());
        }

        // ----------------------------------------------------------------------
        // FOP configuration
        // ----------------------------------------------------------------------
        fopFactory = FopFactory.newInstance();
        try {
            fopFactory.getFontManager().setFontBaseURL(
                (Utils.getComponent(Container.class)).getApplicationContext().getResource("/WEB-INF/fonts/").getPath());
        } catch (Throwable ex) {
            LOG.warn("Starting with 1.5, XWiki uses the WEB-INF/fonts/ directory as the font directory, "
                + "and it should contain the FreeFont (http://savannah.gnu.org/projects/freefont/) fonts. "
                + "FOP cannot access this directory. If this is an upgrade from a previous version, "
                + "make sure you also copy the WEB-INF/fonts directory from the new distribution package.");
        }
        if (PdfExportImpl.class.getResource("/fop-config.xml") != null) {
            DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            Configuration cfg;
            try {
                cfg = cfgBuilder.build(PdfExportImpl.class.getResourceAsStream("/fop-config.xml"));
                fopFactory.setUserConfig(cfg);
            } catch (Exception ex) {
                LOG.warn("Wrong FOP configuration: " + ex.getMessage());
            }
        }

        // ----------------------------------------------------------------------
        // Tidy configuration
        // ----------------------------------------------------------------------
        // Setup a default configuration for Tidy
        Properties baseConfiguration = new Properties();
        baseConfiguration.setProperty("quiet", Boolean.TRUE.toString());
        baseConfiguration.setProperty("clean", Boolean.TRUE.toString());
        baseConfiguration.setProperty("tidy-mark", Boolean.FALSE.toString());
        baseConfiguration.setProperty("output-xhtml", Boolean.TRUE.toString());
        baseConfiguration.setProperty("show-warnings", Boolean.FALSE.toString());
        baseConfiguration.setProperty("trim-empty-elements", Boolean.FALSE.toString());
        baseConfiguration.setProperty("numeric-entities", Boolean.TRUE.toString());
        // Don't replace non-ASCII apostrophes/quotes/dashes with plain ASCII ones.
        baseConfiguration.setProperty("ascii-chars", Boolean.FALSE.toString());
        // Don't wrap, as it is not needed, and it triggers JTidy bugs when combined with non-ASCII chars.
        baseConfiguration.setProperty("wrap", "0");

        // Allow Tidy to be configured in the tidy.properties file
        TIDY_CONFIGURATION = new Properties(baseConfiguration);
        try {
            TIDY_CONFIGURATION.load(PdfExportImpl.class.getClassLoader().getResourceAsStream("/tidy.properties"));
        } catch (IOException ex) {
            LOG.warn("Tidy configuration file could not be read. Using default configuration.");
        } catch (NullPointerException ex) {
            LOG.warn("Tidy configuration file doesn't exist. Using default configuration.");
        }
    }

    /** Default constructor. */
    public PdfExportImpl()
    {
        this.tidy = new Tidy();
        this.tidy.setConfigurationFromProps(TIDY_CONFIGURATION);
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
    public String getXhtmlxsl(XWikiContext context) throws XWikiException
    {
        String xsl = getPDFTemplateField("xhtmlxsl", context);
        if (StringUtils.isBlank(xsl)) {
            return DEFAULT_XHTML2FOP_XSLT;
        } else {
            return xsl;
        }
    }

    /**
     * Convert a valid XHTML document into PDF. No further processing of the XHTML occurs.
     * 
     * @param xhtml the source document to transform
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     */
    public void exportXHtml(String xhtml, OutputStream out, int type, XWikiContext context) throws XWikiException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Final XHTML for export: " + xhtml);
        }

        // If OpenOffice server is connected use it instead of FOP which does not support RTF very well
        // Only switch to openoffice server for RTF because FOP is supposedly a lot more powerful for PDF
        if (type != PDF && oooManager.getState() == ManagerState.CONNECTED) {
            exportOffice(xhtml, out, type, context);
        } else {
            // XSL Transformation to XML-FO
            String xmlfo = convertXHtmlToXMLFO(xhtml, context);

            // Debug output
            if (LOG.isDebugEnabled()) {
                LOG.debug("XSL-FO source: " + xmlfo);
            }

            exportXMLFO(xmlfo, out, type);
        }
    }

    /**
     * Export a valid XHTML document into PDF using the OpenOffice converter.
     * 
     * @param xhtml the source document to transform
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     */
    public void exportOffice(String xhtml, OutputStream out, int type, XWikiContext context) throws XWikiException
    {
        String html = xhtml;

        // FIXME: put that in some XSL transformation instead (no time and knowledge to do that before 2.4)
        // html = applyXsl(xhtml, getXhtmlOfficexsl(context));
        html = html.replaceAll("(<div[^>]+class=\"pdftoc\"[^>]*>)", "<p style=\"page-break-before: always;\" />\n$1");
        html = html.replaceAll("(<div[^>]+id=\"xwikimaincontainer\"[^>]*>)", "<p style=\"page-break-before: always;\" />\n$1");

        // OpenOffice does not support XHTML so we remove the XML marker and let it parse it as if it was HTML content
        html = html.substring(xhtml.indexOf("?>") + 2);

        // id attribute on body element makes openoffice converter to fail
        html = html.replaceFirst("(<body[^>]+)id=\"body\"([^>]*>)", "$1$2");

        OpenOfficeConverter documentConverter = oooManager.getConverter();

        String inputFileName = "export_input.html";
        String outputFileName = "export_output" + (type == PdfExportImpl.RTF ? ".rtf" : ".pdf");

        Map<String, InputStream> inputStreams =
            Collections.<String, InputStream> singletonMap(inputFileName, new ByteArrayInputStream(html.getBytes()));

        try {
            Map<String, byte[]> ouput = documentConverter.convert(inputStreams, inputFileName, outputFileName);

            out.write(ouput.values().iterator().next());
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while exporting "
                    + (type == PdfExportImpl.RTF ? "RTF" : "PDF"), e);
        }
    }

    /**
     * Convert an XSL-FO document into PDF.
     * 
     * @param xmlfo the source FO to render
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @throws XWikiException if the conversion fails for any reason
     */
    public void exportXMLFO(String xmlfo, OutputStream out, int type) throws XWikiException
    {
        // XSL Transformation to XML-FO

        try {
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Construct fop with desired output format
            Fop fop =
                fopFactory.newFop(type == PdfExportImpl.RTF ? MimeConstants.MIME_RTF : MimeConstants.MIME_PDF,
                    foUserAgent, out);

            // Setup JAXP using identity transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer

            // Setup input stream
            Source source = new StreamSource(new StringReader(xmlfo));

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(source, res);

            // Result processing
            FormattingResults foResults = fop.getResults();
            if (foResults != null && LOG.isDebugEnabled()) {
                java.util.List<PageSequenceResults> pageSequences = foResults.getPageSequences();
                for (PageSequenceResults pageSequenceResults : pageSequences) {
                    LOG.debug("PageSequence " + StringUtils.defaultIfEmpty(pageSequenceResults.getID(), "<no id>")
                        + " generated " + pageSequenceResults.getPageCount() + " pages.");
                }
                LOG.debug("Generated " + foResults.getPageCount() + " pages in total.");
            }
        } catch (IllegalStateException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while exporting "
                    + (type == PdfExportImpl.RTF ? "RTF" : "PDF"), e);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_EXPORT_PDF_FOP_FAILED, "Exception while exporting "
                    + (type == PdfExportImpl.RTF ? "RTF" : "PDF"), e);
        }
    }

    /**
     * Convert an HTML document to PDF. The HTML is cleaned up, and CSS style is applied to it.
     * 
     * @param html the source document to transform
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     */
    public void exportHtml(String html, OutputStream out, int type, XWikiContext context) throws XWikiException
    {
        exportXHtml(applyCSS(convertToStrictXHtml(html), context), out, type, context);
    }

    /**
     * Export a wiki Document into PDF. See {@link #export(XWikiDocument, OutputStream, int, XWikiContext)} for more
     * details about the conversion process.
     * 
     * @param doc the document to export
     * @param out where to write the resulting document
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     * @see #export(XWikiDocument, OutputStream, int, XWikiContext)
     */
    public void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException
    {
        export(doc, out, PdfExportImpl.PDF, context);
    }

    /**
     * Export a wiki Document into PDF or RTF. The content of the document is rendered into HTML using the {@code
     * pdf.vm} template, the resulting HTML is cleaned up into valid XHTML using JTidy, and custom CSS is applied to it.
     * If an OpenOffice service is configured and the output format is RTF, then the XHTML source is converted to RTF
     * using it. Otherwise, the XHTML document is transformed into an XSL-FO document, which is finally processed using
     * Apache FOP.
     * 
     * @param doc the document to export
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     */
    public void export(XWikiDocument doc, OutputStream out, int type, XWikiContext context) throws XWikiException
    {
        // Note: The passed document is not used currently since we're calling pdf.vm and that
        // velocity template uses the XWiki Context to get the current doc or its translations.
        // This could be improved by setting a specific context using the passed document but we
        // would also need to get the translations and set them too.

        File dir = context.getWiki().getTempDirectory(context);
        File tempdir = new File(dir, RandomStringUtils.randomAlphanumeric(8));
        this.tidy.setOutputEncoding(context.getWiki().getEncoding());
        this.tidy.setInputEncoding(context.getWiki().getEncoding());
        try {
            tempdir.mkdirs();
            context.put("pdfexportdir", tempdir);
            context.put("pdfexport-file-mapping", new HashMap<String, File>());
            boolean useLocalPlaceholders = !Utils.arePlaceholdersEnabled(context);
            if (useLocalPlaceholders) {
                Utils.enablePlaceholders(context);
            }
            String content = context.getWiki().parseTemplate("pdf.vm", context);
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
                LOG.warn("Failed to cleanup temporary files after a PDF export", ex);
            }
        }
    }

    /**
     * Cleans up an HTML document, turning it into valid XHTML.
     * 
     * @param input the source HTML to process
     * @return the cleaned up source
     */
    public String convertToStrictXHtml(String input)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cleaning HTML: " + input);
        }

        try {
            // First step, Tidy the document
            StringWriter out = new StringWriter(input.length());
            this.tidy.parse(new StringReader(input), out);

            // Tidy can't solve duplicate IDs, so it needs to be done manually
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(Utils.getComponent(EntityResolver.class));
            Document doc = docBuilder.parse(new InputSource(new StringReader(out.toString())));
            List<String> seenIDs = new ArrayList<String>();
            this.cleanIDs(doc.getDocumentElement(), seenIDs);

            // Write back the fixed document to a String
            LSOutput output = lsImpl.createLSOutput();
            StringWriter result = new StringWriter();
            output.setCharacterStream(result);
            LSSerializer serializer = lsImpl.createLSSerializer();
            serializer.setNewLine("\n");
            output.setEncoding(doc.getXmlEncoding());
            serializer.write(doc, output);
            return result.toString();
        } catch (Exception ex) {
            LOG.warn("Failed to tidy document for export: " + ex.getMessage(), ex);
            return input;
        }
    }

    /**
     * Solve duplicate IDs found in a document. When an already seen ID is encountered, a new ID is created for the
     * current element by suffixing its original ID with a counter.
     * 
     * @param e the current element to process
     * @param seenIDs the list of already encountered IDs so far
     */
    private void cleanIDs(org.w3c.dom.Element e, List<String> seenIDs)
    {
        String id = e.getAttribute("id");
        if (StringUtils.isNotEmpty(id)) {
            if (seenIDs.contains(id)) {
                int i = 0;
                while (seenIDs.contains(id + i)) {
                    ++i;
                }
                e.setAttribute("id", id + i);
                seenIDs.add(id + i);
            } else {
                seenIDs.add(id);
            }
        }
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            if (children.item(i) instanceof org.w3c.dom.Element) {
                cleanIDs((org.w3c.dom.Element) children.item(i), seenIDs);
            }
        }
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
    public String convertXHtmlToXMLFO(String xhtml, XWikiContext context) throws XWikiException
    {
        String xmlfo = applyXsl(xhtml, getXhtmlxsl(context));
        return applyXsl(xmlfo, getFopxsl(context));
    }

    /**
     * Applies an XSLT transformation to an XML document.
     * 
     * @param xml the XML document to convert
     * @param xslfile the name of the XSLT file to apply
     * @return the converted document
     * @throws XWikiException if the transformation fails for any reason
     */
    public String applyXsl(String xml, String xslfile) throws XWikiException
    {
        InputStream xsltinputstream = getClass().getClassLoader().getResourceAsStream(xslfile);

        Reader xmlinputstream = new StringReader(xml);
        StringWriter transout = new StringWriter(xml.length());

        try {
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(Utils.getComponent(EntityResolver.class));
            Document xslt = docBuilder.parse(new InputSource(xsltinputstream));
            Document xmldoc = docBuilder.parse(new InputSource(xmlinputstream));
            Transformer transformer = TransformerFactory.newInstance().newTransformer(new DOMSource(xslt));
            transformer.transform(new DOMSource(xmldoc), new StreamResult(transout));
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT, XWikiException.ERROR_XWIKI_EXPORT_XSL_FAILED,
                "XSL Transformation Failed", e);
        }

        return transout.toString();
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
     * @throws XWikiException if any exception occurs
     */
    public String applyCSS(String html, XWikiContext context) throws XWikiException
    {
        String css =
            ((context == null) || (context.getWiki() == null)) ? "" : context.getWiki().parseTemplate("pdf.css",
                context);
        String style = getPDFTemplateField("style", context);
        if (style != null) {
            css += style;
        }
        return applyCSS(html, css, context);
    }

    /**
     * Read a property from the current PDFTemplate document, and pass it through the Velocity engine.
     * 
     * @param propertyName the property to read
     * @param context the current request context
     * @return the content of the property, velocity-parsed, or an empty string if there's no such property in the
     *         current PDFTemplate
     */
    public String getPDFTemplateField(String fieldname, XWikiContext context) throws XWikiException
    {
        String fieldcontent = null;
        XWikiDocument doc = getPDFTemplateDocument(context);
        if (doc != null) {
            BaseObject bobj = doc.getXObject(new DocumentReference(context.getDatabase(), "XWiki", "PDFClass"));
            if (bobj != null) {
                fieldcontent = bobj.getLargeStringValue(fieldname);
                EntityReferenceSerializer<String> entityReferenceSerializer =
                    Utils.getComponent(EntityReferenceSerializer.class);
                try {
                    StringWriter writer = new StringWriter();
                    VelocityEngine engine = velocityManager.getVelocityEngine();
                    try {
                        VelocityContext vcontext = velocityManager.getVelocityContext();
                        engine.startedUsingMacroNamespace(entityReferenceSerializer.serialize(doc
                            .getDocumentReference()));
                        velocityManager.getVelocityEngine().evaluate(vcontext, writer,
                            entityReferenceSerializer.serialize(doc.getDocumentReference()), fieldcontent);
                        fieldcontent = writer.toString();
                    } finally {
                        engine.stoppedUsingMacroNamespace(entityReferenceSerializer.serialize(
                            doc.getDocumentReference()));
                    }
                } catch (XWikiVelocityException ex) {
                    LOG.warn("Velocity errors while parsing pdf export extension [" +
                        entityReferenceSerializer.serialize(doc.getDocumentReference()) + "]: " + ex.getMessage());
                }
            }
        }
        return fieldcontent;
    }

    public XWikiDocument getPDFTemplateDocument(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = null;
        XWikiRequest request = context.getRequest();
        if (request != null) {
            String pdftemplate = request.get("pdftemplate");
            if (pdftemplate != null) {
                doc = context.getWiki().getDocument(pdftemplate, context);
            }
        }
        if (doc == null) {
            doc = context.getDoc();
        }
        return doc;
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
    public String applyCSS(String html, String css, XWikiContext context)
    {
        try {
            // Prepare the input
            Reader re = new StringReader(html);
            InputSource source = new InputSource(re);
            SAXReader reader = new SAXReader(XHTMLDocumentFactory.getInstance());
            reader.setEntityResolver(new DefaultEntityResolver());
            XHTMLDocument document = (XHTMLDocument) reader.read(source);

            // Apply the style sheet
            document.setDefaultStyleSheet(new DOM4JCSSStyleSheet(null, null, null));
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("HTML with CSS applied: " + result);
            }
            return result;
        } catch (Exception ex) {
            LOG.warn("Failed to apply CSS: " + ex.getMessage(), ex);
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
     * Get the XSLT for post-processing the XSL-FO file. The content is searched in:
     * <ol>
     * <li>the <tt>fopxsl</tt> property of the current PDFTemplate</li>
     * <li>the <tt>fop.xsl</tt> resource (usually a file inside xwiki-core-*.jar)</li>
     * </ol>
     * 
     * @param context the current request context
     * @return the content of the XSLT as a byte stream
     */
    public String getFopxsl(XWikiContext context) throws XWikiException
    {
        String xsl = getPDFTemplateField("fopxsl", context);
        if ((xsl == null) || ("".equals(xsl.trim()))) {
            return DEFAULT_CLEANUP_XSLT;
        } else {
            return xsl;
        }
    }
}
