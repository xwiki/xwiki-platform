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
package com.xpn.xwiki.internal.xml;

import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link DOMXMLWriter}.
 * 
 * @version $Id$
 */
public class DOMXMLWriterTest
{
    /**
     * Before 3.0M2 there was a bug where write and writeOpen were reversed.
     * 
     * @throws IOException
     * @see XWIKI-5937
     */
    @Test
    public void writeVersusWriteOpen() throws IOException
    {
        Document doc = new DOMDocument();
        DOMXMLWriter writer = new DOMXMLWriter(doc);
        DOMElement e = new DOMElement("a");
        writer.writeOpen(e);
        writer.write(new DOMElement("b"));
        writer.writeClose(e);
        writer.close();
        Assert.assertNotNull(doc.getRootElement().element("b"));

        doc = new DOMDocument();
        writer = new DOMXMLWriter(doc);
        e = new DOMElement("c");
        writer.write(e);
        writer.write(new DOMElement("d"));
        writer.close();
        Assert.assertNull(doc.getRootElement().element("d"));
    }
}
