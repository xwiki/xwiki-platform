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
package org.xwiki.resource;

import org.junit.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ActionId}.
 *
 * @version
 *
 * @since 6.0M1
 */
public class ActionIdTest
{
    @Test
    public void equalsAndHashCode()
    {
        ActionId id1 = new ActionId("id");
        ActionId id2 = new ActionId("id");
        assertEquals(id1.hashCode(), id2.hashCode());
        assertEquals(id1, id2);
        assertEquals(id1.getId(), id2.getId());
    }

    @Test
    public void fromString()
    {
        ActionId id = ActionId.fromString("view");
        assertEquals(ActionId.VIEW, id);
    }
}
