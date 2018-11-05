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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererDecorator;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.uiextension.UIExtension;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class to automate things common to most implementations of {@link UIExtension}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractWikiUIExtension implements WikiComponent, UIExtension
{
    protected final ComponentManager componentManager;

    protected final JobProgressManager progress;

    protected final ErrorBlockGenerator errorBlockGenerator;

    protected final AsyncContext asyncContext;

    protected final BlockAsyncRendererExecutor executor;

    protected final RenderingContext renderingContext;

    protected final ContentParser parser;

    protected final boolean async;

    protected final boolean cached;

    protected final Set<String> contextEntries;

    protected final ObjectReference objectReference;

    protected final DocumentReference authorReference;

    protected final XDOM xdom;

    protected final Syntax syntax;

    /**
     * @param baseObject the object containing ui extension setup
     * @param componentManager The XWiki content manager
     * @throws ComponentLookupException If module dependencies are missing
     * @throws WikiComponentException When failing to parse content
     */
    public AbstractWikiUIExtension(BaseObject baseObject, ComponentManager componentManager)
        throws ComponentLookupException, WikiComponentException
    {
        this.async = baseObject.getIntValue(WikiUIExtensionConstants.ASYNC_ENABLED_PROPERTY, 0) == 1;
        this.cached = baseObject.getIntValue(WikiUIExtensionConstants.ASYNC_CACHED_PROPERTY, async ? 1 : 0) == 1;
        List<String> contextEntriesList = baseObject.getListValue(WikiUIExtensionConstants.ASYNC_CONTEXT_PROPERTY);
        this.contextEntries = contextEntriesList != null ? new HashSet<>(contextEntriesList) : null;
        this.objectReference = baseObject.getReference();

        XWikiDocument ownerDocument = baseObject.getOwnerDocument();

        this.authorReference = ownerDocument.getAuthorReference();

        this.componentManager = componentManager;
        this.progress = componentManager.getInstance(JobProgressManager.class);
        this.errorBlockGenerator = componentManager.getInstance(ErrorBlockGenerator.class);
        this.asyncContext = componentManager.getInstance(AsyncContext.class);
        this.executor = componentManager.getInstance(BlockAsyncRendererExecutor.class);
        this.renderingContext = componentManager.getInstance(RenderingContext.class);
        this.parser = componentManager.getInstance(ContentParser.class);

        this.syntax = ownerDocument.getSyntax();
        String content = baseObject.getStringValue(WikiUIExtensionConstants.CONTENT_PROPERTY);
        this.xdom = this.parser.parse(content, syntax, ownerDocument.getDocumentReference());
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

            Set<String> entries = new HashSet<>();
            if (this.contextEntries != null) {
                entries.addAll(this.contextEntries);
            }

            // TODO: add right checking (view on current document ?)
            result = this.executor.execute(executorConfiguration, entries);
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
        XDOM transformedBlock = this.xdom;

        BlockAsyncRendererConfiguration executorConfiguration =
            new BlockAsyncRendererConfiguration(Arrays.asList("uix", getId()), transformedBlock);

        // The transformation id
        executorConfiguration.setTransformationId(getRoleHint());

        // Indicate the source syntax
        executorConfiguration.setDefaultSyntax(this.syntax);

        // The author of the source
        executorConfiguration.setAuthorReference(getAuthorReference());

        // The syntax in which the result will be rendered
        executorConfiguration.setTargetSyntax(this.renderingContext.getTargetSyntax());

        // Add decorator
        if (this instanceof BlockAsyncRendererDecorator) {
            executorConfiguration.setDecorator((BlockAsyncRendererDecorator) this);
        }

        // Indicate if asynchronous execution is enabled for this UI extension
        executorConfiguration.setAsync(this.async);

        // Indicate if caching is enabled for this UI extension
        executorConfiguration.setCached(this.cached);

        if (this.cached) {
            // The role type and hint of the UI extension component so that the cache is invalidated when modified
            executorConfiguration.useComponent(getRoleType(), getRoleHint());
        }

        return executorConfiguration;
    }
}
