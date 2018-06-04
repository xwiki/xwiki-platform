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
package org.xwiki.messagestream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since
 */
public class DirectMessageDescriptorTest
{
    @Rule
    public MockitoComponentMockingRule<DirectMessageDescriptor> mocker =
            new MockitoComponentMockingRule<>(DirectMessageDescriptor.class);

    private MessageStreamConfiguration messageStreamConfiguration;

    @Before
    public void setUp() throws Exception
    {
        messageStreamConfiguration = mocker.getInstance(MessageStreamConfiguration.class);
    }

    @Test
    public void getEventType() throws Exception
    {
        assertEquals("directMessage", mocker.getComponentUnderTest().getEventType());
    }

    @Test
    public void getApplicationIcon() throws Exception
    {
        assertEquals("envelope", mocker.getComponentUnderTest().getApplicationIcon());
    }

    @Test
    public void getApplicationId() throws Exception
    {
        assertEquals("org.xwiki.platform:xwiki-platform-messagestream-api",
                mocker.getComponentUnderTest().getApplicationId());
    }

    @Test
    public void isEnabled() throws Exception
    {
        when(messageStreamConfiguration.isActive("wiki1")).thenReturn(true);
        when(messageStreamConfiguration.isActive("wiki2")).thenReturn(false);

        assertTrue(mocker.getComponentUnderTest().isEnabled("wiki1"));
        assertFalse(mocker.getComponentUnderTest().isEnabled("wiki2"));
    }


}

