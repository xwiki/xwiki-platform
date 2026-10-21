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
package org.xwiki.activeinstalls2;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.activeinstalls2.internal.data.Ping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the default methods of {@link DataManager}, which are what an implementation not supporting the
 * distinct counts inherits.
 *
 * @version $Id$
 */
class DataManagerTest
{
    /**
     * A {@link DataManager} implementing only the methods that have no default implementation.
     */
    private static class MinimalDataManager implements DataManager
    {
        @Override
        public List<Ping> searchInstalls(String jsonQuery)
        {
            return List.of();
        }

        @Override
        public long countInstalls(String jsonQuery)
        {
            return 0;
        }
    }

    private final DataManager dataManager = new MinimalDataManager();

    @Test
    void countDistinctInstalls()
    {
        Exception exception = assertThrows(Exception.class, () -> this.dataManager.countDistinctInstalls(null));

        assertEquals(String.format("[%s] doesn't support counting distinct installs",
            MinimalDataManager.class.getName()), exception.getMessage());
    }

    @Test
    void countDistinctInstallsByExtension()
    {
        Exception exception =
            assertThrows(Exception.class, () -> this.dataManager.countDistinctInstallsByExtension(null));

        assertEquals(String.format("[%s] doesn't support counting distinct installs per extension",
            MinimalDataManager.class.getName()), exception.getMessage());
    }
}
