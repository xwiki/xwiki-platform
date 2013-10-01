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

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wikistream.xar.internal.XARUtils.Parameter;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiDocumentFilter;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XARDocumentModel
{
    public static final String ELEMENT_DOCUMENT = "xwikidoc";

    public static final String ATTRIBUTE_STREAMVERSION = "version";

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

    // Utils

    public static final Map<String, Parameter> DOCUMENT_PARAMETERS = new HashMap<String, Parameter>()
    {
        {
            put(ELEMENT_DEFAULTLOCALE, new Parameter(XWikiWikiDocumentFilter.PARAMETER_LOCALE, Locale.class));
        }
    };

    public static final Map<String, Parameter> DOCUMENTLOCALE_PARAMETERS = new HashMap<String, Parameter>()
    {
        {
            put(ELEMENT_CREATION_AUTHOR, new Parameter(XWikiWikiDocumentFilter.PARAMETER_CREATION_AUTHOR));
            put(ELEMENT_CREATION_DATE, new Parameter(XWikiWikiDocumentFilter.PARAMETER_CREATION_DATE, Date.class));
            put(ELEMENT_REVISIONS, new Parameter(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS));
        }
    };

    public static final Map<String, Parameter> DOCUMENTREVISION_PARAMETERS = new HashMap<String, Parameter>()
    {
        {
            put(ELEMENT_CONTENT, new Parameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT));
            put(ELEMENT_CONTENT_AUTHOR, new Parameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT_AUTHOR));
            put(ELEMENT_CONTENT_DATE, new Parameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT_DATE, Date.class));
            put(ELEMENT_CONTENT_HTML, new Parameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT_HTML));
            put(ELEMENT_CUSTOMCLASS, new Parameter(XWikiWikiDocumentFilter.PARAMETER_CUSTOMCLASS));
            put(ELEMENT_DEFAULTTEMPLATE, new Parameter(XWikiWikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE));
            put(ELEMENT_HIDDEN, new Parameter(XWikiWikiDocumentFilter.PARAMETER_HIDDEN, Boolean.class));
            put(ELEMENT_PARENT, new Parameter(XWikiWikiDocumentFilter.PARAMETER_PARENT));
            put(ELEMENT_REVISION_AUTHOR, new Parameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_AUTHOR));
            put(ELEMENT_REVISION_COMMENT, new Parameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_COMMENT));
            put(ELEMENT_REVISION_DATE, new Parameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_DATE, Date.class));
            put(ELEMENT_REVISION_MINOR, new Parameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_MINOR, Boolean.class));
            put(ELEMENT_SYNTAX, new Parameter(XWikiWikiDocumentFilter.PARAMETER_SYNTAX, Syntax.class));
            put(ELEMENT_TITLE, new Parameter(XWikiWikiDocumentFilter.PARAMETER_TITLE));
            put(ELEMENT_VALIDATIONSCRIPT, new Parameter(XWikiWikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT));
        }
    };
}
