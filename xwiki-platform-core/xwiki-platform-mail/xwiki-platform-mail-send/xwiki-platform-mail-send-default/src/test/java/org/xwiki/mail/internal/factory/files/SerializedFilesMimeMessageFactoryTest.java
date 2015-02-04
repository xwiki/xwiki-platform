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

import java.util.Properties;

import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.files.SerializedFilesMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.4.1
 */
public class SerializedFilesMimeMessageFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<SerializedFilesMimeMessageFactory> mocker =
        new MockitoComponentMockingRule<>(SerializedFilesMimeMessageFactory.class);

    @Test
    public void createMessageWhenNoExecution() throws Exception
    {
        Session session = Session.getInstance(new Properties());

        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);

        try {
            this.mocker.getComponentUnderTest().createMessage(session, "batchId", null);
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("Failed to find an Environment Component", expected.getMessage());
        }
    }
}
