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
package org.xwiki.xml.internal;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link XMLScriptService}.
 * 
 * @version $Id$
 * @since 2.7RC1
 */
public class XMLScriptServiceTest extends AbstractComponentTestCase
{
    private XMLScriptService xml;

    @Override
    public void setUp() throws Exception
    {
        this.xml = (XMLScriptService) getComponentManager().getInstance(ScriptService.class, "xml");
    }



    @Test
    public void testGetDomDocument()
    {
        // Nothing much that we can test here...
        Assert.assertNotNull(this.xml.createDOMDocument());
    }

    @Test
    public void testParseString()
    {
        Document result = this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>");
        Assert.assertNotNull("Failed to parse content", result);
        Assert.assertEquals("Incorrect root node", "a", result.getDocumentElement().getLocalName());
    }

    @Test
    public void testParseByteArray() throws UnsupportedEncodingException
    {
        Document result = this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>".getBytes("UTF-8"));
        Assert.assertNotNull("Failed to parse content", result);
        Assert.assertEquals("Incorrect root node", "a", result.getDocumentElement().getLocalName());
    }

    @Test
    public void testParseInputStream() throws UnsupportedEncodingException
    {
        Document result = this.xml.parse(
            new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>".getBytes("UTF-8")));
        Assert.assertNotNull("Failed to parse content", result);
        Assert.assertEquals("Incorrect root node", "a", result.getDocumentElement().getLocalName());
    }

    @Test
    public void testParseWithDifferentEncoding() throws UnsupportedEncodingException
    {
        Document result =
            this.xml.parse("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a>\u00E9</a>".getBytes("ISO-8859-1"));
        Assert.assertNotNull("Failed to parse content", result);
        Assert.assertEquals("Incorrect root node", "a", result.getDocumentElement().getLocalName());
        Assert.assertEquals("Incorrect content", "\u00E9", result.getDocumentElement().getTextContent());
    }

    @Test
    public void testParseWithWrongEncoding() throws UnsupportedEncodingException
    {
        Document result =
            this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>\u00E9</a>".getBytes("ISO-8859-1"));
        Assert.assertNull("Content should be invalid with the specified encoding", result);
    }

    @Test
    public void testParseWithoutXMLDeclaration()
    {
        Document result = this.xml.parse("<a>\u00E9</a>");
        Assert.assertNotNull("Failed to parse content", result);
        Assert.assertEquals("Incorrect root node", "a", result.getDocumentElement().getLocalName());
    }

    @Test
    public void testParseInvalidDocument() throws UnsupportedEncodingException
    {
        Document result = this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a></b>");
        Assert.assertNull("Invalid content shouldn't be parsed", result);
    }

    @Test
    public void testParseNull()
    {
        Document result = this.xml.parse((String) null);
        Assert.assertNull("Null Document input shouldn't be parsed", result);
        result = this.xml.parse((byte[]) null);
        Assert.assertNull("Null byte[] input shouldn't be parsed", result);
        result = this.xml.parse((ByteArrayInputStream) null);
        Assert.assertNull("Null InputStream input shouldn't be parsed", result);
        result = this.xml.parse((LSInput) null);
        Assert.assertNull("Null LSInput input shouldn't be parsed", result);
    }

    @Test
    public void testParseAndSerialize()
    {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>\u00E9</a>";
        String result = this.xml.serialize(this.xml.parse(content));
        Assert.assertEquals("Not identical content after parse + serialize", content, result);

        content = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<a>\u00E9</a>";
        result = this.xml.serialize(this.xml.parse(content));
        Assert.assertEquals("Not identical content after parse + serialize", content, result);

        content = "<a>\u00E9</a>";
        result = this.xml.serialize(this.xml.parse(content), false);
        Assert.assertEquals("Not identical content after parse + serialize", content, result);
    }

    @Test
    public void testSerialize()
    {
        Document d = createSimpleDocument();
        String result = this.xml.serialize(d);
        Assert.assertEquals("Wrong serialization", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>\u00E9</a>", result);
    }

    @Test
    public void testSerializeDocumentElement()
    {
        Document d = createSimpleDocument();
        String result = this.xml.serialize(d.getDocumentElement());
        Assert.assertEquals("Wrong serialization", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>\u00E9</a>", result);
    }

    @Test
    public void testSerializeNode()
    {
        Document d = createSimpleDocument();
        Element b = d.createElement("b");
        b.setTextContent("c");
        d.getDocumentElement().appendChild(b);
        String result = this.xml.serialize(d.getDocumentElement().getElementsByTagName("b").item(0));
        Assert.assertEquals("Wrong serialization", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<b>c</b>", result);
    }

    @Test
    public void testSerializeNull()
    {
        Assert.assertEquals("Wrong serialization for null document", "", this.xml.serialize(null));
    }

    public void testSerializeWithoutXmlDeclaration()
    {
        Document d = createSimpleDocument();
        String result = this.xml.serialize(d.getDocumentElement(), false);
        Assert.assertEquals("Wrong serialization", "<a>\u00E9</a>", result);
    }

    @Test
    public void testNewlineSerialization()
    {
        Document d = this.xml.parse("<a>a\nb\n</a>");
        String result = this.xml.serialize(d, false);
        Assert.assertEquals("Wrong newlines", "<a>a\nb\n</a>", result);

        d = this.xml.parse("<a>a\r\nb\r\n</a>");
        result = this.xml.serialize(d, false);
        Assert.assertEquals("Wrong newlines", "<a>a\nb\n</a>", result);

        d = this.xml.parse("<a>a\rb\r</a>");
        result = this.xml.serialize(d, false);
        Assert.assertEquals("Wrong newlines", "<a>a\nb\n</a>", result);
    }

    @Test
    public void testSerializationWithDoctype()
    {
        Document d = this.xml.parse("<?xml version='1.0' encoding='UTF-8' ?>"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
            + "<html><body>a</body></html>");
        String result = this.xml.serialize(d, true);
        Assert.assertEquals("Failed Doctype", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html><body>a</body></html>", result);
        result = this.xml.serialize(d, false);
        Assert.assertEquals("Failed Doctype", "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html><body>a</body></html>", result);
    }

    @Test
    public void testDoctypeSerialization()
    {
        Document d = this.xml.parse("<?xml version='1.0' encoding='UTF-8' ?>"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
            + "<html><body>a</body></html>");
        String result = this.xml.serialize(d.getDoctype(), true);
        Assert.assertEquals("Doctype alone shouldn't be serialized", "", result);
    }

    @Test
    public void testSerializeToSmallerCharset()
    {
        Document d = this.xml.parse("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a>\u0345</a>");
        String result = this.xml.serialize(d);
        Assert.assertFalse("Non-latin1 character shouldn't be present in the output", result.contains("\u0345"));
    }

    @Test
    public void testTransformDocument()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s = this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml' omit-xml-declaration='yes'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>");
        String result = this.xml.transform(d, s);
        Assert.assertEquals("<a/>", result);
    }

    @Test
    public void testTransformByteArray() throws UnsupportedEncodingException
    {
        byte[] d = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>".getBytes("UTF-8");
        byte[] s = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml' omit-xml-declaration='yes'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>").getBytes("UTF-8");
        String result = this.xml.transform(d, s);
        Assert.assertEquals("<a/>", result);
    }

    @Test
    public void testTransformString() throws UnsupportedEncodingException
    {
        String d = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>";
        String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml' omit-xml-declaration='yes'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>";
        String result = this.xml.transform(d, s);
        Assert.assertEquals("<a/>", result);
    }

    @Test
    public void testTransformWithNullInputs()
    {
        StreamSource d = new StreamSource(new StringReader("<a b='c'>d</a>"));
        StreamSource s = new StreamSource(new StringReader(
            "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'><xsl:output method='xml'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>"));
        String result = this.xml.transform(null, s);
        Assert.assertNull(null, result);
        result = this.xml.transform(d, null);
        Assert.assertNull(null, result);
        result = this.xml.transform((StreamSource) null, (StreamSource) null);
        Assert.assertNull(null, result);
    }

    @Test
    public void testTransformWithNullDocuments()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s = this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>");
        String result = this.xml.transform(null, s);
        Assert.assertNull(null, result);
        result = this.xml.transform(d, null);
        Assert.assertNull(null, result);
        result = this.xml.transform((Document) null, (Document) null);
        Assert.assertNull(null, result);
    }

    @Test
    public void testTransformWithNullStrings()
    {
        String d = "<a b='c'>d</a>";
        String s = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>";
        String result = this.xml.transform(null, s);
        Assert.assertNull(null, result);
        result = this.xml.transform(d, null);
        Assert.assertNull(null, result);
        result = this.xml.transform((String) null, (String) null);
        Assert.assertNull(null, result);
    }

    @Test
    public void testTransformWithNullByteArrays() throws UnsupportedEncodingException
    {
        byte[] d = "<a b='c'>d</a>".getBytes("UTF-8");
        byte[] s = ("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>")
            .getBytes();
        String result = this.xml.transform(null, s);
        Assert.assertNull(null, result);
        result = this.xml.transform(d, null);
        Assert.assertNull(null, result);
        result = this.xml.transform((byte[]) null, (byte[]) null);
        Assert.assertNull(null, result);
    }

    @Test
    public void testTransformWithInvalidSheet()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s = this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>");
        String result = this.xml.transform(s, d);
        Assert.assertEquals(null, result);
    }

    @Test
    public void testTransformToText()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s =
            this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='text'/><xsl:template match='node()'><xsl:value-of select='@*'/></xsl:template>"
            + "</xsl:stylesheet>");
        String result = this.xml.transform(d, s);
        Assert.assertEquals("c", result);
    }

    private Document createSimpleDocument()
    {
        Document d = this.xml.createDOMDocument();
        Element a = d.createElement("a");
        a.setTextContent("\u00E9");
        d.appendChild(a);
        return d;
    }
}
