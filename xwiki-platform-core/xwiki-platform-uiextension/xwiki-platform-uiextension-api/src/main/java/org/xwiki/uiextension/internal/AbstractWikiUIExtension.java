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

import java.lang.reflect.Type;
import java.util.Arrays;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
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

import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class to automate things common to most implementations of {@link UIExtension}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractWikiUIExtension extends AbstractAsyncContentBaseObjectWikiComponent implements UIExtension
{
    protected final JobProgressManager progress;

    protected final ErrorBlockGenerator errorBlockGenerator;

    protected final AsyncContext asyncContext;

    protected final RenderingContext renderingContext;

    /**
     * @param baseObject the object containing ui extension setup
     * @param roleType the role Type implemented
     * @param roleHint the role hint for this role implementation
     * @param componentManager The XWiki content manager
     * @throws ComponentLookupException If module dependencies are missing
     * @throws WikiComponentException When failing to parse content
     */
    public AbstractWikiUIExtension(BaseObject baseObject, Type roleType, String roleHint,
        ComponentManager componentManager) throws ComponentLookupException, WikiComponentException
    {
        super(baseObject, roleType, roleHint, componentManager);

        this.progress = componentManager.getInstance(JobProgressManager.class);
        this.errorBlockGenerator = componentManager.getInstance(ErrorBlockGenerator.class);
        this.asyncContext = componentManager.getInstance(AsyncContext.class);
        this.renderingContext = componentManager.getInstance(RenderingContext.class);
    }

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
        this.progress.startStep(this, "uix.progress.execute", "Execute UIX with id [{}]", getId());

        Block result;
        try {
            BlockAsyncRendererConfiguration executorConfiguration = configure();

            result = this.executor.execute(executorConfiguration, this.contextEntries);
        } catch (Exception e) {
            result = new CompositeBlock(this.errorBlockGenerator
                .generateErrorBlocks(String.format("Failed to execute UIX with id [%s]", getId()), e, false));
        } finally {
            this.progress.endStep(this);
        }

        return result;
    }

    protected BlockAsyncRendererConfiguration configure()
    {
        // We need to clone the block to avoid transforming the original and make it useless after the first
        // transformation
        XDOM transformedBlock = this.xdom.clone();

        BlockAsyncRendererConfiguration executorConfiguration =
            new BlockAsyncRendererConfiguration(Arrays.asList("uix", getId()), transformedBlock);

        // The transformation id
        executorConfiguration.setTransformationId(getRoleHint());

        // Indicate the source syntax
        executorConfiguration.setDefaultSyntax(this.syntax);

        // The author of the source
        executorConfiguration.setAuthorReference(getAuthorReference());
        executorConfiguration.setSourceReference(getDocumentReference());

        // The syntax in which the result will be rendered
        executorConfiguration.setTargetSyntax(this.renderingContext.getTargetSyntax());

        // Add decorator
        if (this instanceof BlockAsyncRendererDecorator) {
            executorConfiguration.setDecorator((BlockAsyncRendererDecorator) this);
        }

        // Indicate if asynchronous execution is enabled for this UI extension
        executorConfiguration.setAsyncAllowed(this.async);

        // Indicate if caching is enabled for this UI extension
        executorConfiguration.setCacheAllowed(this.cached);

        if (this.cached) {
            // The role type and hint of the UI extension component so that the cache is invalidated when modified
            executorConfiguration.useComponent(getRoleType(), getRoleHint());
        }

        return executorConfiguration;
    }
}
