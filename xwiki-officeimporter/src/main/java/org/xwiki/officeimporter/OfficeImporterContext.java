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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;

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
     * Default document syntax id.
     */
    public static final String DEFAULT_SYNTAX_ID = new Syntax(SyntaxType.XWIKI, "1.0").toIdString();
    
    /**
     * Default encoding for office imported documents.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    
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
     * SyntaxId of the target wiki page.
     */
    private String syntaxId;

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
     * Indicates if the document wrapped in this context has been finalized.
     */
    private boolean finalized;

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
                this.sourceFormat = formatRegistry.getFormatByFileExtension(sourceFileName.substring(dot + 1));
            }
            if (sourceFormat == null) {
                throw new OfficeImporterException("Unable to determine input file format.");
            }
        }
        this.targetDocument = targetDocument;
        this.options = options;
        this.docBridge = bridge;
        this.syntaxId = DEFAULT_SYNTAX_ID;
        this.finalized = false;
    }

    /**
     * This method should be invoked when all the transformations are done. A single {@link OfficeImporterContext} can
     * be finalized only once. After that, calling this method has no effect.
     * 
     * @param isPresentation If true, skips the packaging of artifacts as attachments.
     */
    public void finalizeDocument(boolean isPresentation) throws OfficeImporterException
    {
        try {
            if (!finalized) {
                docBridge.setDocumentContent(targetDocument, bufferedContent, "Created by office importer", false);
                if (!DEFAULT_SYNTAX_ID.equals(syntaxId)) {
                    docBridge.setDocumentSyntaxId(targetDocument, syntaxId);
                }
                if (!isPresentation) {
                    for (String artifactName : artifacts.keySet()) {
                        // Filter out the html output.
                        if (!artifactName.equals("output.html")) {
                            docBridge.setAttachmentContent(targetDocument, artifactName, artifacts.get(artifactName));
                        }
                    }
                } else {
                    docBridge.setAttachmentContent(targetDocument, PRESENTATION_ARCHIVE_NAME, artifacts
                        .get(PRESENTATION_ARCHIVE_NAME));
                }
                finalized = true;
            }
        } catch (Exception ex) {
            throw new OfficeImporterException("Internal error while finalizing document.", ex);
        }
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
     * @param content The target document content to be set.
     */
    public void setTargetDocumentContent(String content)
    {
        this.bufferedContent = content;
    }

    /**
     * Updates the syntaxId of the target document.
     * 
     * @param syntaxId Syntax ID to be set.
     */
    public void setTargetDocumentSyntaxId(String syntaxId)
    {
        this.syntaxId = syntaxId;
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
     * @return The target document content encoded with the default encoding for this wiki.
     * @throws OfficeImporterException If the default encoding is not supported.
     */
    public String getEncodedContent() throws OfficeImporterException
    {
        try {
            return new String(bufferedContent.getBytes(), DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            throw new OfficeImporterException("Inernal error while encoding document content.", ex);
        }
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
