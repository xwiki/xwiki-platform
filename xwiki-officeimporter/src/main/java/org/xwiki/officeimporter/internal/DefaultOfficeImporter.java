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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.OfficeImporterResult;
import org.xwiki.officeimporter.transformer.DocumentTransformer;
import org.xwiki.rendering.parser.Syntax;

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
     * {@inheritDoc}
     */
    public OfficeImporterResult doImport(byte[] fileContent, String fileName, String targetDocument,
        Syntax targetSyntax, Map<String, String> options) throws OfficeImporterException
    {
        options.put("targetDocument", targetDocument);
        OfficeImporterContext context =
            new OfficeImporterContext(fileContent, fileName, targetDocument, options, docBridge);
        officeToHtmlTransformer.transform(context);
        DocumentTransformer htmlTransformer = null;
        if (context.isPresentation()) {
            htmlTransformer = htmlToPresentationTransformer;
        } else {
            htmlTransformer = htmlToXWikiTransformer;
        }
        htmlTransformer.transform(context);
        if (targetSyntax.equals(XWIKI_20)) {
            return buildResult(context);
        } else if (targetSyntax.equals(XHTML_10)) {
            xwikiToXhtmlTransformer.transform(context);
            return buildResult(context);
        } else {
            throw new OfficeImporterException("Target syntax " + targetSyntax.toIdString() + " is not supported.");
        }        
    }

    /**
     * Builds an {@link OfficeImporterResult} using information extracted from an {@link OfficeImporterContext} object.
     * 
     * @param context the {@link OfficeImporterContext}.
     * @return the {@link OfficeImporterResult} object containing the results of the import operation.
     * @throws OfficeImporterException If an error occurs while encoding the office importer results.
     */
    private OfficeImporterResult buildResult(OfficeImporterContext context) throws OfficeImporterException
    {
        Map<String, byte[]> resultArtifacts = null;
        if (context.isPresentation()) {
            resultArtifacts = new HashMap<String, byte[]>();
            resultArtifacts.put(OfficeImporterContext.PRESENTATION_ARCHIVE_NAME, context.getArtifacts().get(
                OfficeImporterContext.PRESENTATION_ARCHIVE_NAME));
        } else {
            resultArtifacts = context.getArtifacts();
        }
        OfficeImporterResult result = new OfficeImporterResult(context.getContent(), XWIKI_20, resultArtifacts);
        return result;
    }    
}
