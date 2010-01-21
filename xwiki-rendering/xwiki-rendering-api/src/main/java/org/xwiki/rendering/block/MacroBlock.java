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

import org.xwiki.rendering.listener.Listener;

/**
 * Represents a Macro (standalone or inline) defined in a page.
 * <p>
 * Note: You can get macro parameters using {@link #getParameters()} for example. Macro block is reusing Block standard
 * custom parameters API since macro by definition already have parameters and don't need also block parameters. So in
 * this case MacroBlock parameters and Block parameters are the same thing.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class MacroBlock extends AbstractBlock
{
    /**
     * @see #getId
     */
    private String id;

    /**
     * The macro content for macro that have content. Otherwise it's null.
     */
    private String content;

    /**
     * The macro is located in a inline content (like paragraph, etc.).
     */
    private boolean isInline;

    /**
     * @param id the id of the macro
     * @param parameters the parameters of the macro
     * @param isInline indicate if the macro is located in a inline content (like paragraph, etc.)
     */
    public MacroBlock(String id, Map<String, String> parameters, boolean isInline)
    {
        this(id, parameters, null, isInline);
    }

    /**
     * @param id the id of the macro
     * @param parameters the parameters of the macro
     * @param content the content of the macro. Null if the macro does not have content
     * @param isInline indicate if the macro is located in a inline content (like paragraph, etc.)
     */
    public MacroBlock(String id, Map<String, String> parameters, String content, boolean isInline)
    {
        super(parameters);

        this.id = id;
        this.content = content;
        this.isInline = isInline;
    }

    /**
     * @return the macro id (eg "toc" for the TOC Macro).
     * @since 2.0M3
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the macro content.
     */
    public String getContent()
    {
        return this.content;
    }

    /**
     * @return if true the macro is located in a inline content (like paragraph, etc.).
     */
    public boolean isInline()
    {
        return this.isInline;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        // Don't do anything here since we want the Macro Transformer component to take in charge
        // Macro execution. This is because Macro execution is a complex process that involves:
        // * computing the order in which the macros should be evaluated. For example the TOC macro
        // should evaluate last since other macros can contribute headers/sections blocks.
        // * some macros need to modify blocks in the XDOM object
        // * macro execution is a multi-pass process
        // In essence the Macro Transformer will replace all MacroBlock blocks with other Blocks
        // generated from the execution of the Macros when XDOM.traverse() is called there
        // won't be any MacroBlock.traverse() method called at all.

        // Note: We're calling the event to let other listener downstream decide what to do with it.
        // In practice as described above this method will never get called when the whole rendering
        // process is executed. This does get called during our unit tests though.
        listener.onMacro(getId(), getParameters(), getContent(), isInline());
    }
}
