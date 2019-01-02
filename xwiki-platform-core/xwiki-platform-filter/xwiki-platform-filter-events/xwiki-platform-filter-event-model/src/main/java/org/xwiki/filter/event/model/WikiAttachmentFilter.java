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

import java.io.InputStream;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.annotation.Default;

/**
 * Attachment related events.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public interface WikiAttachmentFilter
{
    /**
     * @type {@link String}
     */
    String PARAMETER_NAME = "name";

    /**
     * @type {@link String}
     * @since 7.1M1
     */
    String PARAMETER_MIMETYPE = "mimetype";

    /**
     * @type {@link String}
     * @since 10.11RC1
     */
    String PARAMETER_CHARSET = "charset";

    /**
     * @type {@link String}
     */
    String PARAMETER_CONTENT_TYPE = "content_type";

    /**
     * @type {@link String}
     */
    String PARAMETER_CREATION_AUTHOR = "creation_author";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_CREATION_DATE = "creation_date";

    /**
     * @type {@link String}
     */
    String PARAMETER_REVISION = "revision";

    /**
     * @type {@link String}
     */
    String PARAMETER_REVISION_AUTHOR = "revision_author";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_REVISION_DATE = "revision_date";

    /**
     * @type {@link String}
     */
    String PARAMETER_REVISION_COMMENT = "revision_comment";

    /**
     * @param name the name of the attachment
     * @param content the binary content of the attachment
     * @param size the size of the attachment
     * @param parameters the properties of the attachment
     * @throws FilterException when failing to send event
     */
    void onWikiAttachment(String name, InputStream content, Long size,
        @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters)
        throws FilterException;
}
