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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.xar.internal.model.XarAttachmentModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class XARAttachmentModel extends XarAttachmentModel
{
    // Utils

    public static final Map<String, EventParameter> ATTACHMENT_PARAMETERS = new HashMap<String, EventParameter>()
    {
        {
            put(ELEMENT_MIMETYPE, new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_MIMETYPE));
            put(ELEMENT_CHARSET, new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_CHARSET));
            put(ELEMENT_REVISION, new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION));
            put(ELEMENT_REVISION_AUTHOR, new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION_AUTHOR));
            put(ELEMENT_REVISION_DATE,
                new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION_DATE, Date.class));
            put(ELEMENT_REVISION_COMMENT, new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_REVISION_COMMENT));
            put(ELEMENT_REVISIONS, new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS));
        }
    };
}
