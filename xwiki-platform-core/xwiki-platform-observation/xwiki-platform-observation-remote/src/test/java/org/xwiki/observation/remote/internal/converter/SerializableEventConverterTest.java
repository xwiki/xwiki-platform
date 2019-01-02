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

import org.junit.jupiter.api.Test;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link SerializableEventConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
public class SerializableEventConverterTest
{
    @InjectMockComponents(role = RemoteEventConverter.class)
    private SerializableEventConverter remoteConverter;

    @InjectMockComponents(role = LocalEventConverter.class)
    private SerializableEventConverter localConverter;

    @Test
    public void toRemote()
    {
        assertFalse(this.remoteConverter.toRemote(new LocalEventData(null, null, null), null));
        assertTrue(
            this.remoteConverter.toRemote(new LocalEventData(new LogEvent(), null, null), new RemoteEventData()));
    }
}
