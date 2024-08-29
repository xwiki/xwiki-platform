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
package org.xwiki.annotation.content;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Component responsible for extracting plain text from inside a given {@code String} depending on the syntax associated
 * with it. For example, the plain text could be extracted from inside an HTML content.
 * <p>
 * Note that from an architecture POV, it's not possible to use XWiki Rendering's parsers and Plain Text Renderers
 * since for example not all HTML content can be expressed into XDOM Block (e.g. FORM tags cannot) and thus we would
 * lose content and not be able to annotate it.
 * 
 * @version $Id$
 * @since 13.10RC1
 */
@Role
public interface TextExtractor
{
    /**
     * Extract text from content considering the associated syntax.
     *
     * @param content the content from where to extract the text
     * @param syntax content specific syntax
     * @return the plain text
     */
    String extractText(String content, Syntax syntax);
}
