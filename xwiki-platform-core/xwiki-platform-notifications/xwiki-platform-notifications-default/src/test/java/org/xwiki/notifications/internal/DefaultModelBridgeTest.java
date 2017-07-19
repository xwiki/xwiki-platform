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
package org.xwiki.notifications.internal;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultModelBridge}.
 *
 * @since 9.7RC1
 * @version $Id$
 */
public class DefaultModelBridgeTest
{
    @Rule
    public final MockitoComponentMockingRule<ModelBridge> mocker =
            new MockitoComponentMockingRule<>(DefaultModelBridge.class);

    private Provider<XWikiContext> provider;

    private BaseObject fakeNotificationPreference;

    private XWikiContext fakeContext;

    private XWiki fakeWiki;

    @Before
    public void setUp() throws Exception
    {
        this.fakeNotificationPreference = mock(BaseObject.class);
        when(fakeNotificationPreference.getStringValue("eventType")).thenReturn("fakeEventType");
        when(fakeNotificationPreference.getDateValue("startDate")).thenReturn(new Date(10));
        when(fakeNotificationPreference.getStringValue("format")).thenReturn("email");
        when(fakeNotificationPreference.getIntValue("notificationEnabled", 0)).thenReturn(1);

        this.provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);

        this.fakeContext = new XWikiContext();
        this.fakeWiki = mock(XWiki.class);
        this.fakeContext.setWiki(this.fakeWiki);

        when(this.provider.get()).thenReturn(fakeContext);
    }

    @Test
    public void testGetNotificationsPreferences() throws Exception
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Admin");

        XWikiDocument fakeDocument = mock(XWikiDocument.class);
        when(this.fakeWiki.getDocument(userReference, this.fakeContext)).thenReturn(fakeDocument);
        when(fakeDocument.getXObjects(any(DocumentReference.class))).thenReturn(
                Collections.singletonList(this.fakeNotificationPreference));

        List<NotificationPreference> preferences =
                this.mocker.getComponentUnderTest().getNotificationsPreferences(userReference);

        assertEquals(1, preferences.size());
        assertEquals("fakeEventType", preferences.get(0).getEventType());
        assertEquals(new Date(10), preferences.get(0).getStartDate());
        assertEquals(NotificationFormat.EMAIL, preferences.get(0).getFormat());
        assertTrue(preferences.get(0).isNotificationEnabled());
    }
}
