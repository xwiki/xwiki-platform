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
package org.xwiki.rendering.macro.code.source;

import org.xwiki.stability.Unstable;

/**
 * A content to use in a code macro.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
@Unstable
public class CodeMacroSource
{
    private final CodeMacroSourceReference reference;

    private final String content;

    private final String language;

    /**
     * @param reference the reference of the content
     * @param content the content to highlight
     * @param language the language of the content
     */
    public CodeMacroSource(CodeMacroSourceReference reference, String content, String language)
    {
        this.reference = reference;
        this.content = content;
        this.language = language;
    }

    /**
     * @return the reference of the content
     */
    public CodeMacroSourceReference getReference()
    {
        return this.reference;
    }

    /**
     * @return the content to highlight
     */
    public String getContent()
    {
        return this.content;
    }

    /**
     * @return the language of the content
     */
    public String getLanguage()
    {
        return this.language;
    }
}
