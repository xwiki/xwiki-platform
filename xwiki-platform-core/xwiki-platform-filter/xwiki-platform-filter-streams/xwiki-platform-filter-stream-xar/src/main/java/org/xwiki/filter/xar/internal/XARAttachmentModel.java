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

import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.xar.internal.model.XarAttachmentModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class XARAttachmentModel extends XarAttachmentModel
{
    /**
     * The map of parameters to be used when reading attachments.
     */
    public static final Map<String, EventParameter> ATTACHMENT_PARAMETERS = new HashMap<>();

    static {
        ATTACHMENT_PARAMETERS.put(ELEMENT_MIMETYPE, new EventParameter(WikiAttachmentFilter.PARAMETER_MIMETYPE));
        ATTACHMENT_PARAMETERS.put(ELEMENT_CHARSET, new EventParameter(WikiAttachmentFilter.PARAMETER_CHARSET));
        ATTACHMENT_PARAMETERS.put(ELEMENT_VERSION, new EventParameter(WikiAttachmentFilter.PARAMETER_REVISION));
        ATTACHMENT_PARAMETERS.put(ELEMENT_REVISION_AUTHOR,
            new EventParameter(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR));
        ATTACHMENT_PARAMETERS.put(ELEMENT_REVISION_DATE,
            new EventParameter(WikiAttachmentFilter.PARAMETER_REVISION_DATE, Date.class));
        ATTACHMENT_PARAMETERS.put(ELEMENT_REVISION_COMMENT,
            new EventParameter(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT));
        ATTACHMENT_PARAMETERS.put(ELEMENT_REVISION_CONTENT_ALIAS,
            new EventParameter(WikiAttachmentFilter.PARAMETER_REVISION_CONTENT_ALIAS));
        ATTACHMENT_PARAMETERS.put(ELEMENT_JRCSVERSIONS,
            new EventParameter(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS));
    }
}
