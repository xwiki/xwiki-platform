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
package org.xwiki.observation.remote.internal.converter;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultEventConverterManager}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultEventConverterManagerTest
{
    @InjectMockComponents
    private DefaultEventConverterManager manager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void getLocalEventConvertersWithNoConverter()
    {
        assertEquals(List.of(), this.manager.getLocalEventConverters());
    }

    @Test
    void getLocalEventConvertersWithThreeConverters() throws Exception
    {
        LocalEventConverter converter1 =
            this.componentManager.registerMockComponent(LocalEventConverter.class, "converter1");
        when(converter1.getPriority()).thenReturn(2);
        LocalEventConverter converter2 =
            this.componentManager.registerMockComponent(LocalEventConverter.class, "converter2");
        when(converter2.getPriority()).thenReturn(3);
        LocalEventConverter converter3 =
            this.componentManager.registerMockComponent(LocalEventConverter.class, "converter3");
        when(converter3.getPriority()).thenReturn(1);

        assertEquals(List.of(converter3, converter1, converter2), this.manager.getLocalEventConverters());
    }

    @Test
    void getRemoteEventConvertersWithNoConverter()
    {
        assertEquals(List.of(), this.manager.getRemoteEventConverters());
    }

    @Test
    void getRemoteEventConvertersWithThreeConverters() throws Exception
    {
        RemoteEventConverter converter1 =
            this.componentManager.registerMockComponent(RemoteEventConverter.class, "converter1");
        when(converter1.getPriority()).thenReturn(2);
        RemoteEventConverter converter2 =
            this.componentManager.registerMockComponent(RemoteEventConverter.class, "converter2");
        when(converter2.getPriority()).thenReturn(3);
        RemoteEventConverter converter3 =
            this.componentManager.registerMockComponent(RemoteEventConverter.class, "converter3");
        when(converter3.getPriority()).thenReturn(1);

        assertEquals(List.of(converter3, converter1, converter2), this.manager.getRemoteEventConverters());
    }
}
