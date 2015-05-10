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
package org.xwiki.mail.internal.factory.message;

import javax.mail.MessagingException;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.message.MessageMimeMessageFactory}.
 *
 * @version $Id$
 */
public class MessageMimeMessageFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<MessageMimeMessageFactory> mocker =
        new MockitoComponentMockingRule<>(MessageMimeMessageFactory.class);

    @Test
    public void createMessageWithBadSource() throws Exception
    {
        try {
            this.mocker.getComponentUnderTest().createMessage(null, "source", null);
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("Failed to create mime message from source [class java.lang.String]", expected.getMessage());
        }
    }
}
