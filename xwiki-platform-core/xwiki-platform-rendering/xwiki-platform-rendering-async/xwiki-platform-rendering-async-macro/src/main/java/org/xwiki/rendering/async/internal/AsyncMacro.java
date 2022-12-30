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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.AsyncMacroParameters;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Asynchronous and cached execution of wiki content.
 * 
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Named("async")
@Singleton
public class AsyncMacro extends AbstractExecutedContentMacro<AsyncMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Asynchronous and cached execution of wiki content.";

    @Inject
    private AsyncContext asyncContext;

    /**
     * Default constructor.
     */
    public AsyncMacro()
    {
        super("Async", DESCRIPTION, new DefaultContentDescriptor("Content to execute", true, Block.LIST_BLOCK_TYPE),
            AsyncMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public List<Block> execute(AsyncMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        XDOM xdom = this.parser.parse(content, context, false, context.isInline());

        if (xdom.getChildren().isEmpty()) {
            return Collections.emptyList();
        }

        String source = getCurrentSource(context);

        List<Object> idElements = new ArrayList<>(4);

        idElements.add("async");
        idElements.add("macro");

        // Generate the id if not provided
        if (parameters.getId() != null) {
            idElements.addAll(parameters.getId());
        } else {
            // Add the source reference in the id
            if (source != null) {
                idElements.add(source);
            }
            // Add the macro index in the id
            long index = context.getXDOM().indexOf(context.getCurrentMacroBlock());
            if (index != -1) {
                idElements.add(index);
            }
        }

        BlockAsyncRendererConfiguration configuration =
            createBlockAsyncRendererConfiguration(idElements, xdom, source, context);

        // Enable/disable async
        configuration.setAsyncAllowed(parameters.isAsync());
        // Enable/disable caching
        configuration.setCacheAllowed(parameters.isCached());
        // Indicate context entries
        configuration.addContextEntries(parameters.getContext());

        try {
            Block result = this.executor.execute(configuration);

            // Indicate the content is not transformed if the current execution is not async
            if (!parameters.isAsync() || !this.asyncContext.isEnabled()) {
                result = new MetaDataBlock(Collections.singletonList(result), getNonGeneratedContentMetaData());
            }

            return Collections.singletonList(result);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed start the execution of the macro", e);
        }
    }
}
