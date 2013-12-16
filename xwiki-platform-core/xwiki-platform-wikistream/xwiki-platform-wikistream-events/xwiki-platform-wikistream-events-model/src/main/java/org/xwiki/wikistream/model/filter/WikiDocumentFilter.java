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
package org.xwiki.wikistream.model.filter;

import java.util.Locale;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;

/**
 * Document related events.
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public interface WikiDocumentFilter
{
    // Properties

    /**
     * @type {@link org.xwiki.rendering.syntax.Syntax}
     */
    String PARAMETER_SYNTAX = "syntax";

    /**
     * @type {@link org.xwiki.model.reference.EntityReference}
     */
    String PARAMETER_PARENT = "parent_reference";

    /**
     * @type {@link String}
     */
    String PARAMETER_TITLE = "title";

    /**
     * @type {@link Boolean}
     */
    String PARAMETER_HIDDEN = "hidden";

    /**
     * @type {@link String}
     */
    String PARAMETER_CUSTOMCLASS = "customclass";

    /**
     * @type {@link String}
     */
    String PARAMETER_DEFAULTTEMPLATE = "defaulttemplate";

    /**
     * @type {@link String}
     */
    String PARAMETER_VALIDATIONSCRIPT = "validationscript";

    // content

    /**
     * @type {@link String}
     */
    String PARAMETER_CONTENT = "content";

    /**
     * @type {@link String}
     */
    String PARAMETER_CONTENT_HTML = "content_html";

    /**
     * @type {@link String}
     */
    String PARAMETER_CONTENT_AUTHOR = "content_author";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_CONTENT_DATE = "content_date";

    // creation

    /**
     * @type {@link String}
     */
    String PARAMETER_CREATION_AUTHOR = "creation_author";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_CREATION_DATE = "creation_date";

    // locale

    /**
     * @type {@link Locale}
     */
    String PARAMETER_LOCALE = "locale";

    // revision

    /**
     * @type {@link String}
     * @since 5.4M1
     */
    String PARAMETER_LASTREVISION = "lastrevision";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_REVISION_DATE = "revision_date";

    /**
     * @type {@link String}
     */
    String PARAMETER_REVISION_AUTHOR = "revision_author";

    /**
     * @type {@link String}
     */
    String PARAMETER_REVISION_COMMENT = "revision_comment";

    /**
     * @type {@link Boolean}
     */
    String PARAMETER_REVISION_MINOR = "revision_minor";

    // Events

    /**
     * @param name the name of the document
     * @param parameters the properties of the document
     * @throws WikiStreamException when failing to send event
     */
    void beginWikiDocument(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    /**
     * @param name the name of the document
     * @param parameters the properties of the document
     * @throws WikiStreamException when failing to send event
     */
    void endWikiDocument(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    /**
     * @param locale the locale of the document
     * @param parameters the properties of the document locale
     * @throws WikiStreamException when failing to send event
     */
    void beginWikiDocumentLocale(@Default("") @Name("locale") Locale locale,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    /**
     * @param locale the locale of the document
     * @param parameters the properties of the document
     * @throws WikiStreamException when failing to send event
     */
    void endWikiDocumentLocale(@Default("") @Name("locale") Locale locale,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    /**
     * @param revision the revision of the document
     * @param parameters the properties of the document revision
     * @throws WikiStreamException when failing to send event
     */
    void beginWikiDocumentRevision(@Name("revision") String revision,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    /**
     * @param revision the revision of the document
     * @param parameters the properties of the document revision
     * @throws WikiStreamException when failing to send event
     */
    void endWikiDocumentRevision(@Name("revision") String revision,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;
}
