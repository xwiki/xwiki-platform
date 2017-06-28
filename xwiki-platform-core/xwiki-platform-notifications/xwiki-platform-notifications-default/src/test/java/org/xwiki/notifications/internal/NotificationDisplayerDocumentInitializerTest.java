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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link NotificationDisplayerDocumentInitializer}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class NotificationDisplayerDocumentInitializerTest
{
    @Rule
    public final MockitoComponentMockingRule<NotificationDisplayerDocumentInitializer> mocker =
            new MockitoComponentMockingRule<>(NotificationDisplayerDocumentInitializer.class);

    @Test
    public void testCreateClass() throws Exception
    {
        BaseClass testXClass = new BaseClass();

        this.mocker.getComponentUnderTest().createClass(testXClass);

        assertEquals(2, testXClass.getFieldList().size());
    }
}
