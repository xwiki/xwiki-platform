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
package com.xpn.xwiki.internal.pdf;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.fop.apps.EnvironmentProfile;
import org.apache.fop.apps.EnvironmentalProfileFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.io.ResourceResolverFactory;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiXMLReaderFactory;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.xml.EntityResolver;
import org.xwiki.xml.XMLReaderFactory;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.internal.DefaultXMLReaderFactory;
import org.xwiki.xml.internal.LocalEntityResolver;

import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.*;

/**
 * Integration tests to verify that the {@code html2fo.xsl} XSL stylesheet works.
 *
 * @version $Id$
 */
@ComponentList({
    DefaultXMLReaderFactory.class,
    XWikiXMLReaderFactory.class,
    LocalEntityResolver.class
})
public class XHTML2FOTest
{
    @Rule
    public AllLogRule logRule = new AllLogRule();

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private static FopFactory fopFactory;

    private static FOUserAgent foUserAgent;

    private static TransformerFactory transformerFactory;

    @BeforeClass
    public static void setUp() throws Exception
    {
        EnvironmentProfile environmentProfile = EnvironmentalProfileFactory.createDefault(new File(".").toURI(),
            ResourceResolverFactory.createDefaultResourceResolver());
        FopFactoryBuilder builder = new FopFactoryBuilder(environmentProfile);
        fopFactory = builder.build();

        foUserAgent = fopFactory.newFOUserAgent();
        transformerFactory = TransformerFactory.newInstance();
    }

    /**
     * Verifies that we can have some style using the "font" attribute.
     */
    @Test
    public void transformWithFontStyle() throws Exception
    {
        String xml = constructXML(
            "<p style=\"display: block; margin: 1em 0; color: red; \">\n"
            + "  <span style=\"font: 7px 14px Courier; \">Test</span>\n"
            + "</p>");

        String transformedXML = getTransformedXML(xml);
        String expectedXML = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("xhtml2fo.expected"));

        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = new Diff(expectedXML, transformedXML);
        assertTrue("XML is not similar [" + diff.toString() + "]", diff.identical());
    }

    @Test
    public void transformWhenUnrecognizedCSSProperties() throws Exception
    {
        String xml = constructXML(
            "<div style=\"box-sizing: border-box; \">\n"
            + "<span style=\"text-justify: inter-ideograph; line-height: normal; text-autospace: none;\">text</span>\n"
            + "</div>");

        String transformedXML = getTransformedXML(xml);
        assertFalse("Generated FO shouldn't contain 'box-sizing'", transformedXML.contains("box-sizing"));
        assertFalse("Generated FO shouldn't contain 'text-justify'", transformedXML.contains("text-justify"));
        assertFalse("Generated FO shouldn't contain 'text-autospace'", transformedXML.contains("text-autospace"));
    }

    private String constructXML(String xmlContent)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "    <title>\n"
            + "          Main.test2 - test2\n"
            + "    </title>\n"
            + "    <meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\" />\n"
            + "    <meta content=\"en\" name=\"language\" />\n"
            + "</head>\n"
            + "<body class=\"exportbody\" id=\"body\" pdfcover=\"0\" pdftoc=\"0\" "
            + "style=\"display: block; margin: 8pt; \">\n"
            + "    <div class=\"pdfheader\" style=\"display: block; \">\n"
            + "            Main.test2 - test2\n"
            + "        \n"
            + "    </div>\n"
            + "    <div class=\"pdffooter\" style=\"display: block; \">\n"
            + "          Page <span class=\"page-number\">\n"
            + "    </span> of <span class=\"page-total\">\n"
            + "    </span> - last modified by Administrator on 2016/01/25 14:07\n"
            + "    \n"
            + "</div>\n"
            + "<div id=\"xwikimaincontainer\" style=\"display: block; \">\n"
            + "    <div id=\"xwikimaincontainerinner\" style=\"display: block; \">\n"
            + "        <div id=\"xwikicontent\" style=\"display: block; \">\n"
            + xmlContent + "\n"
            + "        </div>\n"
            + "    </div>\n"
            + "</div>\n"
            + "</body>\n"
            + "</html>\n";
    }

    private String getTransformedXML(String xml) throws Exception
    {
        InputStream xslt = getClass().getClassLoader().getResourceAsStream("xhtml2fo.xsl");
        assertNotNull(xslt);

        XMLReaderFactory xmlReaderFactory = this.oldcore.getMocker().getInstance(XMLReaderFactory.class);
        XMLReader xmlReader = xmlReaderFactory.createXMLReader();
        EntityResolver entityResolver = this.oldcore.getMocker().getInstance(EntityResolver.class);
        xmlReader.setEntityResolver(entityResolver);
        SAXSource xmlSource = new SAXSource(xmlReader, new InputSource(new StringReader(xml)));
        SAXSource xsltSource = new SAXSource(xmlReader, new InputSource(xslt));

        String transformedXML = XMLUtils.transform(xmlSource, xsltSource);

        validateFOP(transformedXML);

        return transformedXML;
    }

    private void validateFOP(String transformedXML) throws Exception
    {
        // Run FOP to verify that the XML passed as input is some valid FO and doesn't generate errors

        Fop fop = fopFactory.newFop(new PdfExport.ExportType("application/pdf", "pdf").getMimeType(), foUserAgent,
            NullOutputStream.NULL_OUTPUT_STREAM);

        Transformer transformer = transformerFactory.newTransformer();

        Source source = new StreamSource(new StringReader(transformedXML));

        Result res = new SAXResult(fop.getDefaultHandler());

        transformer.transform(source, res);
    }
}
