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
package org.xwiki.notifications.filters.internal;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.minor.MinorEventEmailNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class MinorEventEmailNotificationFilterTest
{
    @Rule
    public final MockitoComponentMockingRule<MinorEventEmailNotificationFilter> mocker =
            new MockitoComponentMockingRule<>(MinorEventEmailNotificationFilter.class);

    @Test
    public void getName() throws Exception
    {
        assertEquals(MinorEventEmailNotificationFilter.FILTER_NAME, mocker.getComponentUnderTest().getName());
    }

    @Test
    public void getFormats() throws Exception
    {
        assertEquals(1, mocker.getComponentUnderTest().getFormats().size());
        assertTrue(mocker.getComponentUnderTest().getFormats().contains(NotificationFormat.EMAIL));
    }

    @Test
    public void filterExpression() throws Exception
    {
        NotificationPreference fakePreference = mock(NotificationPreference.class);

        DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "UserA");
        assertNull(mocker.getComponentUnderTest().filterExpression(randomUser, Collections.emptyList(), fakePreference));
        assertEquals("NOT ((TYPE = \"update\" AND NOT (DOCUMENT_VERSION ENDS WITH \".1\")))",
                mocker.getComponentUnderTest().filterExpression(randomUser, Collections.emptyList(),
                        NotificationFilterType.EXCLUSIVE,
                        NotificationFormat.EMAIL).toString());
    }

    @Test
    public void filterExpressionWithWrongParameters() throws Exception
    {
        DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "UserA");
        assertNull(mocker.getComponentUnderTest().filterExpression(randomUser, Collections.emptyList(),
                NotificationFilterType.INCLUSIVE, NotificationFormat.EMAIL));
        assertNull(mocker.getComponentUnderTest().filterExpression(randomUser, Collections.emptyList(),
                NotificationFilterType.INCLUSIVE, NotificationFormat.ALERT));
    }
}
