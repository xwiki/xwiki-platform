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

import java.util.concurrent.ThreadPoolExecutor;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.stability.Unstable;

/**
 * Execution the notifications for the mentions asynchronously.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@Unstable
@Role
public interface MentionsEventExecutor
{
    /**
     * Looks for mentions in the xdom and creates notifications accordingly. 
     * @param xdom The xdom
     * @param authorReference The author reference
     * @param documentReference The document reference
     * @param location The location
     */
    void executeCreate(XDOM xdom, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location);

    /**
     * Looks for mentions in the content and creates notifications accordingly.
     * @param content The content
     * @param authorReference The author reference
     * @param documentReference The document reference
     * @param location The location
     */
    void executeCreate(String content, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location);

    /**
     * Looks for new mentions in the newXdom (by comparing it to oldXdom) and creates notifications accordingly.
     * @param oldXdom The old XDOM.
     * @param newXdom The new XDOM.
     * @param authorReference The author reference.
     * @param documentReference The location reference.
     * @param location The mention location.
     */
    void executeUpdate(XDOM oldXdom, XDOM newXdom, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location);

    /**
     * Looks for new mentions in the newContent (by comparing it to oldContent) and creates notifications accordingly.
     * @param oldContent The old content.
     * @param newContent The new content.
     * @param authorReference The author reference.
     * @param documentReference The location reference.
     * @param location The mention location.
     */
    void executeUpdate(String oldContent, String newContent, DocumentReference authorReference,
        DocumentReference documentReference, MentionLocation location);

    /**
     *
     * @return the current size of the queue of mentions to analyze.
     */
    long getQueueSize();

    /**
     * Clear the queue of mentions to analyze.
     */
    void clearQueue();

    /**
     * Set the thread pool executor.
     *
     * @param executor The thread pool executor
     */
    void setExecutor(ThreadPoolExecutor executor);
}
