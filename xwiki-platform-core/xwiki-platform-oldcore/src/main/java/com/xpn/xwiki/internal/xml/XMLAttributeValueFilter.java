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
package com.xpn.xwiki.internal.xml;

import org.apache.ecs.filter.CharacterFilter;

/**
 * A filter that can be used to encode the attribute values when serializing XML elements. It should behave exactly as
 * {@link org.xwiki.xml.XMLUtils#escape(Object)}.
 * <p>
 * Fixes the encoding of the apostrophe character which by default is replaced with a left single quote..
 *
 * @version $Id$
 * @since 4.3M2
 */
public class XMLAttributeValueFilter extends CharacterFilter
{
    /**
     * Serial version identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public XMLAttributeValueFilter()
    {
        addAttribute("'", "&#39;");
        // Left curly bracket is included here to protect against {{/html}} in XWiki 2.x syntax.
        addAttribute("{", "&#123;");
    }
}
