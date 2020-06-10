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
package org.xwiki.mentions;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * A service to send mentions notification.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Role
@Unstable
public interface MentionNotificationService
{
    /**
     * Send a notification on behalf of the author, informing the mentioned user that he/she is mentioned on the a page.
     *
     * @param authorReference the reference of the author of the mention.
     * @param documentReference the document in which the mention has been done.
     * @param mentionedIdentity the identity of the mentioned user.
     * @param location The location of the mention.
     * @param anchorId The anchor link to use.
     */
    void sendNotif(DocumentReference authorReference, DocumentReference documentReference,
        DocumentReference mentionedIdentity, MentionLocation location, String anchorId);
}
