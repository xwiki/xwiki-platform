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
package org.xwiki.resource.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link org.xwiki.resource.entity.EntityResourceAction}.
 *
 * @version $Id$
 * @since 6.1M2
 */
class EntityResourceActionTest
{
    @Test
    void equalsAndHashCode()
    {
        EntityResourceAction id1 = new EntityResourceAction("action");
        EntityResourceAction id2 = new EntityResourceAction("action");
        assertEquals(id1.hashCode(), id2.hashCode());
        assertEquals(id1, id2);
        assertEquals(id1.getActionName(), id2.getActionName());
    }

    @Test
    void fromString()
    {
        EntityResourceAction id = EntityResourceAction.fromString("view");
        assertEquals(EntityResourceAction.VIEW, id);
    }
}
