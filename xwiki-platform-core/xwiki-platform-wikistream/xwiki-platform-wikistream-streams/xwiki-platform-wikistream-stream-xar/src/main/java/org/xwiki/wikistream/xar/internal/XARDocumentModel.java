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
package org.xwiki.wikistream.xar.internal;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class XARDocumentModel
{
    public static final String ELEMENT_DOCUMENT = "xwikidoc";

    public static final String ELEMENT_SPACE = "web";

    public static final String ELEMENT_NAME = "name";

    public static final String ELEMENT_LOCALE = "language";

    public static final String ELEMENT_DEFAULTLOCALE = "defaultLanguage";

    public static final String ELEMENT_ISTRANSLATION = "translation";

    public static final String ELEMENT_PARENT = "parent";

    public static final String ELEMENT_TITLE = "title";

    public static final String ELEMENT_ISHIDDEN = "hidden";

    public static final String ELEMENT_CUSTOMCLASS = "customClass";

    public static final String ELEMENT_DEFAULTTEMPLATE = "defaultTemplate";

    public static final String ELEMENT_VALIDATIONSCRIPT = "validationScript";

    // content

    public static final String ELEMENT_CONTENT = "content";

    public static final String ELEMENT_CONTENT_SYNTAX = "syntaxId";

    public static final String ELEMENT_CONTENT_AUTHOR = "contentAuthor";

    public static final String ELEMENT_CONTENT_DATE = "contentUpdateDate";

    // creation

    public static final String ELEMENT_CREATION_AUTHOR = "creator";

    public static final String ELEMENT_CREATION_DATE = "creationDate";

    // revision

    public static final String ELEMENT_REVISION_DATE = "date";

    public static final String ELEMENT_REVISION_VERSION = "version";

    public static final String ELEMENT_REVISION_AUTHOR = "author";

    public static final String ELEMENT_REVISION_COMMENT = "comment";

    public static final String ELEMENT_REVISION_MINOR = "minorEdit";
    
    public static final String ELEMENT_REVISIONS = "versions";
}
