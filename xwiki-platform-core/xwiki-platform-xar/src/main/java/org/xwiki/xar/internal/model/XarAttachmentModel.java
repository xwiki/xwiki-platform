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
public class XarAttachmentModel
{
    public static final String ELEMENT_ATTACHMENT = "attachment";

    public static final String ELEMENT_NAME = "filename";

    public static final String ELEMENT_CONTENT_SIZE = "filesize";

    /**
     * @since 7.1M1
     */
    public static final String ELEMENT_MIMETYPE = "mimetype";

    public static final String ELEMENT_CONTENT = "content";

    public static final String ELEMENT_REVISION = "version";

    public static final String ELEMENT_REVISION_AUTHOR = "author";

    public static final String ELEMENT_REVISION_DATE = "date";

    public static final String ELEMENT_REVISION_COMMENT = "comment";

    public static final String ELEMENT_REVISIONS = "versions";
}
