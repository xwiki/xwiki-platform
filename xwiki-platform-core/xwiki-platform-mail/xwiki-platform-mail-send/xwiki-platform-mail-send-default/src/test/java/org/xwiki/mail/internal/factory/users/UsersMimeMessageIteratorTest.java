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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.users.UsersMimeMessageIterator}.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class UsersMimeMessageIteratorTest
{
    @Test
    public void createMessage() throws Exception
    {
        DocumentReference userReference1 = new DocumentReference("xwiki", "XWiki", "JohnDoe");
        DocumentReference userReference2 = new DocumentReference("xwiki", "XWiki", "JaneDoe");
        DocumentReference userReference3 = new DocumentReference("xwiki", "XWiki", "JonnieDoe");
        List<DocumentReference> userReferences = Arrays.asList(userReference1, userReference2, userReference3);

        Session session = Session.getInstance(new Properties());

        MimeMessageFactory factory = new MimeMessageFactory()
        {
            @Override public MimeMessage createMessage(Session session, Object source, Map parameters)
                throws MessagingException
            {
                return new MimeMessage(session);
            }
        };

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameters", Collections.EMPTY_MAP);
        parameters.put("session", session);

        DocumentAccessBridge accessBridge = mock(DocumentAccessBridge.class);

        when(accessBridge.getProperty(eq(userReference1), any(DocumentReference.class), eq("email"))).thenReturn(
            "john@doe.com");
        when(accessBridge.getProperty(eq(userReference2), any(DocumentReference.class), eq("email"))).thenReturn(
            "jane@doe.com");
        when(accessBridge.getProperty(eq(userReference3), any(DocumentReference.class), eq("email"))).thenReturn(
            "jannie@doe.com");

        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getInstance(eq(DocumentAccessBridge.class))).thenReturn(accessBridge);

        UsersMimeMessageIterator iterator =
            new UsersMimeMessageIterator(userReferences, factory, parameters, componentManager);

        assertTrue(iterator.hasNext());
        MimeMessage message1 = iterator.next();
        assertArrayEquals(message1.getRecipients(Message.RecipientType.TO), InternetAddress.parse("john@doe.com"));

        assertTrue(iterator.hasNext());
        MimeMessage message2 = iterator.next();
        assertArrayEquals(message2.getRecipients(Message.RecipientType.TO), InternetAddress.parse("jane@doe.com"));

        assertTrue(iterator.hasNext());
        MimeMessage message3 = iterator.next();
        assertArrayEquals(message3.getRecipients(Message.RecipientType.TO), InternetAddress.parse("jannie@doe.com"));

        assertFalse(iterator.hasNext());
    }
}