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
package org.xwiki.xml.script;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link org.xwiki.xml.script.XMLScriptService}.
 *
 * @version $Id$
 * @since 2.7RC1
 */
@ComponentTest
public class XMLScriptServiceTest
{
    @InjectMockComponents
    private XMLScriptService xml;

    @Test
    void testGetDomDocument()
    {
        // Nothing much that we can test here...
        assertNotNull(this.xml.createDOMDocument());
    }

    @Test
    void testParseString()
    {
        Document result = this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>");
        assertNotNull(result, "Failed to parse content");
        assertEquals("a", result.getDocumentElement().getLocalName(), "Incorrect root node");
    }

    @Test
    void testParseByteArray() throws UnsupportedEncodingException
    {
        Document result = this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>".getBytes("UTF-8"));
        assertNotNull(result, "Failed to parse content");
        assertEquals("a", result.getDocumentElement().getLocalName(), "Incorrect root node");
    }

    @Test
    void testParseInputStream() throws UnsupportedEncodingException
    {
        Document result = this.xml.parse(
            new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>".getBytes("UTF-8")));
        assertNotNull(result, "Failed to parse content");
        assertEquals("a", result.getDocumentElement().getLocalName(), "Incorrect root node");
    }

    @Test
    void testParseWithDifferentEncoding() throws UnsupportedEncodingException
    {
        Document result =
            this.xml.parse("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a>\u00E9</a>".getBytes("ISO-8859-1"));
        assertNotNull(result, "Failed to parse content");
        assertEquals("a", result.getDocumentElement().getLocalName(), "Incorrect root node");
        assertEquals("\u00E9", result.getDocumentElement().getTextContent(), "Incorrect content");
    }

    @Test
    void testParseWithWrongEncoding() throws UnsupportedEncodingException
    {
        Document result =
            this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>\u00E9</a>".getBytes("ISO-8859-1"));
        assertNull(result, "Content should be invalid with the specified encoding");
    }

    @Test
    void testParseWithoutXMLDeclaration()
    {
        Document result = this.xml.parse("<a>\u00E9</a>");
        assertNotNull(result, "Failed to parse content");
        assertEquals("a", result.getDocumentElement().getLocalName(), "Incorrect root node");
    }

    @Test
    void testParseInvalidDocument()
    {
        Document result = this.xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a></b>");
        assertNull(result, "Invalid content shouldn't be parsed");
    }

    @Test
    void testParseNull()
    {
        Document result = this.xml.parse((String) null);
        assertNull(result, "Null Document input shouldn't be parsed");
        result = this.xml.parse((byte[]) null);
        assertNull(result, "Null byte[] input shouldn't be parsed");
        result = this.xml.parse((ByteArrayInputStream) null);
        assertNull(result, "Null InputStream input shouldn't be parsed");
        result = this.xml.parse((LSInput) null);
        assertNull(result, "Null LSInput input shouldn't be parsed");
    }

    @Test
    void testParseAndSerialize()
    {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>\u00E9</a>";
        String result = this.xml.serialize(this.xml.parse(content));
        assertEquals(content, result, "Not identical content after parse + serialize");

        content = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<a>\u00E9</a>";
        result = this.xml.serialize(this.xml.parse(content));
        assertEquals(content, result, "Not identical content after parse + serialize");

        content = "<a>\u00E9</a>";
        result = this.xml.serialize(this.xml.parse(content), false);
        assertEquals(content, result, "Not identical content after parse + serialize");
    }

    @Test
    void testSerialize()
    {
        Document d = createSimpleDocument();
        String result = this.xml.serialize(d);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>\u00E9</a>", result, "Wrong serialization");
    }

    @Test
    void testSerializeDocumentElement()
    {
        Document d = createSimpleDocument();
        String result = this.xml.serialize(d.getDocumentElement());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>\u00E9</a>", result, "Wrong serialization");
    }

    @Test
    void testSerializeNode()
    {
        Document d = createSimpleDocument();
        Element b = d.createElement("b");
        b.setTextContent("c");
        d.getDocumentElement().appendChild(b);
        String result = this.xml.serialize(d.getDocumentElement().getElementsByTagName("b").item(0));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<b>c</b>", result, "Wrong serialization");
    }

    @Test
    void testSerializeNull()
    {
        assertEquals("", this.xml.serialize(null), "Wrong serialization for null document");
    }

    @Test
    void testSerializeWithoutXmlDeclaration()
    {
        Document d = createSimpleDocument();
        String result = this.xml.serialize(d.getDocumentElement(), false);
        assertEquals("<a>\u00E9</a>", result, "Wrong serialization");
    }

    @Test
    void testNewlineSerialization()
    {
        Document d = this.xml.parse("<a>a\nb\n</a>");
        String result = this.xml.serialize(d, false);
        assertEquals("<a>a\nb\n</a>", result, "Wrong newlines");

        d = this.xml.parse("<a>a\r\nb\r\n</a>");
        result = this.xml.serialize(d, false);
        assertEquals("<a>a\nb\n</a>", result, "Wrong newlines");

        d = this.xml.parse("<a>a\rb\r</a>");
        result = this.xml.serialize(d, false);
        assertEquals("<a>a\nb\n</a>", result, "Wrong newlines");
    }

    @Test
    void testSerializationWithDoctype()
    {
        Document d = this.xml.parse("<?xml version='1.0' encoding='UTF-8' ?>"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
            + "<html><body>a</body></html>");
        String result = this.xml.serialize(d, true);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html><body>a</body></html>", result, "Failed Doctype");
        result = this.xml.serialize(d, false);
        assertEquals("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html><body>a</body></html>", result, "Failed Doctype");
    }

    @Test
    void testDoctypeSerialization()
    {
        Document d = this.xml.parse("<?xml version='1.0' encoding='UTF-8' ?>"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
            + "<html><body>a</body></html>");
        String result = this.xml.serialize(d.getDoctype(), true);
        assertEquals("", result, "Doctype alone shouldn't be serialized");
    }

    @Test
    void testSerializeToSmallerCharset()
    {
        Document d = this.xml.parse("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a>\u0345</a>");
        String result = this.xml.serialize(d);
        assertFalse(result.contains("\u0345"), "Non-latin1 character shouldn't be present in the output");
    }

    @Test
    void testTransformDocument()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s = this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml' omit-xml-declaration='yes'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>");
        String result = this.xml.transform(d, s);
        assertEquals("<a/>", result);
    }

