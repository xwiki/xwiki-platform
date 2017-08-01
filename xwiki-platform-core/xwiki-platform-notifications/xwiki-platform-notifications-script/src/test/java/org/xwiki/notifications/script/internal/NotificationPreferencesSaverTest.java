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
package org.xwiki.notifications.script.internal;

import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
public class NotificationPreferencesSaverTest
{
    @Rule
    public final MockitoComponentMockingRule<NotificationPreferencesSaver> mocker =
            new MockitoComponentMockingRule<>(NotificationPreferencesSaver.class);

    private NotificationPreferenceManager notificationPreferenceManager;

    @Before
    public void setUp() throws Exception
    {
        notificationPreferenceManager = mocker.getInstance(NotificationPreferenceManager.class);
    }

    @Test
    public void test() throws Exception
    {
        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "UserA");
        mocker.getComponentUnderTest().saveNotificationPreferences(
                IOUtils.toString(getClass().getResourceAsStream("/preferences.json")), userRef);

        verify(notificationPreferenceManager, times(1)).saveNotificationsPreferences(eq(userRef), any(List.class));
    }
}
