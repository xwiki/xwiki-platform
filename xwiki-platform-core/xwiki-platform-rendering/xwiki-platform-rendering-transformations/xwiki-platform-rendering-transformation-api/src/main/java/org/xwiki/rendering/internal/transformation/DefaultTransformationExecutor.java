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
package org.xwiki.rendering.internal.transformation;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationExecutor;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.security.authorization.AuthorExecutor;

/**
 * Default implementation for {@link TransformationExecutor}.
 * 
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultTransformationExecutor implements TransformationExecutor
{
    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    private TransformationManager transformationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    private XWikiTransformationContext transformationContext = new XWikiTransformationContext();

    @Override
    public TransformationExecutor withId(String id)
    {
        this.transformationContext.setId(id);
        return this;
    }

    @Override
    public TransformationExecutor withXDOM(XDOM xdom)
    {
        this.transformationContext.setXDOM(xdom);
        return this;
    }

    @Override
    public TransformationExecutor withSyntax(Syntax syntax)
    {
        this.transformationContext.setSyntax(syntax);
        return this;
    }

    @Override
    public TransformationExecutor withTargetSyntax(Syntax targetSyntax)
    {
        this.transformationContext.setTargetSyntax(targetSyntax);
        return this;
    }

    @Override
    public TransformationExecutor withRestricted(boolean restricted)
    {
        this.transformationContext.setRestricted(restricted);
        return this;
    }

    @Override
    public TransformationExecutor withTransformations(List<String> transformationNames)
    {
        this.transformationContext.setTransformationNames(Optional.ofNullable(transformationNames));
        return this;
    }

    @Override
    public TransformationExecutor withContentDocument(DocumentReference contentDocumentReference)
    {
        this.transformationContext.setContentDocumentReference(contentDocumentReference);
        return this;
    }

    @Override
    public void execute() throws TransformationException
    {
        try {
            // Even if the content on which transformations are executed has an associated document, we can't be sure
            // who is the author of the content so we assume it's the current user (i.e. the content may have changes
            // compared to the saved version of the document, and these changes are attributed to the current user).
            //
            // Note that there are two main cases we need to protect against:
            // * a user executing transformations that require more access rights that they have; we hope to prevent
            // this by treating the current user as the last author of the transformed content and by associating the
            // proper content document (to check access rights for)
            // * a user executing transformations for which they have the required access rights but the content has
            // been modified by someone else, with less access rights; this is handled at the level of the script
            // calling this API, e.g. by checking the presence of the CSRF token, i.e. by making sure the current user
            // is aware that transformations are going to be executed.
            this.authorExecutor.call(() -> {
                this.transformationManager.performTransformations(this.transformationContext.getXDOM(),
                    this.transformationContext);
                return null;
            }, this.documentAccessBridge.getCurrentUserReference(),
                this.transformationContext.getContentDocumentReference());
        } catch (Exception e) {
            throw new TransformationException("Failed to execute transformations", e);
        }
    }
}
