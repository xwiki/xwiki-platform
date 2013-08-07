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
import java.util.Map;

import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;

/**
 * Document related events.
 * 
 * @version $Id$
 */
@Unstable
public interface WikiDocumentFilter
{
    // Properties

    /**
     * @type String
     */
    String PROPERTY_SPACE = "document.space";

    /**
     * @type String
     */
    String PROPERTY_NAME = "document.name";

    /**
     * @type String
     */
    String PROPERTY_PARENT = "document.parentreference";

    /**
     * @type String
     */
    String PROPERTY_TITLE = "document.title";

    /**
     * @type Boolean
     */
    String PROPERTY_HIDDEN = "document.hidden";

    /**
     * @type String
     */
    String PROPERTY_CUSTOMCLASS = "document.customclass";

    /**
     * @type String
     */
    String PROPERTY_DEFAULTTEMPLATE = "document.defaulttemplate";

    /**
     * @type String
     */
    String PROPERTY_VALIDATIONSCRIPT = "document.validationscript";

    // content

    /**
     * @type String
     */
    String PROPERTY_CONTENT = "document.content";

    /**
     * @type Syntax
     */
    String PROPERTY_CONTENT_SYNTAX = "document.content.syntax";

    /**
     * @type String
     */
    String PROPERTY_CONTENT_AUTHOR = "document.content.author";

    /**
     * @type Date
     */
    String PROPERTY_CONTENT_DATE = "document.content.date";

    // creation

    /**
     * @type String
     */
    String PROPERTY_CREATION_AUTHOR = "document.creation.creator";

    /**
     * @type Date
     */
    String PROPERTY_CREATION_DATE = "document.creation.date";

    // locale

    /**
     * @type Locale
     */
    String PROPERTY_LOCALE = "document.locale";

    // revision

    /**
     * @type String
     */
    String PROPERTY_REVISION = "document.revision";

    /**
     * @type Date
     */
    String PROPERTY_REVISION_DATE = "document.revision.date";

    /**
     * @type String
     */
    String PROPERTY_REVISION_AUTHOR = "document.revision.author";

    /**
     * @type String
     */
    String PROPERTY_REVISION_COMMENT = "document.revision.comment";

    /**
     * @type Boolean
     */
    String PROPERTY_REVISION_MINOR = "document.revision.minor";

    // Events

    void beginWikiDocument(@Name("name") String name, @Default("") @Name("properties") Map<String, Object> properties)
        throws WikiStreamException;

    void endWikiDocument(@Name("name") String name, @Default("") @Name("properties") Map<String, Object> properties)
        throws WikiStreamException;

    void beginWikiDocumentLocale(@Name("locale") Locale locale,
        @Default("") @Name("properties") Map<String, Object> properties) throws WikiStreamException;

    void endWikiDocumentLocale(@Name("locale") Locale locale,
        @Default("") @Name("properties") Map<String, Object> properties) throws WikiStreamException;

    void beginWikiDocumentRevision(@Name("version") String version,
        @Default("") @Name("properties") Map<String, Object> properties) throws WikiStreamException;

    void endWikiDocumentRevision(@Name("version") String version,
        @Default("") @Name("properties") Map<String, Object> properties) throws WikiStreamException;
}
