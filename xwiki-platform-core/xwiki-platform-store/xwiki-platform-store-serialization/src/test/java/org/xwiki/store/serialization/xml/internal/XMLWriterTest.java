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
package org.xwiki.store.serialization.xml.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests XMLWriter
 *
 * @version $Id$
 * @since 3.0M2
 */
public class XMLWriterTest
{
    private static final String TEST_CONTENT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<root>\n"
      + " <doc>\n"
      + "  <obj>\n"
      + "   <prop>\n"
      + "   </prop>\n"
      + "  </obj>\n"
      + " </doc>\n"
      + "</root>";

    private static final String BASE64_INPUT =
        "This string will be converted to base64, it has to be long enough to see that the "
      + "lines will be broken at the right length.";

    private static final String BASE64_TEST =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<root>\n"
      + " <doc>\n"
      + "  <obj>\n"
      + "   <prop>\n"
      + "VGhpcyBzdHJpbmcgd2lsbCBiZSBjb252ZXJ0ZWQgdG8gYmFzZTY0LCBpdCBoYXMgdG8gYmUgbG9uZyBl\n"
      + "bm91Z2ggdG8gc2VlIHRoYXQgdGhlIGxpbmVzIHdpbGwgYmUgYnJva2VuIGF0IHRoZSByaWdodCBsZW5n\n"
      + "dGgu\n"
      + "   </prop>\n"
      + "  </obj>\n"
      + " </doc>\n"
      + "</root>";

    private static final String WRITE_STREAM_TEST =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<root>\n"
      + " <prop>Hello World!</prop>\n"
      + "</root>";

    private XMLWriter writer;

    /**
     * Make sure writeClose closes internediet nodes.
     */
    @Test
    public void testWriteClose() throws Exception
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputFormat of = new OutputFormat(" ", true, "UTF-8");

        this.writer = new XMLWriter(baos, of);
        this.writer.startDocument();

        this.writer.writeOpen(new DOMElement("root"));
        this.writer.writeOpen(new DOMElement("doc"));
        this.writer.writeOpen(new DOMElement("obj"));
        this.writer.writeOpen(new DOMElement("prop"));
        this.writer.writeClose(new DOMElement("root"));

        this.writer.endDocument();

        Assert.assertEquals("WriteClose didn't write the correct response.",
            TEST_CONTENT,
            new String(baos.toByteArray(), "UTF-8"));
    }

    @Test
    public void testBase64() throws Exception
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputFormat of = new OutputFormat(" ", true, "UTF-8");

        this.writer = new XMLWriter(baos, of);
        this.writer.startDocument();

        this.writer.writeOpen(new DOMElement("root"));
        this.writer.writeOpen(new DOMElement("doc"));
        this.writer.writeOpen(new DOMElement("obj"));
        this.writer.writeBase64(new DOMElement("prop"),
            new ByteArrayInputStream(BASE64_INPUT.getBytes("UTF-8")));
        this.writer.writeClose(new DOMElement("root"));

        this.writer.endDocument();

        Assert.assertEquals("Incorrect response from testBase64.",
            BASE64_TEST,
            new String(baos.toByteArray(), "UTF-8"));
    }

    @Test
    public void testWriteStream() throws Exception
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputFormat of = new OutputFormat(" ", true, "UTF-8");

        this.writer = new XMLWriter(baos, of);
        this.writer.startDocument();

        this.writer.writeOpen(new DOMElement("root"));
        this.writer.write(new DOMElement("prop"), new StringReader("Hello World!"));
        this.writer.writeClose(new DOMElement("root"));

        this.writer.endDocument();

        Assert.assertEquals("Incorrect response from testWriteStream.",
            WRITE_STREAM_TEST,
            new String(baos.toByteArray(), "UTF-8"));
    }
}
