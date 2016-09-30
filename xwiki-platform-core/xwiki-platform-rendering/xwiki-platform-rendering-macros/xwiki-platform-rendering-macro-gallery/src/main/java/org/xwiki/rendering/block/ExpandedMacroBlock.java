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
package org.xwiki.rendering.block;

import java.util.Map;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

/**
 * A {@link MacroBlock} whose content is expanded. The macro content can be accessed and modified through the child
 * blocks. This type of macro block is suited only for macros whose content can be parsed into a list of block nodes.
 * 
 * @since 3.0M3
 * @version $Id$
 */
public class ExpandedMacroBlock extends MacroBlock
{
    /**
     * The component used to reconstruct the macro content from the child blocks.
     */
    private final BlockRenderer contentRenderer;

    /**
     * Used to get the {@link #contentRenderer} corresponding to the syntax specified by an ancestor meta data block.
     */
    private final ComponentManager componentManager;

    /**
     * Creates a new expanded macro block.
     * 
     * @param id the id of the macro
     * @param parameters the parameters of the macro
     * @param contentRenderer the component used to reconstruct the macro content from the child blocks
     * @param inline indicates if the macro is located in an in-line content (e.g. inside a paragraph)
     */
    public ExpandedMacroBlock(String id, Map<String, String> parameters, BlockRenderer contentRenderer, boolean inline)
    {
        this(id, parameters, contentRenderer, inline, null);
    }

    /**
     * Creates a new expanded macro block.
     * 
     * @param id the id of the macro
     * @param parameters the parameters of the macro
     * @param contentRenderer the component used to reconstruct the macro content from the child blocks
     * @param inline indicates if the macro is located in an in-line content (e.g. inside a paragraph)
     * @param componentManager allows the expanded macro block to use a different syntax for its content if one of its
     *            ancestor meta data blocks specify the syntax
     */
    public ExpandedMacroBlock(String id, Map<String, String> parameters, BlockRenderer contentRenderer, boolean inline,
        ComponentManager componentManager)
    {
        super(id, parameters, inline);
        this.contentRenderer = contentRenderer;
        this.componentManager = componentManager;
    }

    @Override
    public String getContent()
    {
        // Keep the macro content synchronized with the child blocks.
        WikiPrinter printer = new DefaultWikiPrinter();
        XDOM xdom;
        // We need the begin/endDocument events so we wrap the child blocks in a XDOM block.
        if (getChildren().size() == 1 && getChildren().get(0) instanceof XDOM) {
            xdom = (XDOM) getChildren().get(0);
        } else {
            xdom = new XDOM(getChildren());
        }
        getContentRenderer().render(xdom, printer);
        return printer.toString();
    }

    private BlockRenderer getContentRenderer()
    {
        if (this.componentManager != null) {
            MetaDataBlock metaDataBlock =
                getFirstBlock(new MetadataBlockMatcher(MetaData.SYNTAX), Axes.ANCESTOR_OR_SELF);
            if (metaDataBlock != null) {
                Syntax syntax = (Syntax) metaDataBlock.getMetaData().getMetaData(MetaData.SYNTAX);
                if (this.componentManager.hasComponent(BlockRenderer.class, syntax.toIdString())) {
                    try {
                        return this.componentManager.getInstance(BlockRenderer.class, syntax.toIdString());
                    } catch (ComponentLookupException e) {
                        // Fall-back on the default content renderer;
                    }
                }
            }
        }

        return this.contentRenderer;
    }
}
