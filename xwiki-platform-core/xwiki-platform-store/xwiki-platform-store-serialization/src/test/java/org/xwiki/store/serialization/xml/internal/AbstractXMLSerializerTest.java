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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to make sure AbstractXMLSerializer faithfuly parses and serializes content.
 *
 * @version $Id$
 * @since 3.0M2
 */
class AbstractXMLSerializerTest
{
    private static final String TEST_CONTENT = """
        <?xml version="1.0" encoding="UTF-8"?>

        <root>
         <doc>
          <name>docName</name>
          <obj>
           <name>objName</name>
           <content>objContent</content>
          </obj>
          <content>docContent</content>
         </doc>
        </root>""";

    @Test
    void parseAndSerialize() throws Exception
    {
        AbstractXMLSerializer<Element, Element> serializer = new AbstractXMLSerializer<Element, Element>()
        {
            @Override public Element parse(Element xmlElement)
            {
                return xmlElement;
            }

            @Override public void serialize(Element object, XMLWriter writeTo) throws IOException
            {
                writeTo.write(object);
            }
        };
        final ByteArrayInputStream bais = new ByteArrayInputStream(TEST_CONTENT.getBytes(StandardCharsets.US_ASCII));
        final Element element = serializer.parse(bais);
        bais.close();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(serializer.serialize(element), baos);
        final String test = baos.toString(StandardCharsets.US_ASCII);
        assertEquals(TEST_CONTENT, test, "Parsing and serializing yields a different output.");
    }
}
