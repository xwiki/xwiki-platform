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
package org.xwiki.rendering.async.internal;

import java.util.List;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.internal.transformation.RenderingContextStore;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * Base class to implement a macro which have content to execute.
 * 
 * @param <P> the type of the macro parameters bean
 * @version $Id$
 * @since 14.8RC1
 * @since 14.4.5
 * @since 13.10.10
 */
public abstract class AbstractExecutedContentMacro<P> extends AbstractMacro<P>
{
    @Inject
    protected BlockAsyncRendererExecutor executor;

    @Inject
    protected MacroContentParser parser;

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    protected DocumentReferenceResolver<String> resolver;

    @Inject
    protected RenderingContext renderingContext;

    /**
     * Creates a new {@link Macro} instance.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description string describing this macro.
     * @param contentDescriptor the {@link ContentDescriptor} describing the content of this macro.
     * @param parametersBeanClass class of the parameters bean.
     */
    protected AbstractExecutedContentMacro(String name, String description, ContentDescriptor contentDescriptor,
        Class<?> parametersBeanClass)
    {
        super(name, description, contentDescriptor, parametersBeanClass);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    protected String getCurrentSource(MacroTransformationContext context)
    {
        String currentSource = null;

        if (context != null) {
            currentSource =
                context.getTransformationContext() != null ? context.getTransformationContext().getId() : null;

            MacroBlock currentMacroBlock = context.getCurrentMacroBlock();

            if (currentMacroBlock != null) {
                MetaDataBlock metaDataBlock =
                    currentMacroBlock.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Axes.ANCESTOR_OR_SELF);

                if (metaDataBlock != null) {
                    currentSource = (String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE);
                }
            }
        }

        return currentSource;
    }

    protected BlockAsyncRendererConfiguration createBlockAsyncRendererConfiguration(List<?> idElements, Block content,
        MacroTransformationContext context)
    {
        return createBlockAsyncRendererConfiguration(idElements, content, getCurrentSource(context), context);
    }

    protected BlockAsyncRendererConfiguration createBlockAsyncRendererConfiguration(List<?> idElements, Block content,
        String source, MacroTransformationContext context)
    {
        BlockAsyncRendererConfiguration configuration = new BlockAsyncRendererConfiguration(idElements, content);

        // Set author
        if (source != null) {
            DocumentReference sourceReference = this.resolver.resolve(source);
            configuration.setSecureReference(sourceReference, this.documentAccessBridge.getCurrentAuthorReference());

            // Invalidate the cache when the document containing the macro call is modified
            configuration.useEntity(sourceReference);
        }

        // Indicate if the result should be inline or not
        configuration.setInline(context.isInline());

        // Indicate the syntax of the content
        configuration.setDefaultSyntax(this.parser.getCurrentSyntax(context));

        // Indicate the target syntax
        configuration.setTargetSyntax(this.renderingContext.getTargetSyntax());

        // Set the transformation id
        configuration.setTransformationId(context.getTransformationContext().getId());

        // Indicate if we are in a restricted mode
        configuration.setResricted(context.getTransformationContext().isRestricted());
        // Make sure the fact that it's restricted is taken into account by the cache and is know to the ContextStore
        // components
        configuration.addContextEntries(RenderingContextStore.PROP_RESTRICTED);

        return configuration;
    }
}
