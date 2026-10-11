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
import java.nio.charset.StandardCharsets;

import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests XMLWriter
 *
 * @version $Id$
 * @since 3.0M2
 */
class XMLWriterTest
{
    private static final String TEST_CONTENT = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
         <doc>
          <obj>
           <prop>
           </prop>
          </obj>
         </doc>
        </root>""";

    private static final String BASE64_INPUT =
        "This string will be converted to base64, it has to be long enough to see that the "
      + "lines will be broken at the right length.";

    private static final String BASE64_TEST = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
         <doc>
          <obj>
           <prop>
        VGhpcyBzdHJpbmcgd2lsbCBiZSBjb252ZXJ0ZWQgdG8gYmFzZTY0LCBpdCBoYXMgdG8gYmUgbG9uZyBl
        bm91Z2ggdG8gc2VlIHRoYXQgdGhlIGxpbmVzIHdpbGwgYmUgYnJva2VuIGF0IHRoZSByaWdodCBsZW5n
        dGgu
           </prop>
          </obj>
         </doc>
        </root>""";

    private static final String WRITE_STREAM_TEST = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
         <prop>Hello World!</prop>
        </root>""";

    /**
     * Make sure writeClose closes internediet nodes.
     */
    @Test
    void writeClose() throws Exception
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputFormat of = new OutputFormat(" ", true, "UTF-8");

        XMLWriter writer = new XMLWriter(baos, of);
        writer.startDocument();

        writer.writeOpen(new DOMElement("root"));
        writer.writeOpen(new DOMElement("doc"));
        writer.writeOpen(new DOMElement("obj"));
        writer.writeOpen(new DOMElement("prop"));
        writer.writeClose(new DOMElement("root"));

        writer.endDocument();

        assertEquals(TEST_CONTENT, baos.toString(StandardCharsets.UTF_8),
            "WriteClose didn't write the correct response.");
    }

    @Test
    void base64() throws Exception
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputFormat of = new OutputFormat(" ", true, "UTF-8");

        XMLWriter writer = new XMLWriter(baos, of);
        writer.startDocument();

        writer.writeOpen(new DOMElement("root"));
        writer.writeOpen(new DOMElement("doc"));
        writer.writeOpen(new DOMElement("obj"));
        writer.writeBase64(new DOMElement("prop"),
            new ByteArrayInputStream(BASE64_INPUT.getBytes(StandardCharsets.UTF_8)));
        writer.writeClose(new DOMElement("root"));

        writer.endDocument();

        assertEquals(BASE64_TEST, baos.toString(StandardCharsets.UTF_8),
            "Incorrect response from testBase64.");
    }

    @Test
    void writeStream() throws Exception
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputFormat of = new OutputFormat(" ", true, "UTF-8");

        XMLWriter writer = new XMLWriter(baos, of);
        writer.startDocument();

        writer.writeOpen(new DOMElement("root"));
        writer.write(new DOMElement("prop"), new StringReader("Hello World!"));
        writer.writeClose(new DOMElement("root"));

        writer.endDocument();

        assertEquals(WRITE_STREAM_TEST, baos.toString(StandardCharsets.UTF_8),
            "Incorrect response from testWriteStream.");
    }
}
