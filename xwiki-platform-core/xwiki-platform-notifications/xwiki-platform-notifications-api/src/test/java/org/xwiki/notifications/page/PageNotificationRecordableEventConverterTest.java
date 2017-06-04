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
package org.xwiki.notifications.page;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id$
 */
public class PageNotificationRecordableEventConverterTest
{
    @Rule
    public final MockitoComponentMockingRule<PageNotificationRecordableEventConverter> mocker =
            new MockitoComponentMockingRule<>(PageNotificationRecordableEventConverter.class);

    @Test
    public void supportedEvents() throws Exception
    {
        List<RecordableEvent> eventsList = mocker.getComponentUnderTest().getSupportedEvents();

        assertEquals(1, eventsList.size());
        assertTrue(eventsList.get(0) instanceof PageNotificationEvent);
    }
}
