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

import java.util.List;
import java.util.Map;

/**
 * A special block that Macro Blocks emits when they are executed so that it's possible to reconstruct
 * the initial syntax even after Macros have been executed.
 *
 * @version $Id: $
 * @since 1.5M2
 */
public class MacroMarkerBlock extends AbstractFatherBlock
{
    /**
     * The Macro name that we are preserving.
     */
    private String name;

    /**
     * The Macro parameters that we are preserving.
     */
    private Map<String, String> parameters;

    /**
     * The Macro content that we are preserving.
     */
    private String content;

    public MacroMarkerBlock(String name, Map<String, String> parameters, List<Block> childBlocks)
    {
        this(name, parameters, null, childBlocks);
    }

    public MacroMarkerBlock(String name, Map<String, String> parameters, String content, List<Block> childBlocks)
    {
        super(childBlocks);
        this.name = name;
        this.parameters = parameters;
        this.content = content;
    }

    public String getName()
    {
        return this.name;
    }

    public Map<String, String> getParameters()
    {
        return this.parameters;
    }

    public String getContent()
    {
        return this.content;
    }
    
    public void before(Listener listener)
    {
        listener.beginMacroMarker(getName(), getParameters(), getContent());
    }

    public void after(Listener listener)
    {
        listener.endMacroMarker(getName(), getParameters(), getContent());
    }
}
