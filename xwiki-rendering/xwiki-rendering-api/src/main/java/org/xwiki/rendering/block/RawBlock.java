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
import org.xwiki.rendering.parser.Syntax;

/**
 * Represents some raw content that shouldn't be parsed or modified and that should be injected as is
 * in any output. The content depends on a syntax and listeners decide if they can handle that syntax 
 * or not. For example if it's in "xhtml/1.0" syntax then the XHTML Renderer can insert it directly
 * in the XHTML output.
 * 
 * @version $Id$
 * @since 1.8.3
 */
public class RawBlock extends AbstractBlock
{
    /**
     * @see #getRawContent()
     */
    private String rawContent;

    /**
     * @see #getSyntax()
     */
    private Syntax syntax;
    
    /**
     * @param rawContent the content to inject as is into the listener (it won't be modified)
     * @param syntax the syntax in which the content is written
     */
    public RawBlock(String rawContent, Syntax syntax)
    {
        this.rawContent = rawContent;
        this.syntax = syntax;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractBlock#traverse(Listener)
     */
    public void traverse(Listener listener)
    {
        listener.onRawText(getRawContent(), getSyntax());
    }

    /**
     * @return the content to inject as is into the listener (it won't be modified)
     */
    public String getRawContent()
    {
        return this.rawContent;
    }
    
    /**
     * @return the syntax in which the content is written
     */
    public Syntax getSyntax()
    {
        return this.syntax;
    }
}
