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
package org.xwiki.display.internal;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.AsyncProperties;
import org.xwiki.rendering.async.internal.block.AbstractBlockAsyncRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of DocumentContentAsyncRenderer.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component(roles = DocumentContentAsyncRenderer.class)
public class DocumentContentAsyncRenderer extends AbstractBlockAsyncRenderer
{
    @Inject
    private DocumentContentAsyncExecutor executor;

    @Inject
    private AsyncContext asyncContext;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    private DocumentContentAsyncParser asyncParser;

    private DocumentDisplayerParameters parameters;

    private AsyncProperties asyncProperties;

    private DocumentReference documentReference;

    private List<String> id;

    /**
     * @param document the document to execute
     * @param parameters display parameters
     * @return the context elements required during the execution
     */
    public Set<String> initialize(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        this.parameters = parameters;

        // Make sure the restricted property of the document is properly taken into account.
        if (document.isRestricted()) {
            parameters.setTransformationContextRestricted(true);
        }

        this.asyncProperties = this.asyncParser.getAsyncProperties(document);

        String transformationId = this.defaultEntityReferenceSerializer
            .serialize(parameters.isContentTransformed() && parameters.isTransformationContextIsolated()
                ? document.getDocumentReference() : this.documentAccessBridge.getCurrentDocumentReference());

        this.documentReference = document.getDocumentReference();

        if (this.asyncProperties.isAsyncAllowed() || this.asyncProperties.isCacheAllowed()) {
            this.id = createId("display", "document", "content",
                this.defaultEntityReferenceSerializer.serialize(this.documentReference), this.parameters.getSectionId(),
                this.parameters.getTargetSyntax() != null ? this.parameters.getTargetSyntax().toIdString() : "",
                transformationId, this.parameters.isContentTransformed(),
                this.parameters.isTransformationContextRestricted(), this.parameters.isTransformationContextIsolated());
        }

        this.executor.initialize(transformationId, document, parameters);

        return this.asyncProperties.getContextElements();
    }

    @Override
    public Block execute(boolean async, boolean cached) throws RenderingException
    {
        // Register the known involved references
        this.asyncContext.useEntity(this.documentReference);

        return this.executor.execute(async);
    }

    @Override
    public List<String> getId()
    {
        return this.id;
    }

    @Override
    public boolean isAsyncAllowed()
    {
        return this.asyncProperties.isAsyncAllowed();
    }

    @Override
    public boolean isCacheAllowed()
    {
        return this.asyncProperties.isCacheAllowed();
    }

    @Override
    public boolean isInline()
    {
        return false;
    }

    @Override
    public Syntax getTargetSyntax()
    {
        return this.parameters.getTargetSyntax();
    }
}
