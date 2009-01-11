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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.transformer.DocumentTransformer;

import com.artofsolving.jodconverter.DocumentFormat;

/**
 * Default implementation of the office importer component.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class DefaultOfficeImporter implements OfficeImporter
{
    /**
     * File extensions corresponding to slide presentations.
     */
    private static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "odp");

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
        boolean isPresentation = isPresentation(importerContext.getSourceFormat());
        if (isPresentation) {
            htmlTransformer = htmlToPresentationTransformer;
        } else {
            htmlTransformer = htmlToXWikiTransformer;
        }
        htmlTransformer.transform(importerContext);
        importerContext.finalizeDocument(isPresentation);
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

    /**
     * @param format the input document's format
     * @return true if the given format corresponds to a slide presentation.
     */
    private boolean isPresentation(DocumentFormat format)
    {
        return PRESENTATION_FORMAT_EXTENSIONS.contains(format.getFileExtension().toLowerCase());
    }
}
