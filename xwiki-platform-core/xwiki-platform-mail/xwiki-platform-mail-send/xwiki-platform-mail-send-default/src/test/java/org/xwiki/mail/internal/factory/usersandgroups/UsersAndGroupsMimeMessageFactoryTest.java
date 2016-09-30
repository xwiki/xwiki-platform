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
package org.xwiki.mail.internal.factory.usersandgroups;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UsersAndGroupsMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
public class UsersAndGroupsMimeMessageFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<UsersAndGroupsMimeMessageFactory> mocker =
        new MockitoComponentMockingRule<>(UsersAndGroupsMimeMessageFactory.class);

    @Test
    public void createMessageWhenNullParametersPassed() throws Exception
    {
        try {
            this.mocker.getComponentUnderTest().createMessage(Collections.<String, Object>emptyMap(), null);
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("You must pass parameters for this Mime Message Factory to work!", expected.getMessage());
        }
    }

    @Test
    public void createMessageWhenNoHintParameterPassed() throws Exception
    {
        try {
            this.mocker.getComponentUnderTest().createMessage(
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap());
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("The parameter [hint] is mandatory.", expected.getMessage());
        }
    }

    @Test
    public void createMessageWhenNoSourceParameterPassed() throws Exception
    {
        try {
            this.mocker.getComponentUnderTest().createMessage(Collections.<String, Object>emptyMap(),
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hint", "factoryHint");
        parameters.put("source", "factoryHint");

        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);

        try {
            this.mocker.getComponentUnderTest().createMessage(Collections.<String, Object>emptyMap(),
                parameters);
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("Failed to find a [MimeMessageFactory<MimeMessage>] for hint [factoryHint]",
                expected.getMessage());
        }
    }

    @Test
    public void createMessage() throws Exception
    {
        DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        Map<String, Object> source = Collections.<String, Object>singletonMap("users",
            Collections.singletonList(userReference));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hint", "template");
        parameters.put("source", new DocumentReference("templatewiki", "templatespace", "templatepage"));

        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);
        this.mocker.registerMockComponent(new DefaultParameterizedType(null, MimeMessageFactory.class,
            MimeMessage.class), "template");

        // Setup XWikiContext since this is required internally by the iterator constructor
        Execution execution = this.mocker.registerMockComponent(Execution.class);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xwikiContext);
        when(execution.getContext()).thenReturn(executionContext);

        Iterator<MimeMessage> iterator = this.mocker.getComponentUnderTest().createMessage(source, parameters);
        assertNotNull(iterator);
    }
}
