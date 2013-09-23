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
package com.xpn.xwiki.tool.doc;

import java.io.File;
import java.io.FileReader;

import org.apache.commons.io.IOUtils;
import org.junit.*;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Tests for {@link AbstractDocumentMojo}.
 * 
 * @version $Id$
 */
public class DocumentUpdateMojoTest
{
    /**
     * Test that a document loaded in memory from XML by the mojo then written back to XML does not lose any
     * information/is not affected by the process
     */
    @Test
    public void testXMLDocumentLoading() throws Exception
    {
        AttachMojo mojo = new AttachMojo();

        File resourceFile = new File(this.getClass().getResource("/SampleWikiXMLDocument.input").toURI());

        String inputContent = IOUtils.toString(new FileReader(resourceFile));
        Assert.assertTrue(inputContent.contains("<class>"));

        XWikiDocument doc = mojo.loadFromXML(resourceFile);
        Assert.assertEquals(doc.getName(), "Install");

        File outputFile = File.createTempFile("output", "xml");
        mojo.writeToXML(doc, outputFile);

        String outputContent = IOUtils.toString(new FileReader(outputFile));

        // Check that we did not lose the class definition during the loading from XML/writing to XML process.
        Assert.assertTrue(outputContent.contains("<class>"));
    }
}
