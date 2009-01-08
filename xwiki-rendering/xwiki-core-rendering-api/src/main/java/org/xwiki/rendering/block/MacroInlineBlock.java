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
 * Represents an inline Macro defined in a page (ie a Macro located inside another Block, for example a Macro located
 * inside a Paragraph Block).
 *
 * @version $Id$
 * @since 1.6M2
 */
public class MacroInlineBlock extends AbstractMacroBlock
{
    public MacroInlineBlock(String name, Map<String, String> parameters)
    {
        super(name, parameters);
    }

    public MacroInlineBlock(String name, Map<String, String> parameters, String content)
    {
        super(name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        // See org.xwiki.rendering.block.StandaloneMacroBlock for explanations.
        listener.onInlineMacro(getName(), getParameters(), getContent());
    }

}
