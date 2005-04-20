/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 16 août 2004
 * Time: 11:01:17
 */
package com.xpn.xwiki.pdf.impl;

import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

import org.w3c.tidy.Tidy;
import org.w3c.tidy.Configuration;
import org.w3c.dom.Document;
import org.apache.fop.apps.Driver;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.lang.RandomStringUtils;
import org.xml.sax.InputSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.util.Properties;

public class PdfExportImpl implements PdfExport {
    private Tidy tidy;
    private String xhtmlxsl = "xhtml2fo.xsl";
    private String fopxsl = "fop.xsl";
    private static final int PDF = 0;
    private static final int RTF = 1;

    public PdfExportImpl() {
        tidy = new Tidy();
        Properties props = new Properties();
        props.setProperty("quiet", "true");
        props.setProperty("quoteAmpersand", "true");
        props.setProperty("xHtml", "true");
        props.setProperty("showWarnings", "false");
        props.setProperty("tidyMark", "false");
        props.setProperty("clean", "true");
        tidy.setConfigurationFromProps(props);
        tidy.setCharEncoding(Configuration.LATIN1);
    }

    public String getXhtmlxsl() {
        return xhtmlxsl;
    }

    public void setXhtmlxsl(String xhtmlxsl) {
        this.xhtmlxsl = xhtmlxsl;
    }

    public void exportXHtml(byte[] xhtml, OutputStream out, int type) throws XWikiException {
        // XSL Transformation to XML-FO
        byte[] xmlfo = convertXHtmlToXMLFO(xhtml);
        exportXMLFO(xmlfo, out, type);
    }

    public void exportXMLFO(byte[] xmlfo, OutputStream out, int type) throws XWikiException {
        // XSL Transformation to XML-FO

        try {
            Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);

            // Reset the image cache otherwise it could be a security issue
            org.apache.fop.image.FopImageFactory.resetCache();

            InputSource source = new InputSource(new ByteArrayInputStream(xmlfo));
            Driver driver = new Driver(source, out);

            driver.setRenderer(Driver.RENDER_PDF);
            driver.setLogger(logger);
            driver.setErrorDump(true);
            driver.run();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                    XWikiException.ERROR_XWIKI_EXPORT_PDF_FOP_FAILED,
                    "Exception while exporting PDF", e);
        }

    }

    public void exportHtml(String html, OutputStream out, int type) throws XWikiException {
        exportXHtml(convertToStrictXHtml(html.getBytes()), out, type);
    }

    public void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException {
        export(doc, out, PdfExportImpl.PDF, context);
    }

    public void export(XWikiDocument doc, OutputStream out, int type, XWikiContext context) throws XWikiException {
        File dir = (File) context.getEngineContext().getAttribute("javax.servlet.context.tempdir");
        File tempdir = new File(dir, RandomStringUtils.randomAlphanumeric(8));
        try {
            tempdir.mkdirs();
            context.put("pdfexportdir", tempdir);
            String content = context.getWiki().parseTemplate("pdf.vm", context);
            exportHtml(content, out, type);
        } finally {
            File[] filelist = tempdir.listFiles();
            for (int i=0;i<filelist.length;i++)
                filelist[i].delete();
            tempdir.delete();
        }
    }

    public byte[] convertToStrictXHtml(byte[] input) {

        System.out.println(new String(input));

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

    public byte[] convertXHtmlToXMLFO(byte[] xhtml) throws XWikiException {
        byte[] xmlfo = applyXsl(xhtml, getXhtmlxsl());
        return applyXsl(xmlfo, getFopxsl());
    }

    public byte[] applyXsl(byte[] xml, String xslfile) throws XWikiException {
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
            transformer.transform(new DOMSource(xmldoc),
                    new StreamResult(transout));
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                    XWikiException.ERROR_XWIKI_EXPORT_XSL_FAILED,
                    "XSL Transformation Failed", e);
        }

        return transout.toByteArray();
    }

    public String getFopxsl() {
        return fopxsl;
    }

    public void setFopxsl(String fopxsl) {
        this.fopxsl = fopxsl;
    }

    public static void main(String[] argv) throws IOException, XWikiException {
        String param = (String) argv[0];
        String inputfile;
        String outputfile;
        String content;
        PdfExportImpl pdf = new PdfExportImpl();

        if (param.equals("-html2xhtml")) {
            // HTML TO XHTML
            inputfile = (String) argv[1];
            outputfile = (String) argv[2];
            content = Util.getFileContent(new File(inputfile));
            byte[] xhtml = pdf.convertToStrictXHtml(content.getBytes());
            saveFile(outputfile, xhtml);
        } else if (param.equals("-html2xmlfo")) {
            inputfile = (String) argv[1];
            outputfile = (String) argv[2];
            content = Util.getFileContent(new File(inputfile));
            // XHTML TO XMLFO
            byte[] xhtml = pdf.convertXHtmlToXMLFO(pdf.convertToStrictXHtml(content.getBytes()));
            saveFile(outputfile, xhtml);
        } else if (param.equals("-xmlfo2pdf")) {
            inputfile = (String) argv[1];
            outputfile = (String) argv[2];
            content = Util.getFileContent(new File(inputfile));
            // XML-FO2 PDF
            FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportXMLFO(content.getBytes(), fos,PdfExportImpl.PDF);
            fos.close();
        } else if (param.equals("-html2pdf")) {
            inputfile = (String) argv[1];
            outputfile = (String) argv[2];
            content = Util.getFileContent(new File(inputfile));
            // PDF
            FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportHtml(content, fos,PdfExportImpl.PDF);
            fos.close();
        } else {
            inputfile = param;
            outputfile = (String) argv[1];
            content = Util.getFileContent(new File(inputfile));
            // PDF
                FileOutputStream fos = new FileOutputStream(new File(outputfile));
            pdf.exportHtml(content, fos,PdfExportImpl.PDF);
            fos.close();
        }
    }

    public static void saveFile(String filename, byte[] content) throws IOException {
     FileOutputStream fos = new FileOutputStream(new File(filename));
     fos.write(content);
     fos.close();
    }

}
