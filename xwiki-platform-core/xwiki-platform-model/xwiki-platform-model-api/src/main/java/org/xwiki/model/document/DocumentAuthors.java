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
 * @since 14.0RC1
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
     * The displayed author is the author of a document that we want to display in the UI. This author should never be
     * used for security reasons as it does not reflect who is responsible of the save performed.
     * For example, this author is different from metadata author when the document is saved through a script
     * wrote by a user without PR rights. Note that this author is optional: whenever it's not specified, it will
     * fallback on the {@link #getMetadataAuthor()}.
     *
     * @return the reference of the user who impulses the creation of the document, without being the real user used
     *          in the APIs for saving the changes.
     */
    UserReference getDisplayedAuthor();

    /**
     * The creator of the document is the first author of the document when it has been created.
     *
     * @return the reference of the user who authored the first version of the document.
     */
    UserReference getCreator();
}
