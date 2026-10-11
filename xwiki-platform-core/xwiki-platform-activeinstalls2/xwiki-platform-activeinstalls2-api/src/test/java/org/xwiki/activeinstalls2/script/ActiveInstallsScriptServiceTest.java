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
package org.xwiki.activeinstalls2.script;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

import org.junit.jupiter.api.Test;
import org.xwiki.activeinstalls2.DataManager;
import org.xwiki.activeinstalls2.TooManyExtensionsException;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ActiveInstallsScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class ActiveInstallsScriptServiceTest
{
    private static final String QUERY = "{ \"term\" : { \"distribution.instanceId\" : \"id\" } }";

    @InjectMockComponents
    private ActiveInstallsScriptService scriptService;

    @MockComponent
    private DataManager dataManager;

    @Test
    void countInstalls() throws Exception
    {
        when(this.dataManager.countInstalls(QUERY)).thenReturn(42L);

        assertEquals(42L, this.scriptService.countInstalls(QUERY));
    }

    @Test
    void searchInstalls() throws Exception
    {
        List<Ping> pings = List.of(new Ping());
        when(this.dataManager.searchInstalls(QUERY)).thenReturn(pings);

        assertSame(pings, this.scriptService.searchInstalls(QUERY));
    }

    @Test
    void countDistinctInstalls() throws Exception
    {
        when(this.dataManager.countDistinctInstalls(QUERY)).thenReturn(7L);

        assertEquals(7L, this.scriptService.countDistinctInstalls(QUERY));
    }

    @Test
    void countDistinctInstallsByExtension() throws Exception
    {
        SequencedMap<String, Long> counts = new LinkedHashMap<>(Map.of("extensionId", 3L));
        when(this.dataManager.countDistinctInstallsByExtension(QUERY)).thenReturn(counts);

        assertSame(counts, this.scriptService.countDistinctInstallsByExtension(QUERY));
    }

    @Test
    void countDistinctInstallsWhenError() throws Exception
    {
        when(this.dataManager.countDistinctInstalls(QUERY)).thenThrow(new Exception("error"));

        Exception exception = assertThrows(Exception.class, () -> this.scriptService.countDistinctInstalls(QUERY));

        assertEquals("error", exception.getMessage());
    }

    @Test
    void countDistinctInstallsByExtensionWhenTooManyExtensions() throws Exception
    {
        when(this.dataManager.countDistinctInstallsByExtension(QUERY))
            .thenThrow(new TooManyExtensionsException(42));

        // Verify that the typed exception reaches the caller as is, so that it can tell that case apart from a
        // failure to reach the data.
        TooManyExtensionsException exception = assertThrows(TooManyExtensionsException.class,
            () -> this.scriptService.countDistinctInstallsByExtension(QUERY));

        assertEquals("Found more extensions than the [42] that can be counted at once.", exception.getMessage());
    }
}
