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
package org.xwiki.uiextension.internal;

import java.util.Arrays;

import javax.inject.Inject;

import org.xwiki.component.wiki.internal.AbstractAsyncContentBaseObjectWikiComponent;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererDecorator;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.uiextension.UIExtension;

/**
 * Base class to automate things common to most implementations of {@link UIExtension}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractWikiUIExtension extends AbstractAsyncContentBaseObjectWikiComponent implements UIExtension
{
    private static final String TM_FAILEDUIX = "uiextension.error.failed";

    @Inject
    protected JobProgressManager progress;

    @Inject
    protected ErrorBlockGenerator errorBlockGenerator;

    @Inject
    protected AsyncContext asyncContext;

    @Inject
    protected RenderingContext renderingContext;

    @Override
    protected String getContentPropertyName()
    {
        return WikiUIExtensionConstants.CONTENT_PROPERTY;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.objectReference.getDocumentReference();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.objectReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    public Block execute()
    {
        return execute(false);
    }

    @Override
    public Block execute(boolean inline)
    {
        this.progress.startStep(this, "uix.progress.execute", "Execute UIX with id [{}]", getId());

        Block result;
        try {
            BlockAsyncRendererConfiguration executorConfiguration = configure(inline);

            result = this.executor.execute(executorConfiguration);
        } catch (Exception e) {
            result = new CompositeBlock(this.errorBlockGenerator.generateErrorBlocks(false, TM_FAILEDUIX,
                "Failed to execute UIX with id [{}]", null, getId(), e));
        } finally {
            this.progress.endStep(this);
        }

        return result;
    }

    protected BlockAsyncRendererConfiguration configure(boolean inline)
    {
        // Prepare the block if it's not the case yet
        XDOM transformedBlock = getPreparedContent();

        // We need to clone the block to avoid transforming the original and make it useless after the first
        // transformation
        transformedBlock = transformedBlock.clone();

        BlockAsyncRendererConfiguration executorConfiguration =
            new BlockAsyncRendererConfiguration(Arrays.asList("uix", getId()), transformedBlock);

        // The transformation id
        executorConfiguration.setTransformationId(getRoleHint());

        // Indicate the source syntax
        executorConfiguration.setDefaultSyntax(this.syntax);

        // Inline
        executorConfiguration.setInline(inline);

        // The author of the source
        executorConfiguration.setSecureReference(getDocumentReference(), getAuthorReference());

        // The syntax in which the result will be rendered
        executorConfiguration.setTargetSyntax(this.renderingContext.getTargetSyntax());

        // Add decorator
        if (this instanceof BlockAsyncRendererDecorator) {
            executorConfiguration.setDecorator((BlockAsyncRendererDecorator) this);
        }

        // Indicate if asynchronous execution is enabled for this UI extension
        executorConfiguration.setAsyncAllowed(this.asyncAllowed);

        // Indicate if caching is enabled for this UI extension
        executorConfiguration.setCacheAllowed(this.cacheAllowed);

        if (this.cacheAllowed) {
            // The role type and hint of the UI extension component so that the cache is invalidated when modified
            executorConfiguration.useComponent(getRoleType(), getRoleHint());
        }

        executorConfiguration.setContextEntries(this.contextEntries);

        return executorConfiguration;
    }
}
