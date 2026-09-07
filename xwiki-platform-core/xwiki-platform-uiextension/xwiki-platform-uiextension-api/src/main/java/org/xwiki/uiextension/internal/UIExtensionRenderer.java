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

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.uiextension.UIExtension;

/**
 * Executes and renders UI extensions in a given output syntax.
 * <p>
 * The output syntax is used both to execute and to render the UI extensions. Executing them with the output syntax as
 * target syntax matters because a UI extension based on a template or on a wiki page produces content (e.g. a raw
 * block) tagged with the target syntax found in the rendering context: if that syntax is not the one used afterwards
 * to render the result then the content is silently dropped.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Component(roles = UIExtensionRenderer.class)
@Singleton
public class UIExtensionRenderer
{
    @Inject
    private RenderingContext renderingContext;

    /**
     * Used to look up the renderer for the output syntax. We use the Context Component Manager because renderers can be
     * registered for a specific user, for a specific wiki or for a whole farm.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Executes and renders the given UI extensions in the given output syntax, concatenating the results without any
     * separator. A UI extension that fails to execute is skipped, so that a single faulty UI extension doesn't hide
     * the others.
     *
     * @param extensions the UI extensions to execute and render; {@code null} entries are ignored
     * @param outputSyntax the syntax to execute and render the UI extensions in
     * @param inline {@code true} if the UI extensions are executed in an inline context, {@code false} otherwise
     * @return the concatenated result of rendering the given UI extensions in the given output syntax
     * @throws RenderingException if there's no renderer for the given output syntax
     */
    public String render(List<UIExtension> extensions, Syntax outputSyntax, boolean inline) throws RenderingException
    {
        BlockRenderer blockRenderer;
        try {
            blockRenderer =
                this.contextComponentManagerProvider.get().getInstance(BlockRenderer.class, outputSyntax.toIdString());
        } catch (ComponentLookupException e) {
            throw new RenderingException(
                String.format("Failed to find a renderer for syntax [%s]", outputSyntax.toIdString()), e);
        }

        // Execute the UI extensions with the output syntax as target syntax. Only the target syntax is overwritten:
        // everything else, including the default (source) syntax, must remain the one the caller is executing in.
        MutableRenderingContext mutableRenderingContext =
            this.renderingContext instanceof MutableRenderingContext mutableContext ? mutableContext : null;
        if (mutableRenderingContext != null) {
            mutableRenderingContext.push(this.renderingContext.getTransformation(), this.renderingContext.getXDOM(),
                this.renderingContext.getDefaultSyntax(), this.renderingContext.getTransformationId(),
                this.renderingContext.isRestricted(), outputSyntax);
        }

        try {
            WikiPrinter printer = new DefaultWikiPrinter();
            blockRenderer.render(execute(extensions, inline), printer);
            return printer.toString();
        } finally {
            if (mutableRenderingContext != null) {
                mutableRenderingContext.pop();
            }
        }
    }

    private List<Block> execute(List<UIExtension> extensions, boolean inline)
    {
        List<Block> blocks = new ArrayList<>(extensions.size());

        for (UIExtension extension : extensions) {
            if (extension == null) {
                this.logger.warn("Ignoring a null UI extension.");
                continue;
            }

            try {
                blocks.add(extension.execute(inline));
            } catch (Exception e) {
                this.logger.warn("Failed to execute the UI extension [{}]. Root cause is [{}]", extension.getId(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return blocks;
    }
}
