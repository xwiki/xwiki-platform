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
import org.xwiki.text.StringUtils;

/**
 * @version $Id$
 */
@Component(roles = SimilarityCalculator.class)
@Singleton
public class SimilarityCalculator
{
    private static final int SAME_GROUP_ID = 10000;

    private static final int SAME_DOCUMENT_AND_TYPE = 1000;

    private static final int SAME_DOCUMENT = 100;

    private static final int SAME_TYPE_BUT_NO_DOCUMENT = 10;

    private static final int NO_SIMILARITY = 0;

    public int computeSimilarity(Event event1, Event event2)
    {
        if (event1.getDocument() != null && event1.getDocument().equals(event2.getDocument())) {
            if (StringUtils.equals(event1.getGroupId(), event2.getGroupId())) {
                return SAME_GROUP_ID;
            }
            if (StringUtils.equals(event1.getType(), event2.getType())) {
                return SAME_DOCUMENT_AND_TYPE;
            }
            // TODO: TO REMOVE BECAUSE IT HAS BAD CONSEQUENCE
            return SAME_DOCUMENT;
        } else if (event1.getDocument() == null && StringUtils.equals(event1.getType(), event2.getType())) {
            return SAME_TYPE_BUT_NO_DOCUMENT;
        }

        return NO_SIMILARITY;
    }
}
