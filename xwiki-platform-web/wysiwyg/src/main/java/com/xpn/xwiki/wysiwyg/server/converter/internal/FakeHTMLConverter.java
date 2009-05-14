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
package com.xpn.xwiki.wysiwyg.server.converter.internal;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.wysiwyg.server.converter.HTMLConverter;

/**
 * Fake HTML converter that doesn't change the input string at all.
 * 
 * @version $Id$
 */
@Component(hints = {"xhtml/1.0", "html/4.01" })
public class FakeHTMLConverter implements HTMLConverter
{
    /**
     * {@inheritDoc}
     * 
     * @see HTMLConverter#fromHTML(String)
     */
    public String fromHTML(String html)
    {
        return html;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HTMLConverter#toHTML(String)
     */
    public String toHTML(String source)
    {
        return source;
    }
}
