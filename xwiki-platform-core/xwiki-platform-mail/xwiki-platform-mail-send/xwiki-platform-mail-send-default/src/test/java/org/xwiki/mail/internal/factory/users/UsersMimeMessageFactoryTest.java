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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.users.UsersMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.4.1
 */
public class UsersMimeMessageFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<UsersMimeMessageFactory> mocker =
        new MockitoComponentMockingRule<>(UsersMimeMessageFactory.class);

    @Test
    public void createMessageWhenNullParametersPassed() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");

        try {
            this.mocker.getComponentUnderTest().createMessage(session, Arrays.asList(userReference), null);
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("You must pass parameters for this Mime Message Factory to work!", expected.getMessage());
        }
    }

    @Test
    public void createMessageWhenNoHintParameterPassed() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");

        try {
            this.mocker.getComponentUnderTest().createMessage(session, Arrays.asList(userReference),
                Collections.<String, Object>emptyMap());
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("The parameter [hint] is mandatory.", expected.getMessage());
        }
    }

    @Test
    public void createMessageWhenNoSourceParameterPassed() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");

        try {
            this.mocker.getComponentUnderTest().createMessage(session, Arrays.asList(userReference),
                Collections.<String, Object>singletonMap("hint", "factoryHint"));
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("The parameter [source] is mandatory.", expected.getMessage());
        }
    }

    @Test
    public void createMessageWhenNotExistingMimeMessageFactory() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        DocumentReference userReference = new DocumentReference("wiki", "space", "page");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hint", "factoryHint");
        parameters.put("source", "factoryHint");

        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);

        try {
            this.mocker.getComponentUnderTest().createMessage(session, Arrays.asList(userReference), parameters);
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("Failed to find a [MimeMessageFactory<String, MimeMessage>] for hint [factoryHint]",
                expected.getMessage());
        }
    }
}
