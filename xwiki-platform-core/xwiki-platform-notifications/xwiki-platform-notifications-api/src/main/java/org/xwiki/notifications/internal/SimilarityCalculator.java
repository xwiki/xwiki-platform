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
package org.xwiki.notifications.internal;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;

/**
 * Compute similarity between two events.
 *
 * Our computation is different than the way Event Stream set the "groupId" field of the events.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component(roles = SimilarityCalculator.class)
@Singleton
public class SimilarityCalculator
{
    /**
     * The 2 events have the same groupId and the same document but different types.
     *
     * The idea is to group events with different types (like "addComment" and "update") but concerning
     * the same document.
     *
     * If they have the same type, they will be grouped anyway so we don't need to check the groupId.
     *
     * Example: * Event1: "update" on docA with groupA (legit update)
     *          * Event2: "update" on docA with groupA (auto watch listener: adding "docA" to the list of watched pages)
     *          * Event3: "update" on docA with groupB (legit update)
     *
     * If you check the groupId first, you will group Event 1 & 2, and let the Event 3 alone.
     *
     * If you check if the groupIds match only when the event types are different, you will
     * group Event 1, 2 & 3 together.
     */
    public static final int SAME_GROUP_ID_AND_DOCUMENT_BUT_DIFFERENT_TYPES = 10000;

    /**
     * The 2 events have the same type and concern the same document.
     */
    public static final int SAME_DOCUMENT_AND_TYPE = 1000;

    /**
     * The 2 events have the same type but no document is concerned.
     */
    public static final int SAME_TYPE_BUT_NO_DOCUMENT = 10;

    /**
     * The 2 events are totally different.
     */
    public static final int NO_SIMILARITY = 0;

    /**
     * Compute the similarity between two events.
     *
     * @param event1 first event to compare
     * @param event2 second event to compare
     *
     * @return an integer representing the similarity between the two events
     */
    public int computeSimilarity(Event event1, Event event2)
    {
        if (event1.getDocument() != null && event1.getDocument().equals(event2.getDocument())) {
            if (event1.getType() != null && event1.getType().equals(event2.getType())) {
                return SAME_DOCUMENT_AND_TYPE;
            } else if (event1.getGroupId() != null && event1.getGroupId().equals(event2.getGroupId())) {
                return SAME_GROUP_ID_AND_DOCUMENT_BUT_DIFFERENT_TYPES;
            }
        } else if (event1.getDocument() == null && event1.getType() != null
                && event1.getType().equals(event2.getType())) {
            return SAME_TYPE_BUT_NO_DOCUMENT;
        }

        return NO_SIMILARITY;
    }
}
