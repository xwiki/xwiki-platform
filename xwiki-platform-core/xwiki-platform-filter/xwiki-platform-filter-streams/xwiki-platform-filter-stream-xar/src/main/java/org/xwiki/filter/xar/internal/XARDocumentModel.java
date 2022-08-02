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
package org.xwiki.filter.xar.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.xwiki.filter.event.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.xar.internal.model.XarDocumentModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class XARDocumentModel extends XarDocumentModel
{
    // Utils

    public static final Map<String, EventParameter> DOCUMENT_PARAMETERS = new HashMap<String, EventParameter>()
    {
        {
            put(ELEMENT_DEFAULTLOCALE, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_LOCALE, Locale.class));
        }
    };

    public static final Map<String, EventParameter> DOCUMENTLOCALE_PARAMETERS = new HashMap<String, EventParameter>()
    {
        {
            put(ELEMENT_CREATION_AUTHOR, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_CREATION_AUTHOR));
            put(ELEMENT_CREATION_DATE, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_CREATION_DATE, Date.class));
            put(ELEMENT_REVISIONS, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS));
        }
    };

    public static final Map<String, EventParameter> DOCUMENTREVISION_PARAMETERS = new HashMap<String, EventParameter>()
    {
        {
            put(ELEMENT_CONTENT, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT));
            put(ELEMENT_CONTENT_AUTHOR, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT_AUTHOR));
            put(ELEMENT_CONTENT_DATE, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT_DATE, Date.class));
            put(ELEMENT_CONTENT_HTML, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_CONTENT_HTML));
            put(ELEMENT_CUSTOMCLASS, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_CUSTOMCLASS));
            put(ELEMENT_DEFAULTTEMPLATE, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE));
            put(ELEMENT_HIDDEN, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_HIDDEN, Boolean.class));
            put(ELEMENT_PARENT, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_PARENT, EntityReference.class));
            put(ELEMENT_REVISION_EFFECTIVEMEDATAAUTHOR,
                new EventParameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR));
            put(ELEMENT_REVISION_ORIGINALMEDATAAUTHOR,
                new EventParameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_ORIGINALMETADATA_AUTHOR));
            put(ELEMENT_REVISION_COMMENT, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_COMMENT));
            put(ELEMENT_REVISION_DATE, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_DATE, Date.class));
            put(ELEMENT_REVISION_MINOR,
                new EventParameter(XWikiWikiDocumentFilter.PARAMETER_REVISION_MINOR, Boolean.class));
            put(ELEMENT_SYNTAX, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_SYNTAX, Syntax.class));
            put(ELEMENT_TITLE, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_TITLE));
            put(ELEMENT_VALIDATIONSCRIPT, new EventParameter(XWikiWikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT));
        }
    };

    public static final Set<String> DOCUMENT_SKIPPEDPARAMETERS =
        new HashSet<>(Arrays.asList(ELEMENT_ISTRANSLATION, ELEMENT_TEMPLATE));
}
