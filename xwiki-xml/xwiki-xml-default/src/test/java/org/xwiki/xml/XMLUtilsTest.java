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
package org.xwiki.xml;

import org.junit.*;

/**
 * Unit tests for {@link org.xwiki.xml.XMLUtils}.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class XMLUtilsTest
{
    @Test
    public void testEscapeXMLComment()
    {
        Assert.assertEquals("-\\- ", XMLUtils.escapeXMLComment("-- "));
        Assert.assertEquals("-\\", XMLUtils.escapeXMLComment("-"));
        Assert.assertEquals("-\\-\\-\\", XMLUtils.escapeXMLComment("---"));
        Assert.assertEquals("- ", XMLUtils.escapeXMLComment("- "));
    }

    @Test
    public void testUnescapeXMLComment()
    {
        Assert.assertEquals("", XMLUtils.unescapeXMLComment("\\"));
        Assert.assertEquals("\\", XMLUtils.unescapeXMLComment("\\\\"));
        Assert.assertEquals("--", XMLUtils.unescapeXMLComment("\\-\\-"));
        Assert.assertEquals("--", XMLUtils.unescapeXMLComment("\\-\\-\\"));
    }

    @Test
    public void testEscape()
    {
        String escapedText = XMLUtils.escape("a < a' && a' < a\" => a < a\"");

        Assert.assertFalse("Failed to escape <", escapedText.contains("<"));
        Assert.assertFalse("Failed to escape >", escapedText.contains(">"));
        Assert.assertFalse("Failed to escape '", escapedText.contains("'"));
        Assert.assertFalse("Failed to escape \"", escapedText.contains("\""));
        Assert.assertFalse("Failed to escape &", escapedText.contains("&&"));
    }

    @Test
    public void testEscapeApos()
    {
        Assert.assertFalse("' wrongly escaped to non-HTML &apos;", XMLUtils.escape("'").equals("&apos;"));
    }

    @Test
    public void testEscapeEmptyString()
    {
        Assert.assertEquals("\"\" should be \"\"", "", XMLUtils.escape(""));
    }

    @Test
    public void testEscapeWithNull()
    {
        Assert.assertNull("null should be null", XMLUtils.escape(null));
    }

    @Test
    public void testEscapeNonAscii()
    {
        Assert.assertTrue("Non-ASCII characters were escaped", XMLUtils.escape("\u0123").equals("\u0123"));
    }

    @Test
    public void testEscapeAttributeValue()
    {

        String escapedText = XMLUtils.escapeAttributeValue("a < a' && a' < a\" => a < a\"");

        Assert.assertFalse("Failed to escape <", escapedText.contains("<"));
        Assert.assertFalse("Failed to escape >", escapedText.contains(">"));
        Assert.assertFalse("Failed to escape '", escapedText.contains("'"));
        Assert.assertFalse("Failed to escape \"", escapedText.contains("\""));
        Assert.assertFalse("Failed to escape &", escapedText.contains("&&"));
    }

    @Test
    public void testEscapeAttributeValueApos()
    {
        Assert.assertFalse("' wrongly escaped to non-HTML &apos;", XMLUtils.escapeAttributeValue("'")
            .equals("&apos;"));
    }

    @Test
    public void testEscapeFAttributeValueEmptyString()
    {
        Assert.assertEquals("\"\" should be \"\"", "", XMLUtils.escapeAttributeValue(""));
    }

    @Test
    public void testEscapeFAttributeValueWithNull()
    {
        Assert.assertNull("null should be null", XMLUtils.escapeAttributeValue(null));
    }

    @Test
    public void testEscapeAttributeValueNonAscii()
    {
        Assert.assertTrue("Non-ASCII characters were escaped", XMLUtils.escapeAttributeValue("\u0123")
            .equals("\u0123"));
    }

    @Test
    public void testEscapeElementContent()
    {

        String escapedText = XMLUtils.escapeElementContent("a < a' && a' < a\" => a < a\"");

        Assert.assertFalse("Failed to escape <", escapedText.contains("<"));
        Assert.assertFalse("Failed to escape >", escapedText.contains(">"));
        Assert.assertTrue("Wrongfully escaped '", escapedText.contains("'"));
        Assert.assertTrue("Wrongfully escaped \"", escapedText.contains("\""));
        Assert.assertFalse("Failed to escape &", escapedText.contains("&&"));
    }

    @Test
    public void testEscapeElementContentEmptyString()
    {
        Assert.assertEquals("\"\" should be \"\"", "", XMLUtils.escapeElementContent(""));
    }

    @Test
    public void testEscapeElementContentWithNull()
    {
        Assert.assertNull("null should be null", XMLUtils.escapeElementContent(null));
    }

    @Test
    public void testEscapeElementContentNonAscii()
    {
        Assert.assertTrue("Non-ASCII characters were escaped", XMLUtils.escapeElementContent("\u0123")
            .equals("\u0123"));
    }

    @Test
    public void testUnescape()
    {
        Assert.assertEquals("Failed to unescaped named entities", "&'\"<>",
            XMLUtils.unescape("&amp;&apos;&quot;&lt;&gt;"));
        Assert.assertEquals("Failed to unescaped decimal entities", "&'\"<>",
            XMLUtils.unescape("&#38;&#39;&#34;&#60;&#62;"));
        Assert.assertEquals("Failed to unescaped decimal entities with leading zeros", "&'\"<>",
            XMLUtils.unescape("&#038;&#0039;&#00034;&#000060;&#0000062;"));
        Assert.assertEquals("Failed to unescaped hexadecimal entities", "&'\"<<>>",
            XMLUtils.unescape("&#x26;&#x27;&#x22;&#x3c;&#x3C;&#x3e;&#x3E;"));
        Assert.assertEquals("Failed to unescaped hexadecimal entities with leading zeros", "&'\"<<>>",
            XMLUtils.unescape("&#x026;&#x0027;&#x00022;&#x00003c;&#x0003C;&#x003e;&#x03E;"));
    }

    @Test
    public void testUnescapeEmptyString()
    {
        Assert.assertEquals("\"\" should be \"\"", "", XMLUtils.unescape(""));
    }

    @Test
    public void testUnescapeWithNull()
    {
        Assert.assertNull("null should be null", XMLUtils.unescape(null));
    }

    @Test
    public void testUnescapeOtherEscapes()
    {
        Assert.assertEquals("Extra named entities were unescaped", "&deg;", XMLUtils.unescape("&deg;"));
        Assert.assertEquals("Extra decimal entities were unescaped", "&#65;", XMLUtils.unescape("&#65;"));
        Assert.assertEquals("Extra hexadecimal entities were unescaped", "&#x5;", XMLUtils.unescape("&#x5;"));
    }
}
