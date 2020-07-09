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
package org.xwiki.mentions.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;

/**
 * Execution the notifications for the mentions asynchronously.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@Role
public interface MentionsEventExecutor
{
    /**
     * Looks for mentions in the content and creates notifications accordingly.
     *
     * @param documentReference the document reference
     * @param authorReference the author reference
     * @param version the document version
     *
     */
    void execute(DocumentReference documentReference, DocumentReference authorReference, String version);

    /**
     *
     * @return the current size of the queue of mentions to analyze.
     */
    long getQueueSize();

    /**
     * Clear the queue of mentions to analyze. This operation is only allowed for users with 
     * {@link Right#ADMIN} rights. 
     * @exception AccessDeniedException In case of access control error.
     */
    void clearQueue() throws AccessDeniedException;
}
