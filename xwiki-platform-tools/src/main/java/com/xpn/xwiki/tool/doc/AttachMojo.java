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
import java.io.FileInputStream;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Attach a file to a XWiki document
 * 
 * @version $Id: $
 * @goal attach
 */
public class AttachMojo extends AbstractDocumentMojo
{
    /**
     * The attachment author
     * 
     * @parameter expression="XWiki.Admin"
     */
    private String author;

    /**
     * The file to attach
     * 
     * @parameter
     * @required
     */
    private File file;

    /**
     * Document to attach to
     * 
     * @parameter
     * @required
     */
    private File document;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try {
            XWikiDocument doc = loadFromXML(document);
            
            // Create a XWiki attachment from the file
            XWikiAttachment attachment = createAttachment(file, author);

            // Add the attachment to the document attachments list
            List attachlist = doc.getAttachmentList();
            attachlist.add(attachment);
            doc.setAttachmentList(attachlist);

            // output the file
            File outputFile = this.getOutputFileForDocument(document);
            writeToXML(doc, outputFile);

        } catch (Exception e) {
            throw new MojoExecutionException("Error while attaching the file", e);
        }
    }

    /**
     * Create a XWikiAttachment from a File
     * 
     * @param file the file to create the attachment from
     * @return the attachment
     * @throws MojoExecutionException
     */
    private XWikiAttachment createAttachment(File file, String author) throws MojoExecutionException
    {
        try {
            // Create an empty attachment
            XWikiAttachment attachment = new XWikiAttachment();

            // Read the file data. I don't know if it's the proper way of doing this.
            // Could possibly not handle huge files
            byte[] data = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(data);
            fis.close();

            // Feed the attachment
            attachment.setContent(data);
            attachment.setAuthor(author);
            attachment.setFilename(file.getName());

            return attachment;
        } catch (Exception e) {
            throw new MojoExecutionException("Error while creating attachment for file ["
                + file.getName() + "]", e);
        }
    }
}
