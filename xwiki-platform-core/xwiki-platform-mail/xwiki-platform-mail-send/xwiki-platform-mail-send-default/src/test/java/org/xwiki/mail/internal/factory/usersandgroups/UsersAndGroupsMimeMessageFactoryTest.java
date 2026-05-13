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

import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UsersAndGroupsMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
@ComponentTest
class UsersAndGroupsMimeMessageFactoryTest
{
    @InjectMockComponents
    private UsersAndGroupsMimeMessageFactory factory;

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
        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(Collections.emptyMap(), null));
        assertEquals("You must pass parameters for this Mime Message Factory to work!", exception.getMessage());
    }

    @Test
    void createMessageWhenNoHintParameterPassed()
    {
        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(Collections.emptyMap(), Collections.emptyMap()));
        assertEquals("The parameter [hint] is mandatory.", exception.getMessage());
    }

    @Test
    void createMessageWhenNoSourceParameterPassed()
    {
        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(Collections.emptyMap(),
                Collections.singletonMap("hint", "factoryHint")));
        assertEquals("The parameter [source] is mandatory.", exception.getMessage());
    }

    @Test
    void createMessageWhenNotExistingMimeMessageFactory()
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hint", "factoryHint");
        parameters.put("source", "factoryHint");

        Throwable exception = assertThrows(MessagingException.class, () ->
            this.factory.createMessage(Collections.emptyMap(), parameters));
        assertEquals("Failed to find a [MimeMessageFactory<MimeMessage>] for hint [factoryHint]",
            exception.getMessage());
    }

    @Test
    void createMessage() throws Exception
    {
        DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        Map<String, Object> source = Collections.singletonMap("users", Collections.singletonList(userReference));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hint", "template");
        parameters.put("source", new DocumentReference("templatewiki", "templatespace", "templatepage"));

        this.componentManager.registerMockComponent(new DefaultParameterizedType(null, MimeMessageFactory.class,
            MimeMessage.class), "template");

        // Setup XWikiContext since this is required internally by the iterator constructor
        Execution execution = this.componentManager.registerMockComponent(Execution.class);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xwikiContext);
        when(execution.getContext()).thenReturn(executionContext);

        Iterator<MimeMessage> iterator = this.factory.createMessage(source, parameters);
        assertNotNull(iterator);
    }
}
