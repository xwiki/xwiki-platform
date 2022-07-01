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
package org.xwiki.eventstream.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Validate {@link DefaultEventStatus}.
 * 
 * @version $Id$
 */
class DefaultEventStatusTest
{
    @Test
    void equalsANDhashCode()
    {
        DefaultEventStatus eventStatus1 = new DefaultEventStatus(new DefaultEvent(), "entity1", false);
        DefaultEventStatus eventStatus2 = new DefaultEventStatus(new DefaultEvent(), "entity2", true);
        DefaultEventStatus eventStatus3 = new DefaultEventStatus(new DefaultEvent(), "entity1", true);
        DefaultEventStatus eventStatus1bis = new DefaultEventStatus(new DefaultEvent(), "entity1", false);

        assertNotEquals(eventStatus1, eventStatus2);
        assertNotEquals(eventStatus1.hashCode(), eventStatus2.hashCode());
        
        assertNotEquals(eventStatus1, eventStatus3);
        assertNotEquals(eventStatus1.hashCode(), eventStatus3.hashCode());

        assertEquals(eventStatus1, eventStatus1bis);
        assertEquals(eventStatus1.hashCode(), eventStatus1bis.hashCode());
    }
}
