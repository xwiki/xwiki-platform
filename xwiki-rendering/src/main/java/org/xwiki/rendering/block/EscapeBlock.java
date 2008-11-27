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

/**
 * Represents some string escape, i.e. for which there's no wiki syntax interpretation. Note that this is slightly
 * different from a {@link VerbatimBlock} in that an escape block is usually meant to replace a single character
 * being escaped while a verbatim block is meant to not render a longer text portion. Both have a different wiki
 * syntax hence making it easy for users to choose one of the other. For example in XWiki syntax to escape a quote
 * you use <code>~"</code> while a verbatim block would be for example {{{this is "verbatim"}}}. 
 * 
 * @version $Id$
 * @since 1.5RC1
 * @see Listener#onEscape(String)
 */
public class EscapeBlock extends AbstractBlock
{
    /**
     * The string to escape.
     */
    private String escapedString;

    /**
     * @param escapedString the string to escape
     */
    public EscapeBlock(String escapedString)
    {
        this.escapedString = escapedString;
    }

    /**
     * @return the string to escape
     */
    public String getEscapedString()
    {
        return this.escapedString;
    }

    /**
     * {@inheritDoc}
     * @see AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        listener.onEscape(getEscapedString());
    }
}
