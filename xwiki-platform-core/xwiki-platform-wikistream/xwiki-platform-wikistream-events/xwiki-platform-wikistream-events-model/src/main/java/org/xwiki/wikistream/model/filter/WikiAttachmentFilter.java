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
package org.xwiki.wikistream.model.filter;

import java.io.InputStream;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;

/**
 * Attachment related events.
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public interface WikiAttachmentFilter
{
    /**
     * @type String
     */
    String PARAMETER_NAME = "name";

    /**
     * @type String
     */
    String PARAMETER_REVISION_AUTHOR = "revision_author";

    /**
     * @type Date
     */
    String PARAMETER_REVISION_DATE = "revision_date";

    /**
     * @type String
     */
    String PARAMETER_REVISION = "revision_version";

    /**
     * @type String
     */
    String PARAMETER_REVISION_COMMENT = "revision_comment";

    void onWikiAttachment(@Name("name") String name, @Name("content") InputStream content, @Name("size") Long size,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;
}
