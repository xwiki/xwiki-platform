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
package org.xwiki.extension.security.internal.analyzer.osv.model.response;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test of {@link RangeObject}.
 *
 * @version $Id$
 */
class RangeObjectTest
{
    private final RangeObject rangeObject = new RangeObject();

    @Test
    void getEndNull()
    {
        this.rangeObject.setEvents(List.of(new EventObject()));
        assertNull(this.rangeObject.getEnd());
    }
    
    @Test
    void getEndNullHasFixed()
    {
        EventObject eventObject = new EventObject();
        eventObject.setFixed("fix");
        this.rangeObject.setEvents(List.of(new EventObject(), eventObject));
        assertEquals("fix", this.rangeObject.getEnd());
    }

    @Test
    void getEndNullHasLastAffected()
    {
        EventObject eventObject = new EventObject();
        eventObject.setLastAffected("last");
        this.rangeObject.setEvents(List.of(new EventObject(), new EventObject(), eventObject));
        assertEquals("last", this.rangeObject.getEnd());
    }

}
