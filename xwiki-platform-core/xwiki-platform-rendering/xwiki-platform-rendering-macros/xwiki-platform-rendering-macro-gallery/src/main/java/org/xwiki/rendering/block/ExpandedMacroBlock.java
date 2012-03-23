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

import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

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
     * Creates a new expanded macro block.
     * 
     * @param id the id of the macro
     * @param parameters the parameters of the macro
     * @param contentRenderer the component used to reconstruct the macro content from the child blocks
     * @param inline indicates if the macro is located in an in-line content (e.g. inside a paragraph)
     */
    public ExpandedMacroBlock(String id, Map<String, String> parameters, BlockRenderer contentRenderer, boolean inline)
    {
        super(id, parameters, inline);
        this.contentRenderer = contentRenderer;
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
        contentRenderer.render(xdom, printer);
        return printer.toString();
    }
}
