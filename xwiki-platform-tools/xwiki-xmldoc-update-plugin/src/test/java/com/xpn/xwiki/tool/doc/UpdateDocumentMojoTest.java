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
import java.io.IOException;
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;

import com.xpn.xwiki.doc.XWikiDocument;

import junit.framework.TestCase;

/**
 * Test for {@link AbstractDocumentMojo}
 * 
 * @version $Id: $
 */
public class UpdateDocumentMojoTest extends TestCase
{

    /**
     * Test that a document loaded in memory from XML by the mojo then written back to XML does not lose any
     * information/is not affected by the process
     */
    public void testXMLDocumentLoading()
    {
        AttachMojo mojo = new AttachMojo();
        URL resURL = this.getClass().getResource("/SampleWikiXMLDocument.input");
        try {
            File resourceFile = new File(resURL.getPath());
            XWikiDocument doc = mojo.loadFromXML(resourceFile);
            FileReader fr = new FileReader(resourceFile);
            char[] bytes = new char[(int) resourceFile.length()];
            fr.read(bytes);
            String inputContent = new String(bytes);

            assertTrue(inputContent.contains("<class>"));

            assertEquals(doc.getName(), "Install");

            File outputFile = File.createTempFile("output", "xml");
            mojo.writeToXML(doc, outputFile);

            fr = new FileReader(outputFile);
            bytes = new char[(int) outputFile.length()];
            fr.read(bytes);
            String outputContent = new String(bytes);

            // Test that prove class definitions are lost during the process of loading/writing back the XML document.
            // It currently fail, see http://jira.xwiki.org/jira/browse/XTXMLDOC-6
            assertTrue(outputContent.contains("<class>"));

            // check there is no diff bewteen input and output file, as no transformation
            // has been applied.
            assertEquals(inputContent, outputContent);

        } catch (MojoExecutionException e) {
            fail();
        } catch (IOException e) {
            fail("Could not load wiki xml document resource file.");
        }

    }

}
