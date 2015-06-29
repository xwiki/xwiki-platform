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
package org.xwiki.xar.internal.model;

/**
 * @version $Id$
 * @since 5.4M1
 */
public class XarDocumentModel
{
    /**
     * Never really used. It's supposedly what an old format would have used.
     */
    public static final String VERSION_10 = "1.0";

    /**
     * Not real change, it only change the order of things.
     */
    public static final String VERSION_11 = "1.1";

    /**
     * Add support for nested spaces (see {@link #ATTRIBUTE_DOCUMENT_REFERENCE}).
     */
    public static final String VERSION_12 = "1.2";

    public static final String ELEMENT_DOCUMENT = "xwikidoc";

    /**
     * Contains the version of the specification.
     * 
     * @since 7.2M1
     */
    public static final String ATTRIBUTE_DOCUMENT_SPECVERSION = "version";

    /**
     * Contains the complete reference of the document.
     * 
     * @since 7.2M1
     */
    public static final String ATTRIBUTE_DOCUMENT_REFERENCE = "reference";

    /**
     * Contains the locale of the document.
     * 
     * @since 7.2M1
     */
    public static final String ATTRIBUTE_DOCUMENT_LOCALE = "locale";

    /**
     * @since 7.2M1, use {@link #ATTRIBUTE_DOCUMENT_SPECVERSION} instead
     */
    @Deprecated
    public static final String ATTRIBUTE_STREAMVERSION = ATTRIBUTE_DOCUMENT_SPECVERSION;

    public static final String ELEMENT_SPACE = "web";

    public static final String ELEMENT_NAME = "name";

    public static final String ELEMENT_LOCALE = "language";

    public static final String ELEMENT_DEFAULTLOCALE = "defaultLanguage";

    public static final String ELEMENT_ISTRANSLATION = "translation";

    public static final String ELEMENT_PARENT = "parent";

    public static final String ELEMENT_TITLE = "title";

    public static final String ELEMENT_HIDDEN = "hidden";

    public static final String ELEMENT_CUSTOMCLASS = "customClass";

    public static final String ELEMENT_DEFAULTTEMPLATE = "defaultTemplate";

    public static final String ELEMENT_VALIDATIONSCRIPT = "validationScript";

    public static final String ELEMENT_SYNTAX = "syntaxId";

    // content

    public static final String ELEMENT_CONTENT = "content";

    public static final String ELEMENT_CONTENT_AUTHOR = "contentAuthor";

    public static final String ELEMENT_CONTENT_DATE = "contentUpdateDate";

    public static final String ELEMENT_CONTENT_HTML = "renderedcontent";

    // creation

    public static final String ELEMENT_CREATION_AUTHOR = "creator";

    public static final String ELEMENT_CREATION_DATE = "creationDate";

    // revision

    public static final String ELEMENT_REVISION_DATE = "date";

    public static final String ELEMENT_REVISION = "version";

    public static final String ELEMENT_REVISION_AUTHOR = "author";

    public static final String ELEMENT_REVISION_COMMENT = "comment";

    public static final String ELEMENT_REVISION_MINOR = "minorEdit";

    public static final String ELEMENT_REVISIONS = "versions";
}
