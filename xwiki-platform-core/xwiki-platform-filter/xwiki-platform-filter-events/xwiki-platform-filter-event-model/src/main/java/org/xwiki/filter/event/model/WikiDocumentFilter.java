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
package org.xwiki.filter.event.model;

import java.util.Locale;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.annotation.Default;
import org.xwiki.stability.Unstable;

/**
 * Document related events.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public interface WikiDocumentFilter
{
    // Log

    /**
     * Mark a log as a document save notification. First log parameter has to be the
     * {@link org.xwiki.model.reference.DocumentReference}.
     * 
     * @since 6.2M1
     */
    Marker LOG_DOCUMENT_CREATED = MarkerFactory.getMarker("filter.document.created");

    /**
     * Mark a log as a document save notification. First log parameter has to be the
     * {@link org.xwiki.model.reference.DocumentReference}.
     * 
     * @since 6.2M1
     */
    Marker LOG_DOCUMENT_UPDATED = MarkerFactory.getMarker("filter.document.updated");

    /**
     * Mark a log as a document delete notification. First log parameter has to be the
     * {@link org.xwiki.model.reference.DocumentReference}.
     * 
     * @since 6.2M1
     */
    Marker LOG_DOCUMENT_DELETED = MarkerFactory.getMarker("filter.document.deleted");

    /**
     * Mark a log as a document skipped notification. First log parameter has to be the
     * {@link org.xwiki.model.reference.DocumentReference}.
     * 
     * @since 6.2M1
     */
    Marker LOG_DOCUMENT_SKIPPED = MarkerFactory.getMarker("filter.document.skipped");

    /**
     * Mark a log as a document error notification. First log parameter has to be the
     * {@link org.xwiki.model.reference.DocumentReference}.
     * 
     * @since 6.2M1
     */
    Marker LOG_DOCUMENT_ERROR = MarkerFactory.getMarker("filter.document.error");

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
     * @since 6.2M1
     */
    String PARAMETER_LASTREVISION = "lastrevision";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_REVISION_DATE = "revision_date";

    /**
     * @type {@link String}
     * @since 14.0RC1
     */
    String PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR = "revision_author";

    /**
     * @type {@link String}
     * @since 14.0RC1
     */
    String PARAMETER_REVISION_ORIGINALMETADATA_AUTHOR = "revision_original_author";

    /**
     * @type {@link String}
     * @deprecated since 14.0RC1, use {@link #PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR} instead
     */
    @Deprecated
    String PARAMETER_REVISION_AUTHOR = PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR;

    /**
     * @type {@link String}
     */
    String PARAMETER_REVISION_COMMENT = "revision_comment";

    /**
     * @type {@link Boolean}
     */
    String PARAMETER_REVISION_MINOR = "revision_minor";

    // required rights
    /**
     * @type {@link Boolean}
     * @since 16.10.0RC1
     */
    @Unstable
    String PARAMETER_ENFORCE_REQUIRED_RIGHTS = "enforce_required_rights";

    // Events

    /**
     * @param name the name part of the {@link org.xwiki.model.reference.DocumentReference}
     * @param parameters the properties of the document
     * @throws FilterException when failing to send event
     */
    void beginWikiDocument(String name, @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters)
        throws FilterException;

    /**
     * @param name the name part of the {@link org.xwiki.model.reference.DocumentReference}
     * @param parameters the properties of the document
     * @throws FilterException when failing to send event
     */
    void endWikiDocument(String name, @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters)
        throws FilterException;

    /**
     * @param locale the locale of the document
     * @param parameters the properties of the document locale
     * @throws FilterException when failing to send event
     */
    void beginWikiDocumentLocale(@Default("") Locale locale,
        @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters) throws FilterException;

    /**
     * @param locale the locale of the document
     * @param parameters the properties of the document
     * @throws FilterException when failing to send event
     */
    void endWikiDocumentLocale(@Default("") Locale locale,
        @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters) throws FilterException;

    /**
     * @param revision the revision of the document
     * @param parameters the properties of the document revision
     * @throws FilterException when failing to send event
     */
    void beginWikiDocumentRevision(String revision,
        @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters) throws FilterException;

    /**
     * @param revision the revision of the document
     * @param parameters the properties of the document revision
     * @throws FilterException when failing to send event
     */
    void endWikiDocumentRevision(String revision,
        @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters) throws FilterException;
}
