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
package org.xwiki.wikistream.filter;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;

/**
 * Attachment related events.
 * 
 * @version $Id$
 */
@Unstable
public interface WikiAttachmentFilter
{
    /**
     * @type String
     */
    String PARAMETER_NAME = "attachment_name";

    /**
     * @type String
     */
    String PARAMETER_REVISION_AUTHOR = "attachment_revision_author";

    /**
     * @type Date
     */
    String PARAMETER_REVISION_DATE = "attachment_revision_date";

    /**
     * @type String
     */
    String PARAMETER_REVISION_VERSION = "attachment_revision_version";

    /**
     * @type String
     */
    String PARAMETER_REVISION_COMMENT = "attachment_revision_comment";

    /**
     * @type String
     */
    String PARAMETER_CONTENT = "attachment_content";

    void beginWikiAttachment(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void beginWikiAttachmentRevision(@Name("version") String version,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void endWikiAttachmentRevision(@Name("version") String version,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void endWikiAttachment(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;
}
