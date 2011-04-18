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
import java.util.HashMap;
import java.util.List;
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
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.xml.EntityResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.pdf.api.PdfExport.ExportType;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

public class PdfExportImpl implements PdfExport
{
    private static final Log log = LogFactory.getLog(PdfExportImpl.class);

    private Tidy tidy;

    private String xhtmlxsl = "xhtml2fo.xsl";

    private String fopxsl = "fop.xsl";

    /** DOM parser factory. */
    private static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    /** DOM Serializer factory. */
    private static DOMImplementationLS lsImpl;

    private static FopFactory fopFactory;

    static {
        dbFactory.setNamespaceAware(true);
        dbFactory.setValidating(false);

        try {
            lsImpl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        } catch (Exception ex) {
            log.warn("Cannot initialize the DomLS implementation needed by the PDF export: " + ex.getMessage());
        }

        fopFactory = FopFactory.newInstance();
        try {
            fopFactory.getFontManager().setFontBaseURL(
                (Utils.getComponent(Container.class)).getApplicationContext().getResource("/WEB-INF/fonts/").getPath());
        } catch (Throwable ex) {
            log.warn("Starting with 1.5, XWiki uses the WEB-INF/fonts/ directory as the font directory, "
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
                log.warn("Wrong FOP configuration: " + ex.getMessage());
            }
        }
    }

    public PdfExportImpl()
    {
        this.tidy = new Tidy();

        // Setup a default configuration for Tidy
        Properties baseConfiguration = new Properties();
        baseConfiguration.setProperty("quiet", "true");
        baseConfiguration.setProperty("clean", "true");
        baseConfiguration.setProperty("tidy-mark", "false");
        baseConfiguration.setProperty("output-xhtml", "true");
        baseConfiguration.setProperty("show-warnings", "false");
        baseConfiguration.setProperty("trim-empty-elements", "false");
        baseConfiguration.setProperty("numeric-entities", "true");
        // Don't replace non-ASCII apostrophes/quotes/dashes with plain ASCII ones.
        baseConfiguration.setProperty("ascii-chars", "false");
        // Don't wrap, as it is not needed, and it triggers JTidy bugs when combined with non-ASCII chars.
        baseConfiguration.setProperty("wrap", "0");

        // Allow Tidy to be configured in the tidy.properties file
        Properties configuration = new Properties(baseConfiguration);
        try {
            configuration.load(this.getClass().getClassLoader().getResourceAsStream("/tidy.properties"));
        } catch (IOException ex) {
            log.warn("Tidy configuration file could not be read. Using default configuration.");
        } catch (NullPointerException ex) {
            log.warn("Tidy configuration file doesn't exist. Using default configuration.");
        }

        this.tidy.setConfigurationFromProps(configuration);
    }

    public String getXhtmlxsl()
    {
        return this.xhtmlxsl;
    }

    public String getXhtmlxsl(XWikiContext context) throws XWikiException
    {
        String xsl = getPDFTemplateField("xhtmlxsl", context);
        if (StringUtils.isBlank(xsl)) {
            return this.xhtmlxsl;
        } else {
            return xsl;
        }
    }

    public void setXhtmlxsl(String xhtmlxsl)
    {
        this.xhtmlxsl = xhtmlxsl;
    }

    public void exportXHtml(String xhtml, OutputStream out, ExportType type, XWikiContext context) throws XWikiException
    {
        if (log.isDebugEnabled()) {
            log.debug("Final XHTML for export: " + xhtml);
        }

        // XSL Transformation to XML-FO
        String xmlfo = convertXHtmlToXMLFO(xhtml, context);

        // Debug output
        if (log.isDebugEnabled()) {
            log.debug("XSL-FO source: " + xmlfo);
        }

        exportXMLFO(xmlfo, out, type);
    }

