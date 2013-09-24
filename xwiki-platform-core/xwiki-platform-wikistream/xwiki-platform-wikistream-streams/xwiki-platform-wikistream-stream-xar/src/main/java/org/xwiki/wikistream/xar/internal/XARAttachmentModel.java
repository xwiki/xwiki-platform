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
import java.util.Map;

import org.xwiki.wikistream.xar.internal.XARUtils.Parameter;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiAttachmentFilter;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XARAttachmentModel
{
    public static final String ELEMENT_ATTACHMENT = "attachment";

    public static final String ELEMENT_NAME = "filename";

    public static final String ELEMENT_CONTENT_SIZE = "filesize";

    public static final String ELEMENT_CONTENT = "content";

    public static final String ELEMENT_REVISION = "version";

    public static final String ELEMENT_REVISION_AUTHOR = "author";

    public static final String ELEMENT_REVISION_DATE = "date";

    public static final String ELEMENT_REVISION_COMMENT = "comment";

    public static final String ELEMENT_REVISIONS = "versions";

    // Utils

    public static final Map<String, Parameter> ATTACHMENT_PARAMETERS = new HashMap<String, Parameter>()
    {
        {
            put(ELEMENT_REVISION, new Parameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION));
            put(ELEMENT_REVISION_AUTHOR, new Parameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION_AUTHOR));
            put(ELEMENT_REVISION_DATE, new Parameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION_DATE, Date.class));
            put(ELEMENT_REVISION_COMMENT, new Parameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION_COMMENT));
            put(ELEMENT_REVISIONS, new Parameter(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS));
        }
    };
}
