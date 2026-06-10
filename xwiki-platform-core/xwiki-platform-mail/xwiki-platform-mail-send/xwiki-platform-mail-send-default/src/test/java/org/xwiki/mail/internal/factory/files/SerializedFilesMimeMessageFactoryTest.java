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
package org.xwiki.mail.internal.factory.files;

import javax.inject.Provider;
import javax.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SerializedFilesMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.4.1
 */
@ComponentTest
class SerializedFilesMimeMessageFactoryTest
{
    @InjectMockComponents
    private SerializedFilesMimeMessageFactory factory;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        Provider<ComponentManager> contextProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(contextProvider.get()).thenReturn(this.componentManager);
    }

    @Test
    void createMessageWhenNoExecution()
    {
        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage("batchId", null));
        assertEquals("Failed to find an Environment Component", exception.getMessage());
    }
}
