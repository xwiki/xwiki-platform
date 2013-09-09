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
package org.xwiki.wikistream.filter;

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
     * @type String
     */
    String PARAMETER_SPACE = "space";

    /**
     * @type String
     */
    String PARAMETER_NAME = "document_name";

    /**
     * @type Syntax
     */
    String PARAMETER_SYNTAX = "document_syntax";

    /**
     * @type String
     */
    String PARAMETER_PARENT = "document_parent_reference";

    /**
     * @type String
     */
    String PARAMETER_TITLE = "document_title";

    /**
     * @type Boolean
     */
    String PARAMETER_HIDDEN = "document_hidden";

    /**
     * @type String
     */
    String PARAMETER_CUSTOMCLASS = "document_customclass";

    /**
     * @type String
     */
    String PARAMETER_DEFAULTTEMPLATE = "document_defaulttemplate";

    /**
     * @type String
     */
    String PARAMETER_VALIDATIONSCRIPT = "document_validationscript";

    // content

    /**
     * @type String
     */
    String PARAMETER_CONTENT = "document_content";

    /**
     * @type String
     */
    String PARAMETER_CONTENT_HTML = "document_content_html";

    /**
     * @type String
     */
    String PARAMETER_CONTENT_AUTHOR = "document_content_author";

    /**
     * @type Date
     */
    String PARAMETER_CONTENT_DATE = "document_content_date";

    // creation

    /**
     * @type String
     */
    String PARAMETER_CREATION_AUTHOR = "document_creation_author";

    /**
     * @type Date
     */
    String PARAMETER_CREATION_DATE = "document_creation_date";

    // locale

    /**
     * @type Locale
     */
    String PARAMETER_LOCALE = "document_locale";

    // revision

    /**
     * @type String
     */
    String PARAMETER_REVISION = "document_revision";

    /**
     * @type Date
     */
    String PARAMETER_REVISION_DATE = "document_revision_date";

    /**
     * @type String
     */
    String PARAMETER_REVISION_AUTHOR = "document_revision_author";

    /**
     * @type String
     */
    String PARAMETER_REVISION_COMMENT = "document_revision_comment";

    /**
     * @type Boolean
     */
    String PARAMETER_REVISION_MINOR = "document_revision_minor";

    // Events

    void beginWikiDocument(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void endWikiDocument(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void beginWikiDocumentLocale(@Default("") @Name("locale") Locale locale,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void endWikiDocumentLocale(@Default("") @Name("locale") Locale locale,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void beginWikiDocumentRevision(@Name("version") String version,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void endWikiDocumentRevision(@Name("version") String version,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;
}
