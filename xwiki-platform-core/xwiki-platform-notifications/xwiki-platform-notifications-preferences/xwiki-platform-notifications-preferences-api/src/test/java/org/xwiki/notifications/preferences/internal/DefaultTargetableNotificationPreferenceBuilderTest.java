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
package org.xwiki.notifications.preferences.internal;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Tests for {@link DefaultTargetableNotificationPreferenceBuilder}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultTargetableNotificationPreferenceBuilderTest
{
    @InjectMockComponents
    private DefaultTargetableNotificationPreferenceBuilder builder;

    /**
     * Test that the builder build different instances and equals/hashcode methods are correct.
     * Only format, properties and target are taken into account in the equals/hashcode.
     */
    @Test
    public void build()
    {
        this.builder.prepare();
        this.builder.setCategory(NotificationPreferenceCategory.DEFAULT);
        this.builder.setEnabled(true);
        this.builder.setFormat(NotificationFormat.ALERT);
        this.builder.setProperties(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "foo"));
        this.builder.setStartDate(new Date(42));
        this.builder.setProviderHint("MyHint");
        this.builder.setTarget(new DocumentReference("xwiki", "Space", "Page"));
        TargetableNotificationPreference preference1 = this.builder.build();

        this.builder.prepare();
        this.builder.setCategory(NotificationPreferenceCategory.DEFAULT);
        this.builder.setEnabled(true);
        this.builder.setFormat(NotificationFormat.ALERT);
        this.builder.setProperties(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "foo"));
        this.builder.setStartDate(new Date(42));
        this.builder.setProviderHint("MyHint");
        this.builder.setTarget(new DocumentReference("xwiki", "Space", "Page"));
        TargetableNotificationPreference preference2 = this.builder.build();

        assertNotSame(preference1, preference2);
        assertEquals(preference1, preference2);
        assertEquals(preference1.hashCode(), preference2.hashCode());


        this.builder.prepare();
        this.builder.setCategory(NotificationPreferenceCategory.SYSTEM);
        this.builder.setEnabled(false);
        this.builder.setFormat(NotificationFormat.ALERT);
        this.builder.setProperties(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "foo"));
        this.builder.setStartDate(new Date(88));
        this.builder.setProviderHint("AnotherHint");
        this.builder.setTarget(new DocumentReference("xwiki", "Space", "Page"));
        preference2 = this.builder.build();

        assertNotSame(preference1, preference2);
        assertEquals(preference1, preference2);
        assertEquals(preference1.hashCode(), preference2.hashCode());

        this.builder.prepare();
        this.builder.setCategory(NotificationPreferenceCategory.DEFAULT);
        this.builder.setEnabled(true);
        this.builder.setFormat(NotificationFormat.EMAIL);
        this.builder.setProperties(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "foo"));
        this.builder.setStartDate(new Date(42));
        this.builder.setProviderHint("MyHint");
        this.builder.setTarget(new DocumentReference("xwiki", "Space", "Page"));
        preference2 = this.builder.build();

        assertNotSame(preference1, preference2);
        assertNotEquals(preference1, preference2);
        assertNotEquals(preference1.hashCode(), preference2.hashCode());

        this.builder.prepare();
        this.builder.setCategory(NotificationPreferenceCategory.DEFAULT);
        this.builder.setEnabled(true);
        this.builder.setFormat(NotificationFormat.ALERT);
        this.builder.setProperties(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "bar"));
        this.builder.setStartDate(new Date(42));
        this.builder.setProviderHint("MyHint");
        this.builder.setTarget(new DocumentReference("xwiki", "Space", "Page"));
        preference2 = this.builder.build();

        assertNotSame(preference1, preference2);
        assertNotEquals(preference1, preference2);
        assertNotEquals(preference1.hashCode(), preference2.hashCode());

        this.builder.prepare();
        this.builder.setCategory(NotificationPreferenceCategory.DEFAULT);
        this.builder.setEnabled(true);
        this.builder.setFormat(NotificationFormat.ALERT);
        this.builder.setProperties(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "foo"));
        this.builder.setStartDate(new Date(42));
        this.builder.setProviderHint("MyHint");
        this.builder.setTarget(new DocumentReference("xwiki", "Space", "Page2"));
        preference2 = this.builder.build();

        assertNotSame(preference1, preference2);
        assertNotEquals(preference1, preference2);
        assertNotEquals(preference1.hashCode(), preference2.hashCode());
    }
}
