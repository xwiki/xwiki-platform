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

import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;

/**
 * The reference of a content to highlight.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
@Unstable
public class CodeMacroSourceReference
{
    /**
     * The type to use for a reference which contain the actual content.
     */
    public static final String TYPE_STRING = "string";

    /**
     * The type to use for a reference targeting a script context entry.
     */
    public static final String TYPE_SCRIPT = "script";

    /**
     * The type to use for a reference targeting a URL.
     */
    public static final String TYPE_URL = "url";

    /**
     * The type to use for a reference targeting a local file.
     */
    public static final String TYPE_FILE = "file";

    /**
     * The type to use for a reference targeting an XWiki model entity, the reference is a serialized entity reference
     * (including the entity type).
     */
    public static final String TYPE_ENTITY = "entity";

    /**
     * The type to use for a reference targeting an XWiki model entity, the reference is a serialized document
     * reference.
     */
    public static final String TYPE_DOCUMENT = "document";

    /**
     * The type to use for a reference targeting an XWiki model entity, the reference is a serialized page reference.
     */
    public static final String TYPE_PAGE = "page";

    /**
     * The type to use for a reference targeting an XWiki model entity, the reference is a serialized document object
     * property reference.
     */
    public static final String TYPE_OBJECT_PROPERTY = "object_property";

    /**
     * The type to use for a reference targeting an XWiki model entity, the reference is a serialized page object
     * property reference.
     */
    public static final String TYPE_PAGE_OBJECT_PROPERTY = "page_object_property";

    /**
     * The type to use for a reference targeting an XWiki model entity, the reference is a serialized page reference.
     */
    public static final String TYPE_ATTACHMENT = "attachment";

    /**
     * The type to use for a reference targeting an XWiki model entity, the reference is a serialized page reference.
     */
    public static final String TYPE_PAGE_ATTACHMENT = "page_attachment";

    private final String type;

    private final String reference;

    /**
     * @param type the type of the content
     * @param reference the reference of the content
     */
    public CodeMacroSourceReference(String type, String reference)
    {
        this.type = type;
        this.reference = reference;
    }

    /**
     * @return the type of the content
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @return the reference of the content
     */
    public String getReference()
    {
        return this.reference;
    }

    @Override
    public String toString()
    {
        return getType() + ':' + getReference();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj != null) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof CodeMacroSourceReference) {
                CodeMacroSourceReference otherSource = (CodeMacroSourceReference) obj;

                return Objects.equals(getType(), otherSource.getType())
                    && Objects.equals(getReference(), otherSource.getReference());
            }
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(getType());
        builder.append(getReference());

        return builder.build();
    }
}
