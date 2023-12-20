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
package org.xwiki.wysiwyg.converter;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Converts HTML to/from a specified syntax.
 *
 * @version $Id$
 */
@Role
public interface HTMLConverter
{
    /**
     * Converts the given source text from the specified syntax to HTML.
     *
     * @param source the text to be converted
     * @param syntaxId the syntax of the source
     * @return the HTML result of the conversion
     * @deprecated since 11.9RC1, use {@link #toHTML(String, Syntax, EntityReference)} instead
     */
    @Deprecated
    String toHTML(String source, String syntaxId);

    /**
     * Converts the given source text from the specified syntax to HTML.
     *
     * @param source the text to be converted
     * @param syntax the syntax of the source
     * @param sourceReference the reference of the source
     * @return the HTML result of the conversion
     * @since 11.9RC1
     */
    default String toHTML(String source, Syntax syntax, EntityReference sourceReference)
    {
        return toHTML(source, syntax.toIdString());
    }

    /**
     * Converts the given source text from the specified syntax to HTML.
     *
     * @param source the text to be converted
     * @param syntax the syntax of the source
     * @param sourceReference the reference of the source
     * @param restricted true if the content of this property should be executed in a restricted content, false
     *            otherwise
     * @return the HTML result of the conversion
     * @since 14.10
     * @since 14.4.7
     * @since 13.10.11
     */
    default String toHTML(String source, Syntax syntax, EntityReference sourceReference, boolean restricted)
    {
        return toHTML(source, syntax, sourceReference);
    }

    /**
     * Cleans and converts the given HTML fragment to the specified syntax.
     *
     * @param html the HTML text to be converted
     * @param syntaxId the syntax identifier
     * @return the result on the conversion
     */
    String fromHTML(String html, String syntaxId);

    /**
     * Parses the given HTML fragment and renders the result in annotated XHTML syntax.
     *
     * @param html the HTML fragment to be parsed and rendered
     * @param syntax the storage syntax
     * @return the XHTML result of rendering the given HTML fragment
     * @deprecated since 11.9RC1, use {@link #parseAndRender(String, Syntax, EntityReference)} instead
     */
    @Deprecated
    String parseAndRender(String html, String syntax);

    /**
     * Parses the given HTML fragment and renders the result in annotated XHTML syntax.
     *
     * @param html the HTML fragment to be parsed and rendered
     * @param syntax the storage syntax
     * @param sourceReference the reference of the html (where it's coming from)
     * @return the XHTML result of rendering the given HTML fragment
     * @since 11.9RC1
     */
    default String parseAndRender(String html, Syntax syntax, EntityReference sourceReference)
    {
        return parseAndRender(html, syntax.toIdString());
    }

    /**
     * Parses the given HTML fragment and renders the result in annotated XHTML syntax.
     *
     * @param html the HTML fragment to be parsed and rendered
     * @param syntax the storage syntax
     * @param sourceReference the reference of the html (where it's coming from)
     * @param restricted true if the content of this property should be executed in a restricted content, false
     *            otherwise
     * @return the XHTML result of rendering the given HTML fragment
     * @since 14.10
     * @since 14.4.7
     * @since 13.10.11
     */
    default String parseAndRender(String html, Syntax syntax, EntityReference sourceReference, boolean restricted)
    {
        return parseAndRender(html, syntax, sourceReference);
    }
}
