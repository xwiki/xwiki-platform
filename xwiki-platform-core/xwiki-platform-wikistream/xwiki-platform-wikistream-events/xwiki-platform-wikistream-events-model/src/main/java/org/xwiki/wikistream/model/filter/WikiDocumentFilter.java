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
     * @type Syntax
     */
    String PARAMETER_SYNTAX = "syntax";

    /**
     * @type EntityReference
     */
    String PARAMETER_PARENT = "parent_reference";

    /**
     * @type String
     */
    String PARAMETER_TITLE = "title";

    /**
     * @type Boolean
     */
    String PARAMETER_HIDDEN = "hidden";

    /**
     * @type String
     */
    String PARAMETER_CUSTOMCLASS = "customclass";

    /**
     * @type String
     */
    String PARAMETER_DEFAULTTEMPLATE = "defaulttemplate";

    /**
     * @type String
     */
    String PARAMETER_VALIDATIONSCRIPT = "validationscript";

    // content

    /**
     * @type String
     */
    String PARAMETER_CONTENT = "content";

    /**
     * @type String
     */
    String PARAMETER_CONTENT_HTML = "content_html";

    /**
     * @type String
     */
    String PARAMETER_CONTENT_AUTHOR = "content_author";

    /**
     * @type Date
     */
    String PARAMETER_CONTENT_DATE = "content_date";

    // creation

    /**
     * @type String
     */
    String PARAMETER_CREATION_AUTHOR = "creation_author";

    /**
     * @type Date
     */
    String PARAMETER_CREATION_DATE = "creation_date";

    // locale

    /**
     * @type Locale
     */
    String PARAMETER_LOCALE = "locale";

    // revision

    /**
     * @type String
     */
    String PARAMETER_REVISION = "revision";

    /**
     * @type Date
     */
    String PARAMETER_REVISION_DATE = "revision_date";

    /**
     * @type String
     */
    String PARAMETER_REVISION_AUTHOR = "revision_author";

    /**
     * @type String
     */
    String PARAMETER_REVISION_COMMENT = "revision_comment";

    /**
     * @type Boolean
     */
    String PARAMETER_REVISION_MINOR = "revision_minor";

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
