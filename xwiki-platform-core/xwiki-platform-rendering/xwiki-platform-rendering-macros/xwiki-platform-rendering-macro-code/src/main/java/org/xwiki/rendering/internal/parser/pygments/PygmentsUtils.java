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
package org.xwiki.rendering.internal.parser.pygments;

import java.util.Map;

import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * Various Pygments related helpers.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
public final class PygmentsUtils
{
    private static final String LANGUAGE_HTML = "html";

    private static final String LANGUAGE_XML = "xml";

    // TODO: make extendable
    private static final Map<SyntaxType, String> SYNTAX_TYPE_MAPPING = Map.of(
    // @formatter:off
        SyntaxType.ANNOTATED_HTML, LANGUAGE_HTML,
        SyntaxType.ANNOTATED_XHTML, LANGUAGE_HTML,
        SyntaxType.HTML, LANGUAGE_HTML,
        SyntaxType.XHTML, LANGUAGE_HTML,
        SyntaxType.CONFLUENCEXHTML, LANGUAGE_HTML
    // @formatter:on
    );

    // TODO: make extendable
    private static final Map<String, String> MIME_MAPPING = Map.of(
    // @formatter:off
        "text/html", LANGUAGE_HTML,
        "application/xhtml+xml", LANGUAGE_HTML,
        "text/javascript", "javascript",
        "application/json", "json",
        "application/ld+json", "jsonld",
        "application/x-httpd-php", "php",
        "application/xml", LANGUAGE_XML,
        "text/xml", LANGUAGE_XML,
        "application/atom+xml", LANGUAGE_XML,
        "text/css", "css"
    // @formatter:on
    );

    private PygmentsUtils()
    {

    }

    /**
     * @param syntax the syntax for which to return a Pygments language
     * @return the language corresponding to the syntax
     */
    public static String syntaxToLanguage(Syntax syntax)
    {
        return syntax != null ? SYNTAX_TYPE_MAPPING.get(syntax.getType()) : null;
    }

    /**
     * @param mimetype the mime type for which to return a Pygments language
     * @return the language corresponding to the syntax mime type
     */
    public static String mimetypeToLanguage(String mimetype)
    {
        return mimetype != null ? MIME_MAPPING.get(mimetype) : null;
    }
}
