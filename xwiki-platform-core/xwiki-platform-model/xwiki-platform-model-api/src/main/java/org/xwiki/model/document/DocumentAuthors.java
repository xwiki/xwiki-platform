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
package org.xwiki.model.document;

import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Define the different authors information that are related to an XWiki Document.
 *
 * @version $Id$
 * @since 13.10RC1
 */
@Unstable
public interface DocumentAuthors
{
    /**
     * The content author is the author of the content field only: this author is used for security reason when a script
     * needs to be executed.
     *
     * @return the reference of the user who authored latest content of the document.
     */
    UserReference getContentAuthor();

    /**
     * The metadata author is the author responsible of any change but the content: saving a new xobject in a document
     * update the metadata auhor but not the content author.
     *
     * @return the reference of the user who authored metadata of the document.
     */
    UserReference getMetadataAuthor();

    /**
     * The displayed author is the "real" author of a document as "the one who impulses the changes". However this
     * author is only there for display purpose: it should never be used for security reasons. This author is different
     * from metadata or content author when the document is saved through a script or a mechanism which delegates the
     * save of the document.
     *
     * @return the reference of the user who impulses the creation of the document, without being the real user used
     *          in the APIs for saving the changes.
     */
    UserReference getDisplayedAuthor();
}
