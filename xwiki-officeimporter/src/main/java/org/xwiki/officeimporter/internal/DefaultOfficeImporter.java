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
package org.xwiki.officeimporter.internal;

import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.transformer.DocumentTransformer;

/**
 * Default implementation of the office importer component.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class DefaultOfficeImporter extends AbstractLogEnabled implements OfficeImporter
{
    /**
     * Document access bridge used to access wiki documents.
     */
    private DocumentAccessBridge docBridge;

    /**
     * Transformer responsible for converting office documents into (HTML + artifacts).
     */
    private DocumentTransformer officeToHtmlTransformer;

    /**
     * Transforms a resulting (XHTML + artifacts) into an XWiki presentation (via ZipExplorer).
     */
    private DocumentTransformer htmlToPresentationTransformer;

    /**
     * Transforms an XHTML document into XWiki 2.0 syntax.
     */
    private DocumentTransformer htmlToXWikiTransformer;
    
    /**
     * Transforms an XWiki 2.0 document into Xhtml 1.0 syntax.
     */
    private DocumentTransformer xwikiToXhtmlTransformer;
    
    /**
     * {@inheritDoc} Supports converting the Office document to HTML or XWiki Syntax 2.0.
     * 
     * @see OfficeImporter#importDocument(byte[], String, String, Map)
     */
    public void importDocument(byte[] fileContent, String fileName, String targetDocument, Map<String, String> options)
        throws OfficeImporterException
    {
        validateRequest(targetDocument);
        options.put("targetDocument", targetDocument);
        OfficeImporterContext importerContext =
            new OfficeImporterContext(fileContent, fileName, targetDocument, options, docBridge);
        officeToHtmlTransformer.transform(importerContext);
        DocumentTransformer htmlTransformer = null;
        if (importerContext.isPresentation()) {
            htmlTransformer = htmlToPresentationTransformer;
        } else {
            htmlTransformer = htmlToXWikiTransformer;
        }
        htmlTransformer.transform(importerContext);
        importerContext.finalizeDocument(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see OfficeImporter#importDocument(String, String, Map)
     */
    public String importDocument(String targetDocument, String attachmentName, Map<String, String> options)
        throws OfficeImporterException
    {
        validateRequest(targetDocument);
        options.put("targetDocument", targetDocument);
        byte[] attachmentContent = null;
        try {
            attachmentContent = docBridge.getAttachmentContent(targetDocument, attachmentName);
        } catch (Exception ex) {
            throw new OfficeImporterException("Error while accessing " + targetDocument + ":" + attachmentName, ex);
        }
        OfficeImporterContext importerContext =
            new OfficeImporterContext(attachmentContent, attachmentName, targetDocument, options, docBridge);
        officeToHtmlTransformer.transform(importerContext);
        DocumentTransformer htmlTransformer = null;
        if (importerContext.isPresentation()) {
            htmlTransformer = htmlToPresentationTransformer;
        } else {
            htmlTransformer = htmlToXWikiTransformer;
        }
        htmlTransformer.transform(importerContext);
        xwikiToXhtmlTransformer.transform(importerContext);
        importerContext.finalizeDocument(true);            
        return importerContext.getEncodedContent();
    }

    /**
     * Checks if this request is valid. For a request to be valid, the requested target document should not exist and
     * the user should have enough privileges to create & edit that particular page.
     * 
     * @param targetDocument the target document.
     * @throws OfficeImporterException if the request is invalid.
     */
    private void validateRequest(String targetDocument) throws OfficeImporterException
    {
        boolean exists = true;
        try {
            exists = docBridge.exists(targetDocument);
        } catch (Exception ex) {
            throw new OfficeImporterException("Internal error.", ex);
        }
        if (exists) {
            throw new OfficeImporterException("The target document " + targetDocument + " already exists.");
        } else if (!docBridge.isDocumentEditable(targetDocument)) {
            throw new OfficeImporterException("Inadequate privileges.");
        }
    }
}
