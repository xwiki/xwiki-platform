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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiRequest;
import info.informatica.doc.dom4j.CSSStylableElement;
import info.informatica.doc.dom4j.XHTMLDocument;
import info.informatica.doc.dom4j.XHTMLDocumentFactory;
import info.informatica.doc.style.css.dom.DOMCSSStyleSheet;
import info.informatica.doc.xml.dtd.DefaultEntityResolver;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.PageSequenceResults;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xml.sax.InputSource;

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

public class PdfExportImpl implements PdfExport
{
    private static final Log log = LogFactory.getLog(PdfExportImpl.class);

    private Tidy tidy;

    private String xhtmlxsl = "xhtml2fo.xsl";

    private String fopxsl = "fop.xsl";

    public static final int PDF = 0;

    public static final int RTF = 1;

    public PdfExportImpl()
    {
        tidy = new Tidy();
        Properties props = new Properties();
        props.setProperty("quiet", "true");
        /*
        props.setProperty("quoteAmpersand", "true");
        props.setProperty("xHtml", "true");
        props.setProperty("showWarnings", "false");
        props.setProperty("tidyMark", "false");
        */
        props.setProperty("clean", "true");
        tidy.setConfigurationFromProps(props);
        tidy.setTrimEmptyElements(false);
    }

    public String getXhtmlxsl()
    {
        return xhtmlxsl;
    }

    public String getXhtmlxsl(XWikiContext context) throws XWikiException
    {
        String xsl = getPDFTemplateField("xhtmlxsl", context);
        if ((xsl == null) || ("".equals(xsl.trim()))) {
            return xhtmlxsl;
        } else {
            return xsl;
        }
    }

    public void setXhtmlxsl(String xhtmlxsl)
    {
        this.xhtmlxsl = xhtmlxsl;
    }

    public void exportXHtml(byte[] xhtml, OutputStream out, int type, XWikiContext context) throws XWikiException
    {
        // XSL Transformation to XML-FO
        byte[] xmlfo = convertXHtmlToXMLFO(xhtml, context);

        // DEBUG OUTPUT
        if (log.isDebugEnabled()) {
            log.debug(new String(xmlfo));
        }

        exportXMLFO(xmlfo, out, type);
    }

    public void exportXMLFO(byte[] xmlfo, OutputStream out, int type) throws XWikiException
    {
        // XSL Transformation to XML-FO

        try {
            FopFactory fopFactory = FopFactory.newInstance();
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop((type == PdfExportImpl.RTF) ? MimeConstants.MIME_RTF : MimeConstants.MIME_PDF,
                foUserAgent, out);

            // Setup JAXP using identity transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer

            // Setup input stream
            Source source = new StreamSource(new ByteArrayInputStream(xmlfo));

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(source, res);

            // Result processing
            FormattingResults foResults = fop.getResults();
            if (foResults != null) {
                java.util.List pageSequences = foResults.getPageSequences();
                for (java.util.Iterator it = pageSequences.iterator(); it.hasNext();) {
                    PageSequenceResults pageSequenceResults = (PageSequenceResults) it.next();
                    if (log.isDebugEnabled()) {
                        log.debug("PageSequence "
                            + (String.valueOf(pageSequenceResults.getID()).length() > 0
                            ? pageSequenceResults.getID() : "<no id>")
                            + " generated " + pageSequenceResults.getPageCount() + " pages.");
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Generated " + foResults.getPageCount() + " pages in total.");
                }
            }
            /*
            // Reset the image cache otherwise it could be a security issue
            // This should not be necessary anymore in 0.93
            // org.apache.fop.image.ImageFactory.clearCaches();

            Driver driver = new Driver(source, out);

            driver.setRenderer(Driver.RENDER_PDF);
            driver.setLogger(logger);
            driver.setErrorDump(true);
            driver.run();
            */

        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_EXPORT_PDF_FOP_FAILED,
                "Exception while exporting PDF", e);
        }
    }

    public void exportHtml(String html, OutputStream out, int type, XWikiContext context) throws XWikiException
    {
        exportXHtml(applyCSS(convertToStrictXHtml(html.getBytes(), context), context), out, type, context);
    }

    public void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException
    {
        export(doc, out, PdfExportImpl.PDF, context);
    }

