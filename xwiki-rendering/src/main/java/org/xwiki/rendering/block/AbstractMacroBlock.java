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

/**
 * Represents a Macro (standalone or inline) defined in a page.
 *
 * @version $Id$
 * @since 1.6M2
 */
public abstract class AbstractMacroBlock extends AbstractBlock implements MacroBlock
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
     * Construct a Macro block for a Macro that has no content.
     *
     * @param name the macro name
     * @param parameters the macro parameters
     */
    public AbstractMacroBlock(String name, Map<String, String> parameters)
    {
        this(name, parameters, null);
    }
    
    /**
     * Construct a Macro block for a Macro that has content.
     *
     * @param name the macro name
     * @param parameters the macro parameters
     * @param content the macro content 
     */
    public AbstractMacroBlock(String name, Map<String, String> parameters, String content)
    {
        super(parameters);
        this.name = name;
        this.content = content;
    }

    /**
     * {@inheritDoc}
     * @see MacroBlock#getName() 
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * @see MacroBlock#getContent()  
     */
    public String getContent()
    {
        return this.content;
    }
}
