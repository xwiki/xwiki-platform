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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

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
    private final MacroContentSourceReference reference;

    private final String content;

    private final String language;

    /**
     * @param reference the reference of the content
     * @param content the content to highlight
     * @param language the language of the content if known, null otherwise
     * @since 15.1RC1
     * @since 14.10.5
     */
    public CodeMacroSource(MacroContentSourceReference reference, String content, String language)
    {
        this.reference = reference;
        this.content = content;
        this.language = language;
    }

    /**
     * @return the reference of the content
     * @since 15.1RC1
     * @since 14.10.5
     */
    public MacroContentSourceReference getReference()
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
     * @return the language of the content if known, null otherwise
     */
    public String getLanguage()
    {
        return this.language;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.append("reference", getReference());
        builder.append("language", getLanguage());
        builder.append("content", getContent());

        return builder.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj != null) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof CodeMacroSource) {
                CodeMacroSource otherSource = (CodeMacroSource) obj;

                EqualsBuilder builder = new EqualsBuilder();

                builder.append(getReference(), otherSource.getReference());
                builder.append(getLanguage(), otherSource.getLanguage());
                builder.append(getContent(), otherSource.getContent());

                return builder.isEquals();
            }
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(getReference());
        builder.append(getLanguage());
        builder.append(getReference());

        return builder.build();
    }
}
