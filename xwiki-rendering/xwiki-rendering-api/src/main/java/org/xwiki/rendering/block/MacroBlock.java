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

import org.xwiki.rendering.listener.Listener;

import java.util.Map;

/**
 * Represents a Macro (standalone or inline) defined in a page.
 * 
 * @version $Id: MacroBlock.java 16357 2009-02-06 16:41:42Z tmortagne $
 * @since 1.8M2
 */
public class MacroBlock extends AbstractBlock
{
    /**
     * The macro name.
     */
    private String name;

    /**
     * The macro content for macro that have content. Otherwise it's null.
     */
    private String content;

    /**
     * The macro is located in a inline content (like paragraph, etc.).
     */
    boolean isInline;

    public MacroBlock(String name, Map<String, String> parameters, boolean isInline)
    {
        this(name, parameters, null, isInline);
    }

    public MacroBlock(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        super(parameters);

        this.name = name;
        this.content = content;
        this.isInline = isInline;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroBlock#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroBlock#getContent()
     */
    public String getContent()
    {
        return this.content;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.MacroBlock#isInline()
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
        listener.onMacro(getName(), getParameters(), getContent(), isInline());
    }
}
