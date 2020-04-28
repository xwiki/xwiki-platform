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

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DirectMessageDescriptor}.
 * 
 * @version $Id$
 */
@ComponentTest
class DirectMessageDescriptorTest
{
    @MockComponent
    private MessageStreamConfiguration messageStreamConfiguration;

    @InjectMockComponents
    private DirectMessageDescriptor descriptor;

    @Test
    void getEventType() throws Exception
    {
        assertEquals("directMessage", this.descriptor.getEventType());
    }

    @Test
    void getApplicationIcon() throws Exception
    {
        assertEquals("comment", this.descriptor.getApplicationIcon());
    }

    @Test
    void getApplicationId() throws Exception
    {
        assertEquals("org.xwiki.platform:xwiki-platform-messagestream-api", this.descriptor.getApplicationId());
    }

    @Test
    void isEnabled() throws Exception
    {
        when(this.messageStreamConfiguration.isActive("wiki1")).thenReturn(true);
        when(this.messageStreamConfiguration.isActive("wiki2")).thenReturn(false);

        assertTrue(this.descriptor.isEnabled("wiki1"));
        assertFalse(this.descriptor.isEnabled("wiki2"));
    }

}
