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
package org.xwiki.mail.internal.factory.group;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GroupMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.4.1
 */
@Deprecated
@ComponentTest
class GroupMimeMessageFactoryTest
{
    @InjectMockComponents
    private GroupMimeMessageFactory factory;

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
    void createMessageWhenNullParametersPassed()
    {
        DocumentReference groupReference = new DocumentReference("wiki", "space", "page");

        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(groupReference, null));
        assertEquals("You must pass parameters for this Mime Message Factory to work!", exception.getMessage());
    }

    @Test
    void createMessageWhenNoHintParameterPassed()
    {
        DocumentReference groupReference = new DocumentReference("wiki", "space", "page");

        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(groupReference, Collections.emptyMap()));
        assertEquals("The parameter [hint] is mandatory.", exception.getMessage());
    }

    @Test
    void createMessageWhenNoSourceParameterPassed()
    {
        DocumentReference groupReference = new DocumentReference("wiki", "space", "page");

        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(groupReference, Collections.singletonMap("hint", "factoryHint")));
        assertEquals("The parameter [source] is mandatory.", exception.getMessage());
    }

    @Test
    void createMessageWhenNotExistingMimeMessageFactory()
    {
        DocumentReference groupReference = new DocumentReference("wiki", "space", "page");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hint", "factoryHint");
        parameters.put("source", "factorySource");

        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(groupReference, parameters));
        assertEquals("Failed to find a [MimeMessageFactory<MimeMessage>] for hint [factoryHint]",
            exception.getMessage());
    }
}
