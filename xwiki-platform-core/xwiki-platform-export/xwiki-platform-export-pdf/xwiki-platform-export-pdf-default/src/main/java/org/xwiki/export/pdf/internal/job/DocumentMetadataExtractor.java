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
package org.xwiki.export.pdf.internal.job;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ObjectPropertyReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Extracts metadata from a document that is being exported to PDF, in order to be displayed in the PDF header or
 * footer.
 * 
 * @version $Id$
 * @since 14.10.17
 * @since 15.5.3
 * @since 15.8RC1
 */
@Component(roles = DocumentMetadataExtractor.class)
@Singleton
public class DocumentMetadataExtractor
{
    static final String EXECUTION_CONTEXT_PROPERTY_METADATA = DocumentRenderer.class.getName() + ".metadata";

    static final String EXECUTION_CONTEXT_PROPERTY_SOURCE_DOCUMENT =
        DocumentRenderer.class.getName() + ".sourceDocument";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Execution execution;

    /**
     * Renders the specified metadata property to produce the metadata for the given document. The value of the metadata
     * property is normally a script that produces the metadata when evaluated. The script can use two special bindings:
     * <ul>
     * <li>{@code metadata} - a map that is used to store the metadata</li>
     * <li>{@code sourceDocument} - the document for which to retrieve the metadata.</li>
     * </ul>
     * 
     * @param sourceDocument the document for which to retrieve the metadata
     * @param metadataReference the reference of the property that holds the script to produce the metadata
     * @return the metadata for the given document, obtained from the specified property
     */
    public Map<String, String> getMetadata(XWikiDocument sourceDocument, ObjectPropertyReference metadataReference)
    {
        Map<String, String> metadata = new HashMap<>();
        ExecutionContext executionContext = this.execution.getContext();

        if (metadataReference != null && executionContext != null) {
            executionContext.removeProperty(EXECUTION_CONTEXT_PROPERTY_METADATA);
            executionContext.newProperty(EXECUTION_CONTEXT_PROPERTY_METADATA).inherited().nonNull().initial(metadata)
                .declare();

            executionContext.removeProperty(EXECUTION_CONTEXT_PROPERTY_SOURCE_DOCUMENT);
            XWikiContext xcontext = this.xcontextProvider.get();
            Document safeSourceDocument = new Document(sourceDocument, xcontext);
            executionContext.newProperty(EXECUTION_CONTEXT_PROPERTY_SOURCE_DOCUMENT).inherited().nonNull()
                .initial(safeSourceDocument).declare();

            try {
                XWikiDocument templateDocument =
                    xcontext.getWiki().getDocument(metadataReference.extractReference(EntityType.DOCUMENT), xcontext);
                BaseObject templateObject = templateDocument.getXObject(metadataReference.getParent());
                templateDocument.display(metadataReference.getName(), templateObject, xcontext);
            } catch (Exception e) {
                this.logger.warn("Failed to get the metadata for document [{}] from [{}]. Root cause is [{}].",
                    sourceDocument.getDocumentReference(), metadataReference, ExceptionUtils.getRootCauseMessage(e));
            } finally {
                executionContext.removeProperty(EXECUTION_CONTEXT_PROPERTY_METADATA);
                executionContext.removeProperty(EXECUTION_CONTEXT_PROPERTY_SOURCE_DOCUMENT);
            }
        }

        return metadata;
    }
}
