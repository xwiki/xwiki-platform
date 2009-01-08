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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.internal.transformer.DocumentTransformer;
import org.xwiki.officeimporter.internal.transformer.HtmlToXWikiPresentationTransformer;
import org.xwiki.officeimporter.internal.transformer.HtmlToXWikiTwoZeroTransformer;
import org.xwiki.officeimporter.internal.transformer.OfficeToHtmlTransformer;

import com.artofsolving.jodconverter.DocumentFormat;

/**
 * Default implementation of the office importer component.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class DefaultOfficeImporter implements OfficeImporter, Composable, Initializable
{
    /**
     * File extensions corresponding to slide presentations.
     */
    private static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "odp");

    /**
     * The host address of the Open Office server.
     */
    private String openOfficeServerIp;

    /**
     * The port number of the the Open Office service
     */
    private int openOfficeServerPort;
    
    /**
     * Component manager used to lookup for other components.
     */
    private ComponentManager componentManager;
    
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
    private DocumentTransformer htmlToXWikiPresentationTransformer;

    /**
     * Transforms an XHTML document into XWiki 2.0 syntax.
     */
    private DocumentTransformer htmlToXWikiTwoZeroTransformer;
    
    /**
     * {@inheritDoc}
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        officeToHtmlTransformer = new OfficeToHtmlTransformer(openOfficeServerIp, openOfficeServerPort);
        htmlToXWikiTwoZeroTransformer = new HtmlToXWikiTwoZeroTransformer(componentManager);
        htmlToXWikiPresentationTransformer = new HtmlToXWikiPresentationTransformer(componentManager);
    }

    /**
     * {@inheritDoc}
     *  
     * Supports converting the Office document to HTML or XWiki Syntax 2.0.
     * 
     * @see OfficeImporter#importDocument(byte[], String, String, Map)
     */
    public void importDocument(byte[] fileContent, String fileName, String targetDocument,
        Map<String, String> options) throws OfficeImporterException
    {
        if (isValidRequest(targetDocument)) {
            options.put("targetDocument", targetDocument);
            OfficeImporterContext importerContext =
                new OfficeImporterContext(fileContent, fileName, targetDocument, options, docBridge);
            officeToHtmlTransformer.transform(importerContext);
            DocumentTransformer htmlToXWikiTransformer = null;
            boolean isPresentation = isPresentation(importerContext.getSourceFormat());
            if (isPresentation) {
                htmlToXWikiTransformer = htmlToXWikiPresentationTransformer;
            } else {
                htmlToXWikiTransformer = htmlToXWikiTwoZeroTransformer;
            }
            htmlToXWikiTransformer.transform(importerContext);
            importerContext.finalizeDocument(isPresentation);
        } else {
            throw new OfficeImporterException("Failed to import document [" + fileName + "] into page [" 
                + targetDocument + "]. Most probably the page already exists or you don't have edit rights on it.");
        }
    }

    /**
     * Checks if this request is valid. For a request to be valid, the requested target document
     * should not exist and the user should have enough privileges to create & edit that particular
     * page.
     * 
     * @param targetDocument The target document.
     */
    private boolean isValidRequest(String targetDocument)
    {
        boolean isValid = false;
        try {
            isValid = !docBridge.exists(targetDocument) && docBridge.isDocumentEditable(targetDocument);
        } catch (Exception ex) {
            // Some unexpected error since "exists" and "isDocumentEditable" should not throw exceptions.
            // Don't do anything but consider the request as invalid.
        }
        return isValid;
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