    public void exportXMLFO(String xmlfo, OutputStream out, ExportType type) throws XWikiException
    {
        // XSL Transformation to XML-FO

        try {
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(type.getMimeType(), foUserAgent, out);

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
            if (foResults != null && log.isDebugEnabled()) {
                java.util.List<PageSequenceResults> pageSequences = foResults.getPageSequences();
                for (PageSequenceResults pageSequenceResults : pageSequences) {
                    log.debug("PageSequence " + StringUtils.defaultIfEmpty(pageSequenceResults.getID(), "<no id>")
                        + " generated " + pageSequenceResults.getPageCount() + " pages.");
                }
                log.debug("Generated " + foResults.getPageCount() + " pages in total.");
            }
        } catch (IllegalStateException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while exporting "
                    + type.getExtension(), e);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_EXPORT_PDF_FOP_FAILED, "Exception while exporting "
                    + type.getExtension(), e);
        }
    }

    public void exportHtml(String html, OutputStream out, ExportType type, XWikiContext context) throws XWikiException
    {
        exportXHtml(applyCSS(convertToStrictXHtml(html), context), out, type, context);
    }

    public void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException
    {
        export(doc, out, ExportType.PDF, context);
    }

    public void export(XWikiDocument doc, OutputStream out, ExportType type, XWikiContext context) throws XWikiException
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
                log.warn("Failed to cleanup temporary files after a PDF export", ex);
            }
        }
    }

    public String convertToStrictXHtml(String input)
    {
        if (log.isDebugEnabled()) {
            log.debug("Cleaning HTML: " + input);
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
            log.warn("Failed to tidy document for export: " + ex.getMessage(), ex);
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

    public String convertXHtmlToXMLFO(String xhtml, XWikiContext context) throws XWikiException
    {
        String xmlfo = applyXsl(xhtml, getXhtmlxsl(context));
        return applyXsl(xmlfo, getFopxsl(context));
    }

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
                    VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
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
                    log.warn("Velocity errors while parsing pdf export extension [" +
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
            OutputFormat outputFormat = new OutputFormat("", true);
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
            if (log.isDebugEnabled()) {
                log.debug("HTML with CSS applied: " + result);
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to apply CSS: " + ex.getMessage(), ex);
            return html;
        }
    }

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

    public String getFopxsl()
    {
        return this.fopxsl;
    }

    public String getFopxsl(XWikiContext context) throws XWikiException
    {
        String xsl = getPDFTemplateField("fopxsl", context);
        if ((xsl == null) || ("".equals(xsl.trim()))) {
            return this.fopxsl;
        } else {
            return xsl;
        }
    }

    public void setFopxsl(String fopxsl)
    {
        this.fopxsl = fopxsl;
    }

    public static void main(String[] argv) throws IOException, XWikiException
    {
        String param = argv[0];
        String inputfile;
        String outputfile;
        String content;
        PdfExportImpl pdf = new PdfExportImpl();
        XWikiContext context = new XWikiContext();

        if (param.equals("-html2xhtml")) {
            // HTML TO XHTML
            inputfile = argv[1];
            outputfile = argv[2];
            content = Util.getFileContent(new File(inputfile));
            byte[] xhtml = pdf.convertToStrictXHtml(content.getBytes(), context);
            saveFile(outputfile, xhtml);
        } else if (param.equals("-html2xmlfo")) {
            inputfile = argv[1];
            outputfile = argv[2];
            content = Util.getFileContent(new File(inputfile));
            // XHTML TO XMLFO
            byte[] xhtml = pdf.convertXHtmlToXMLFO(pdf.convertToStrictXHtml(content.getBytes(), context), context);
            saveFile(outputfile, xhtml);
        } else if (param.equals("-xmlfo2pdf")) {
            inputfile = argv[1];
            outputfile = argv[2];
            content = Util.getFileContent(new File(inputfile));
            // XML-FO2 PDF
            FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportXMLFO(content, fos, ExportType.PDF);
            fos.close();
        } else if (param.equals("-html2pdf")) {
            inputfile = argv[1];
            outputfile = argv[2];
            content = Util.getFileContent(new File(inputfile));
            // PDF
            FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportHtml(content, fos, ExportType.PDF, context);
            fos.close();
        } else {
            inputfile = param;
            outputfile = argv[1];
            content = Util.getFileContent(new File(inputfile));
            // PDF
            FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportHtml(content, fos, ExportType.PDF, context);
            fos.close();
        }
    }

    public static void saveFile(String filename, byte[] content) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(new File(filename));
        fos.write(content);
        fos.close();
    }

    public byte[] convertToStrictXHtml(byte[] input, XWikiContext context)
    {
        try {
            return convertToStrictXHtml(new String(input, context.getWiki().getEncoding())).getBytes(
                context.getWiki().getEncoding());
        } catch (UnsupportedEncodingException ex) {
            log.error("Unsupported encoding: " + context.getWiki().getEncoding(), ex);
            return input;
        }
    }

    public byte[] convertXHtmlToXMLFO(byte[] input, XWikiContext context) throws XWikiException
    {
        try {
            return convertXHtmlToXMLFO(new String(input, context.getWiki().getEncoding()), context).getBytes(
                context.getWiki().getEncoding());
        } catch (UnsupportedEncodingException ex) {
            log.error("Unsupported encoding: " + context.getWiki().getEncoding(), ex);
            return input;
        }
    }

    public void exportXHtml(byte[] xhtml, OutputStream out, ExportType type, XWikiContext context) throws XWikiException
    {
        exportXHtml(new String(xhtml), out, type, context);
    }
}
