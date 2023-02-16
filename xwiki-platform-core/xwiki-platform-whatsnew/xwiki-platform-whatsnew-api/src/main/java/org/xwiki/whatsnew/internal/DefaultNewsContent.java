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
package org.xwiki.whatsnew.internal;

import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.whatsnew.NewsContent;

/**
 * Represents some content from a news source item. It holds both the raw content and the syntax in which the content
 * is written in.
 *
 * @version $Id$
 */
public class DefaultNewsContent implements NewsContent
{
    private String content;

    private Syntax syntax;

    /**
     * @param content see {@link #getContent()}
     * @param syntax see {@link #getSyntax()}
     */
    public DefaultNewsContent(String content, Syntax syntax)
    {
        this.content = content;
        this.syntax = syntax;
    }

    /**
     * @return the raw content in the specified syntax
     */
    public String getContent()
    {
        return this.content;
    }

    /**
     * @return the syntax in which the content is written in
     */
    public Syntax getSyntax()
    {
        return this.syntax;
    }
}
