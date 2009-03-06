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
package org.xwiki.officeimporter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jodconverter.DefaultDocumentFormatRegistry;
import net.sf.jodconverter.DocumentFormat;
import net.sf.jodconverter.DocumentFormatRegistry;

import org.xwiki.bridge.DocumentAccessBridge;


/**
 * Contains all the context information for a particular transformation. While an office document is being transformed
 * into a wiki page, this context will act as the storage for that transformation.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OfficeImporterContext
{
    /**
     * File extensions corresponding to slide presentations.
     */
    private static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "odp");

    /**
     * Name of the presentation archive.
     */
    public static final String PRESENTATION_ARCHIVE_NAME = "presentation.zip";

    /**
     * Binary bufferedContent of the original office document.
     */
    private byte[] sourceData;

    /**
     * Name of the office document.
     */
    private String sourceFileName;

    /**
     * Format of the original document, determined by the extension of source file name.
     */
    private DocumentFormat sourceFormat;

    /**
     * Document access bridge used to access wiki documents.
     */
    private DocumentAccessBridge docBridge;

    /**
     * Name of the target document.
     */
    private String targetDocument;

    /**
     * Content of the wiki page. This can be html, xhtml, xwiki 2.0 depending on the current phase of transformation.
     */
    private String bufferedContent;

    /**
     * Collection of all artifacts extracted during the transformations.
     */
    private Map<String, byte[]> artifacts = new HashMap<String, byte[]>();

    /**
     * Additional parameters for the import operation.
     */
    private Map<String, String> options;

    /**
     * The default constructor.
     * 
     * @param input Binary sourceData of the office document to be transformed.
     * @param sourceFileName Name of the orginal office document.
     * @param targetDocument The document where the final result will be stored.
     * @throws OfficeImporterException If the {@link OfficeImporterContext} cannot be initialized.
     */
    public OfficeImporterContext(byte[] input, String sourceFileName, String targetDocument,
        Map<String, String> options, DocumentAccessBridge bridge) throws OfficeImporterException
    {
        this.sourceData = input;
        this.sourceFileName = sourceFileName;
        // Determine the document format.
        {
            DocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
            int dot = sourceFileName.lastIndexOf('.');
            if (dot != -1) {
                this.sourceFormat = formatRegistry.getFormatByExtension(sourceFileName.substring(dot + 1));
            }
            if (sourceFormat == null) {
                throw new OfficeImporterException("Unable to determine input file format.");
            }
        }
        this.targetDocument = targetDocument;
        this.options = options;
        this.docBridge = bridge;
    }

    /**
     * Adds an artifact collected while performing the transformation.
     * 
     * @param name Name of the artifact.
     * @param data Artifact contents.
     */
    public void addArtifact(String name, byte[] data)
    {
        artifacts.put(name, data);
    }

    /**
     * @param content the content.
     */
    public void setContent(String content)
    {
        this.bufferedContent = content;
    }

    /**
     * @return current result of the import operation.
     */
    public String getContent()
    {
        return this.bufferedContent;
    }
    
    /**
     * @return The binary sourceData of the original office document.
     */
    public byte[] getSourceData()
    {
        return sourceData;
    }

    /**
     * @return the sourceFileName
     */
    public String getSourceFileName()
    {
        return sourceFileName;
    }

    /**
     * @return the sourceDocumentFormat
     */
    public DocumentFormat getSourceFormat()
    {
        return sourceFormat;
    }

    /**
     * @return The name of the target xwiki document.
     */
    public String getTargetDocumentName()
    {
        return targetDocument;
    }

    /**
     * @return true if the source format represents a presentation document type.
     */
    public boolean isPresentation()
    {
        return PRESENTATION_FORMAT_EXTENSIONS.contains(sourceFormat.getExtension().toLowerCase());
    }

    /**
     * @param fileName Name of the attachment.
     * @return The URL of an attachment for the target document with the given file name.
     */
    public String getAttachmentURL(String fileName)
    {
        try {
            return docBridge.getAttachmentURL(targetDocument, fileName);
        } catch (Exception ex) {
            // Do nothing
        }
        return null;
    }

    /**
     * @return The current user's name.
     */
    public String getCurrentUser()
    {
        return docBridge.getCurrentUser();
    }

    /**
     * @return The artifacts
     */
    public Map<String, byte[]> getArtifacts()
    {
        return artifacts;
    }

    /**
     * @return The options map.
     */
    public Map<String, String> getOptions()
    {
        return options;
    }
}
