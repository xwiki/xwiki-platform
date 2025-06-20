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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Attach a file to a XWiki document.
 * 
 * @version $Id$
 */
@Mojo(name = "attach")
public class AttachMojo extends AbstractDocumentMojo
{
    /**
     * The attachment author.
     */
    @Parameter(defaultValue = "XWiki.Admin")
    private String author;

    /**
     * The file to attach.
     */
    @Parameter
    private File file;

    /**
     * The files to attach.
     */
    @Parameter
    private File[] files;

    /**
     * Default constructor.
     *
     * @throws MojoExecutionException when there is an error initializing the context
     */
    public AttachMojo() throws MojoExecutionException
    {
        super();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try {
            XWikiDocument doc = loadFromXML(this.sourceDocument);
            List<XWikiAttachment> attachments = doc.getAttachmentList();

            // Create an XWiki attachment from each of the specified files.
            if (this.file != null) {
                attachments.add(createAttachment(this.file, this.author));
            }
            if (this.files != null) {
                for (File inputFile : this.files) {
                    attachments.add(createAttachment(inputFile, this.author));
                }
            }

            // Update the list of attachments.
            doc.setAttachmentList(attachments);

            // output the file
            File outputFile =
                new File(getSpaceDirectory(this.outputDirectory, this.sourceDocument), this.sourceDocument.getName());
            writeToXML(doc, outputFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while attaching files on document ["
                + this.sourceDocument.getParentFile().getName() + "." + this.sourceDocument.getName() + "]", e);
        }
    }

    /**
     * Create a XWikiAttachment from a File.
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

            // Feed the attachment
            try (FileInputStream fis = new FileInputStream(file)) {
                attachment.setContent(fis);
            }

            attachment.setAuthor(author);
            attachment.setFilename(file.getName());
            attachment.resetMimeType(null);

            return attachment;
        } catch (Exception e) {
            throw new MojoExecutionException("Error while creating attachment for file [" + file.getName() + "]", e);
        }
    }
}
