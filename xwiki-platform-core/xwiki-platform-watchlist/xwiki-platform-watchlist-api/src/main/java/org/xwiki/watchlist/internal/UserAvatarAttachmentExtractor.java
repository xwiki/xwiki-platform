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
package org.xwiki.watchlist.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.api.Attachment;

/**
 * Get the user's avatar if it is set. If not, get the standard "noAvatar.png" image.
 * <p>
 * In both cases, the resulting image will be resized to a thumbnail version and it will have a fixed name. The
 * resulting {@link Attachment} object will be a fake as to not affect live documents.
 *
 * @version $Id$
 * @since 7.1RC1
 */
@Role
public interface UserAvatarAttachmentExtractor
{
    /**
     * @param userReference the document reference to the profile of the user from which to extract the avatar
     * @return a n {@link Attachment} instance containing the user's avatar or the default ("noavatar.png") image. The
     *         returned instance is a fake as to not affect live documents. Also, the file is resized and renamed to
     *         default values (i.e. 50px, 50px, "<prefixedFullUserName>.png")
     */
    Attachment getUserAvatar(DocumentReference userReference);

    /**
     * @param userReference the document reference to the profile of the user from which to extract the avatar
     * @param width the width, in pixels, of the user's resized avatar image
     * @param height the height, in pixels, of the user's resized avatar image
     * @param fileName the name of the user's resized avatar image
     * @return an {@link Attachment} instance containing the user's avatar or the default ("noavatar.png") image. The
     *         returned instance is a fake as to not affect live documents. Also, the file is resized and renamed to the
     *         specified values.
     */
    Attachment getUserAvatar(DocumentReference userReference, int width, int height, String fileName);
}
