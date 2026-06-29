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
package org.xwiki.mail.internal.factory.users;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.users.UsersMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.4.1
 */
@ComponentTest
@Deprecated
class UsersMimeMessageFactoryTest
{
    @InjectMockComponents
    private UsersMimeMessageFactory factory;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void createMessageWhenNullParametersPassed()
    {
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");

        MessagingException e = assertThrows(MessagingException.class,
            () -> this.factory.createMessage(List.of(userReference), null));
        assertEquals("You must pass parameters for this Mime Message Factory to work!", e.getMessage());
    }

    @Test
    void createMessageWhenNoHintParameterPassed()
    {
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");

        MessagingException e = assertThrows(MessagingException.class,
            () -> this.factory.createMessage(List.of(userReference), Map.of()));
        assertEquals("The parameter [hint] is mandatory.", e.getMessage());
    }

    @Test
    void createMessageWhenNoSourceParameterPassed()
    {
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");

        MessagingException e = assertThrows(MessagingException.class,
            () -> this.factory.createMessage(List.of(userReference), Map.of("hint", "factoryHint")));
        assertEquals("The parameter [source] is mandatory.", e.getMessage());
    }

    @Test
    void createMessageWhenNotExistingMimeMessageFactory()
    {
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");
        Map<String, Object> parameters = Map.of("hint", "factoryHint", "source", "factoryHint");

        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);

        MessagingException e = assertThrows(MessagingException.class,
            () -> this.factory.createMessage(List.of(userReference), parameters));
        assertEquals("Failed to find a [MimeMessageFactory<MimeMessage>] for hint [factoryHint]", e.getMessage());
    }
}
