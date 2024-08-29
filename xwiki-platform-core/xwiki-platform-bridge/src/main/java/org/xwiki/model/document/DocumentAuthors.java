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

import org.xwiki.user.UserReference;

/**
 * Define the different authors information that are related to an XWikiDocument.
 * Note that since this API uses {@link UserReference} the getters should never returns {@code null} but should return
 * {@link org.xwiki.user.GuestUserReference} instead when needed.
 *
 * @version $Id$
 * @since 14.0RC1
 */
public interface DocumentAuthors
{
    /**
     * The last user that has changed the document's content (ie not object, attachments). The Content author is only
     * changed when the document content changes. Note that Content Author is used to check programming rights on a
     * document and this is the reason we need to know the last author who's modified the content since programming
     * rights depend on this.
     * Note that this should never return {@code null}.
     *
     * @return the reference of the user who authored latest content of the document.
     */
    UserReference getContentAuthor();

    /**
     * Specify the author of the content of the document: this author is only responsible to the content, and not to
     * other information of the document such as xobjects.
     * The last user that has changed the document's content (ie not object, attachments). The Content author is only
     * changed when the document content changes. Note that Content Author is used to check programming rights on a
     * document and this is the reason we need to know the last author who's modified the content since programming
     * rights depend on this.
     *
     * @param contentAuthor the author of the content of the document.
     * @see #getContentAuthor()
     */
    void setContentAuthor(UserReference contentAuthor);

    /**
     * The effective metadata author is the author responsible of any change but the content: e.g. saving a new xobject
     * in a document update the metadata auhor but not the content author. It's effective, as it is the actual author
     * who holds the responsibility of the changes in terms of rights.
     * Note that this should never return {@code null}.
     *
     * @return the reference of the user who saved metadata of the document, or who was used to save it.
     */
    UserReference getEffectiveMetadataAuthor();

    /**
     * Specify the metadata author of the document: this author is not responsible to the content, but responsible to
     * the xobjects and other metadata.
     *
     * @param metadataAuthor the author of the metadata of the document.
     * @see #getEffectiveMetadataAuthor()
     */
    void setEffectiveMetadataAuthor(UserReference metadataAuthor);

    /**
     * The original metadata author is the author who have triggered a save on the document, but who does not hold
     * responsibility for those changes in terms of rights. It should be different from the
     * {@link #getEffectiveMetadataAuthor()} when the save is performed through a mechanism which prevent users to need
     * edit rights for saving the document (e.g. adding a new comment in a page).
     * Note that this should never return {@code null}.
     *
     * @return the reference of the user who originally triggers the save.
     */
    UserReference getOriginalMetadataAuthor();

    /**
     * Specify the original metadata author of the document: the author who have triggered a save on the document,
     * but who does not hold responsibility for those changes in terms of rights.
     *
     * @param originalMetadataAuthor the author to display.
     * @see #getOriginalMetadataAuthor()
     */
    void setOriginalMetadataAuthor(UserReference originalMetadataAuthor);

    /**
     * The creator of the document is the first author of the document when it has been created.
     * Note that this should never return {@code null}.
     *
     * @return the reference of the user who authored the first version of the document.
     */
    UserReference getCreator();

    /**
     * Specify the original creator of the document.
     *
     * @param creator the creator of the document.
     */
    void setCreator(UserReference creator);

    /**
     * Copy all authors from the given document authors.
     *
     * @param documentAuthors the authors to copy value from.
     */
    void copyAuthors(DocumentAuthors documentAuthors);
}
