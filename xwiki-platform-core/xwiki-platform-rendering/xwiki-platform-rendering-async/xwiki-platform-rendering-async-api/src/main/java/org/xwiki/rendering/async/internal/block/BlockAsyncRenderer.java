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
package org.xwiki.rendering.async.internal.block;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Helper to execute Block based asynchronous renderer.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component(roles = BlockAsyncRenderer.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class BlockAsyncRenderer implements AsyncRenderer
{
    @Inject
    private TransformationManager transformationManager;

    @Inject
    @Named("contex")
    private Provider<ComponentManager> componentManager;

    private BlockAsyncRendererConfiguration configuration;

    private Set<EntityReference> references = Collections.emptySet();

    /**
     * @param configuration the configuration of the renderer
     * @param references the references involved in the rendering (they will be used to invalidate the cache when one of
     *            those entities is modified). More can be injected by the {@link Block}s trough script macros.
     */
    public void initialize(BlockAsyncRendererConfiguration configuration, Collection<EntityReference> references)
    {
        this.configuration = configuration;
        this.references = Collections.unmodifiableSet(new HashSet<>(references));
    }

    @Override
    public List<String> getId()
    {
        return this.configuration.getId();
    }

    @Override
    public BlockAsyncRendererResult render() throws RenderingException
    {
        Block block = this.configuration.getBlock();
        XDOM xdom;
        if (block instanceof XDOM) {
            xdom = (XDOM) block;
        } else {
            Block rootBlock = block.getRoot();

            if (rootBlock instanceof XDOM) {
                xdom = (XDOM) rootBlock;
            } else {
                xdom = new XDOM(Collections.singletonList(rootBlock));
            }
        }

        ///////////////////////////////////////
        // Transformations

        TransformationContext transformationContext =
            new TransformationContext(xdom, this.configuration.getDefaultSyntax(), false);

        this.transformationManager.performTransformations(block, transformationContext);

        ///////////////////////////////////////
        // Rendering

        Syntax targetSyntax = this.configuration.getTargetSyntax();
        PrintRendererFactory factory;
        try {
            factory = this.componentManager.get().getInstance(PrintRendererFactory.class, targetSyntax.toIdString());
        } catch (ComponentLookupException e) {
            throw new RenderingException("Failed to lookup renderer for syntax [" + targetSyntax + "]", e);
        }

        DefaultWikiPrinter printer = new DefaultWikiPrinter();

        factory.createRenderer(printer);

        return new BlockAsyncRendererResult(printer.toString(), block);
    }

    @Override
    public Set<EntityReference> getReferences()
    {
        return this.references;
    }
}