    @Test
    void testTransformByteArray() throws UnsupportedEncodingException
    {
        byte[] d = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>".getBytes("UTF-8");
        byte[] s = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml' omit-xml-declaration='yes'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>").getBytes("UTF-8");
        String result = this.xml.transform(d, s);
        assertEquals("<a/>", result);
    }

    @Test
    void testTransformString()
    {
        String d = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a b='c'>d</a>";
        String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml' omit-xml-declaration='yes'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>";
        String result = this.xml.transform(d, s);
        assertEquals("<a/>", result);
    }

    @Test
    void testTransformWithNullInputs()
    {
        StreamSource d = new StreamSource(new StringReader("<a b='c'>d</a>"));
        StreamSource s = new StreamSource(new StringReader(
            "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'><xsl:output method='xml'/>"
            + "<xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>"));
        String result = this.xml.transform(null, s);
        assertNull(null, result);
        result = this.xml.transform(d, null);
        assertNull(null, result);
        result = this.xml.transform((StreamSource) null, (StreamSource) null);
        assertNull(null, result);
    }

    @Test
    void testTransformWithNullDocuments()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s = this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>");
        String result = this.xml.transform(null, s);
        assertNull(null, result);
        result = this.xml.transform(d, null);
        assertNull(null, result);
        result = this.xml.transform((Document) null, (Document) null);
        assertNull(null, result);
    }

    @Test
    void testTransformWithNullStrings()
    {
        String d = "<a b='c'>d</a>";
        String s = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>";
        String result = this.xml.transform(null, s);
        assertNull(null, result);
        result = this.xml.transform(d, null);
        assertNull(null, result);
        result = this.xml.transform((String) null, (String) null);
        assertNull(null, result);
    }

    @Test
    void testTransformWithNullByteArrays() throws UnsupportedEncodingException
    {
        byte[] d = "<a b='c'>d</a>".getBytes("UTF-8");
        byte[] s = ("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>")
            .getBytes();
        String result = this.xml.transform(null, s);
        assertNull(null, result);
        result = this.xml.transform(d, null);
        assertNull(null, result);
        result = this.xml.transform((byte[]) null, (byte[]) null);
        assertNull(null, result);
    }

    @Test
    void testTransformWithInvalidSheet()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s = this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='xml'/><xsl:template match='node()'><xsl:copy/></xsl:template></xsl:stylesheet>");
        String result = this.xml.transform(s, d);
        assertEquals(null, result);
    }

    @Test
    void testTransformToText()
    {
        Document d = this.xml.parse("<a b='c'>d</a>");
        Document s =
            this.xml.parse("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
            "<xsl:output method='text'/><xsl:template match='node()'><xsl:value-of select='@*'/></xsl:template>"
            + "</xsl:stylesheet>");
        String result = this.xml.transform(d, s);
        assertEquals("c", result);
    }

    @Test
    void escape()
    {
        assertEquals("&#60;a b=&#39;c&#39;&#62;d&#60;/a&#62;", XMLScriptService.escape("<a b='c'>d</a>"));
    }

    @Test
    void escapeNull()
    {
        assertNull(XMLScriptService.escape(null));
    }

    @Test
    void escapeForAttributeValue()
    {
        assertEquals("&#60;a b=&#39;c&#39;&#62;d&#60;/a&#62;",
            XMLScriptService.escapeForAttributeValue("<a b='c'>d</a>"));
    }

    @Test
    void escapeForAttributeValueNull()
    {
        assertNull(XMLScriptService.escapeForAttributeValue(null));
    }

    @Test
    void escapeForElementContent()
    {
        assertEquals("&#60;a b='c'>d&#60;/a>", XMLScriptService.escapeForElementContent("<a b='c'>d</a>"));
    }

    @Test
    void escapeForElementContentNull()
    {
        assertNull(XMLScriptService.escapeForElementContent(null));
    }

    @Test
    void unescape()
    {
        assertEquals("<a b='c'>d</a>",
            XMLScriptService.unescape("&#60;a b='c'&#62;d&#60;/a&#62;"));
    }

    @Test
    void unescapeNull()
    {
        assertNull(XMLScriptService.unescape(null));
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
