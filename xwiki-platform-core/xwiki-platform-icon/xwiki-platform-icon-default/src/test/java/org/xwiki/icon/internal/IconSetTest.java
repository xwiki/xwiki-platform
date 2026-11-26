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
package org.xwiki.icon.internal;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link org.xwiki.icon.IconSet}.
 *
 * @since 6.3RC1
 * @version $Id$
 */
class IconSetTest
{
    @Test
    void simpleTest()
    {
        IconSet iconSet = new IconSet("myIconSet");
        assertEquals("myIconSet", iconSet.getName());
        iconSet.setName("newName");
        assertEquals("newName", iconSet.getName());
    }

    @Test
    void getIconNamesWithoutDeprecated()
    {
        IconSet iconSet = new IconSet("myIconSet");
        iconSet.addIcon("table-sort", new Icon("table_sort"));
        Icon deprecatedIcon = new Icon("table_sort");
        deprecatedIcon.setDeprecated(true);
        iconSet.addIcon("table_sort", deprecatedIcon);

        assertEquals(List.of("table-sort"), iconSet.getIconNames(false));
        assertEquals(Set.of("table-sort", "table_sort"), Set.copyOf(iconSet.getIconNames(true)));
        assertEquals(Set.of("table-sort", "table_sort"), Set.copyOf(iconSet.getIconNames()));
    }
}
