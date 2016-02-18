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

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.transform.sax.SAXSource;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xwiki.rendering.internal.parser.xhtml.wikimodel.XWikiXMLReaderFactory;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.xml.EntityResolver;
import org.xwiki.xml.XMLReaderFactory;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.internal.DefaultXMLReaderFactory;
import org.xwiki.xml.internal.LocalEntityResolver;

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
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    /**
     * Verifies that we can have some style using the "font" attribute.
     */
    @Test
    public void transformWithFontStyle() throws Exception
    {
        // Get the xhtml2fo.xsl stylesheet
        InputStream xslt = getClass().getClassLoader().getResourceAsStream("xhtml2fo.xsl");
        assertNotNull(xslt);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
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
            + "            <p style=\"display: block; margin: 1em 0; color: red; \">\n"
            + "                <span style=\"font: 7px 14px Courier; \">Test</span>\n"
            + "            </p>\n"
            + "        </div>\n"
            + "    </div>\n"
            + "</div>\n"
            + "</body>\n"
            + "</html>\n";

        XMLReaderFactory xmlReaderFactory = this.oldcore.getMocker().getInstance(XMLReaderFactory.class);
        XMLReader xmlReader = xmlReaderFactory.createXMLReader();
        EntityResolver entityResolver = this.oldcore.getMocker().getInstance(EntityResolver.class);
        xmlReader.setEntityResolver(entityResolver);
        SAXSource xmlSource = new SAXSource(xmlReader, new InputSource(new StringReader(xml)));
        SAXSource xsltSource = new SAXSource(xmlReader, new InputSource(xslt));

        String transformedXML = XMLUtils.transform(xmlSource, xsltSource);
        String expectedXML = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("xhtml2fo.expected"));

        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = new Diff(expectedXML, transformedXML);
        assertTrue("XML is not similar [" + diff.toString() + "]", diff.identical());
    }
}
