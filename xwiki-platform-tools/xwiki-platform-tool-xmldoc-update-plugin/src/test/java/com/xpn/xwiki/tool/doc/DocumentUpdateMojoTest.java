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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xwiki.component.util.ReflectionUtils;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.*;

/**
 * Tests for {@link AbstractDocumentMojo}.
 * 
 * @version $Id$
 */
public class DocumentUpdateMojoTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Test that a document loaded in memory from XML by the mojo then written back to XML does not lose any
     * information/is not affected by the process
     */
    @Test
    public void testXMLDocumentLoading() throws Exception
    {
        AttachMojo mojo = new AttachMojo();

        File resourceFile = new File(this.getClass().getResource("/Test/SampleWikiXMLDocument.input").toURI());

        String inputContent = IOUtils.toString(new FileReader(resourceFile));
        assertTrue(inputContent.contains("<class>"));

        XWikiDocument doc = mojo.loadFromXML(resourceFile);
        assertEquals(doc.getName(), "Install");

        File outputFile = File.createTempFile("output", "xml");
        mojo.writeToXML(doc, outputFile);

        String outputContent = IOUtils.toString(new FileReader(outputFile));

        // Check that we did not lose the class definition during the loading from XML/writing to XML process.
        assertTrue(outputContent.contains("<class>"));
    }

    @Test
    public void execute() throws Exception
    {
        AttachMojo mojo = new AttachMojo();

        set(mojo, "author", "XWiki.mflorea");
        set(mojo, "file", new File(this.getClass().getResource("/fileToAttach.txt").toURI()));
        set(mojo, "files", new File[] {new File(this.getClass().getResource("/fileToAttach.js").toURI())});
        set(mojo, "sourceDocument", new File(this.getClass().getResource("/Test/SampleWikiXMLDocument.input").toURI()));
        set(mojo, "outputDirectory", this.tempFolder.getRoot());

        mojo.execute();

        File outputFile = new File(tempFolder.getRoot(), "Test/SampleWikiXMLDocument.input");
        String outputContent = IOUtils.toString(new FileReader(outputFile));
        assertTrue(outputContent.contains("<attachment>\n    <filename>fileToAttach.txt</filename>"));
        assertTrue(outputContent.contains("<attachment>\n    <filename>fileToAttach.js</filename>"));
        assertTrue(outputContent.contains("<attachment>\n    <filename>fileToAttach.txt</filename>\n    "
            + "<mimetype>text/plain</mimetype>"));
    }

    private void set(AttachMojo mojo, String fieldName, Object value) throws Exception
    {
        ReflectionUtils.setFieldValue(mojo, fieldName, value);
    }
}