    public void export(XWikiDocument doc, OutputStream out, int type, XWikiContext context) throws XWikiException
    {
        File dir = (File) context.getEngineContext().getAttribute("javax.servlet.context.tempdir");
        File tempdir = new File(dir, RandomStringUtils.randomAlphanumeric(8));
        tidy.setOutputEncoding(context.getWiki().getEncoding());
        tidy.setInputEncoding(context.getWiki().getEncoding());
        try {
            tempdir.mkdirs();
            context.put("pdfexportdir", tempdir);
            String content = context.getWiki().parseTemplate("pdf.vm", context);
            exportHtml(content, out, type, context);
        } finally {
            File[] filelist = tempdir.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                filelist[i].delete();
            }
            tempdir.delete();
        }
    }

    public byte[] convertToStrictXHtml(byte[] input, XWikiContext context)
    {

        if (log.isDebugEnabled()) {
            log.debug(new String(input));
        }

        try {
            InputStream in = new ByteArrayInputStream(input);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            tidy.parse(in, out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    public byte[] convertXHtmlToXMLFO(byte[] xhtml, XWikiContext context) throws XWikiException
    {
        byte[] xmlfo = applyXsl(xhtml, getXhtmlxsl(context));
        return applyXsl(xmlfo, getFopxsl(context));
    }

    public byte[] applyXsl(byte[] xml, String xslfile) throws XWikiException
    {
        InputStream xsltinputstream = getClass().getClassLoader().getResourceAsStream(xslfile);

        InputStream xmlinputstream = new ByteArrayInputStream(xml);
        ByteArrayOutputStream transout = new ByteArrayOutputStream();

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            docFactory.setValidating(false);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new XWikiURIResolver());
            Document xslt = docBuilder.parse(new InputSource(xsltinputstream));
            Document xmldoc = docBuilder.parse(new InputSource(xmlinputstream));
            Transformer transformer = TransformerFactory.newInstance().newTransformer(new DOMSource(xslt));
            transformer.transform(new DOMSource(xmldoc), new StreamResult(transout));
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_EXPORT_XSL_FAILED,
                "XSL Transformation Failed", e);
        }

        return transout.toByteArray();
    }

    public byte[] applyCSS(byte[] html, XWikiContext context) throws XWikiException
    {
        String css = ((context == null) || (context.getWiki() == null)) ? "" :
            context.getWiki().parseTemplate("pdf.css", context);
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
            BaseObject bobj = doc.getObject("XWiki.PDFClass");
            if (bobj != null) {
                fieldcontent = doc.display(fieldname, "view", bobj, context);
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

    public byte[] applyCSS(byte[] html, String css, XWikiContext context)
    {
        try {
            Reader re = new StringReader(new String(html));
            InputSource source = new InputSource(re);
            SAXReader reader = new SAXReader(XHTMLDocumentFactory.getInstance());
            reader.setEntityResolver(new DefaultEntityResolver());
            XHTMLDocument document = (XHTMLDocument) reader.read(source);

            // Apply the style sheet
            document.setDefaultStyleSheet(new DOMCSSStyleSheet(null, null));
            document.getStyleSheet();
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
            // DEBUG OUTPUT
            if (log.isDebugEnabled()) {
                log.debug(result);
            }
            return result.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return html;
        }
    }

    private void applyInlineStyle(Element element)
    {
        for (int i = 0; i < element.nodeCount(); i++) {
            org.dom4j.Node node = element.node(i);
            if (node instanceof CSSStylableElement) {
                CSSStylableElement styleElement = (CSSStylableElement) node;
                if (styleElement.getComputedStyle() != null) {
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
        return fopxsl;
    }

    public String getFopxsl(XWikiContext context) throws XWikiException
    {
        String xsl = getPDFTemplateField("fopxsl", context);
        if ((xsl == null) || ("".equals(xsl.trim()))) {
            return fopxsl;
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
            pdf.exportXMLFO(content.getBytes(), fos, PdfExportImpl.PDF);
            fos.close();
        } else if (param.equals("-html2pdf")) {
            inputfile = argv[1];
            outputfile = argv[2];
            content = Util.getFileContent(new File(inputfile));
            // PDF
            FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportHtml(content, fos, PdfExportImpl.PDF, context);
            fos.close();
        } else {
            inputfile = param;
            outputfile = argv[1];
            content = Util.getFileContent(new File(inputfile));
            // PDF
            FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportHtml(content, fos, PdfExportImpl.PDF, context);
            fos.close();
        }
    }

    public static void saveFile(String filename, byte[] content) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(new File(filename));
        fos.write(content);
        fos.close();
    }
}
