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
package org.xwiki.mail.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.iterator.factory.GroupMimeMessageIteratorFactory;
import org.xwiki.mail.internal.iterator.factory.SerializedFilesMimeMessageIteratorFactory;
import org.xwiki.mail.internal.iterator.factory.UsersMimeMessageIteratorFactory;
import org.xwiki.model.reference.DocumentReference;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MimeMessageIteratorFactoryProvider}.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class MimeMessageIteratorFactoryProviderTest
{
    private ComponentManager componentManager;

    private MimeMessageFactory mimeMessageFactory;

    private Map<String, Object> parameters;

    @Before
    public void setUp() throws Exception
    {
        this.mimeMessageFactory = new MimeMessageFactory()
        {
            @Override public MimeMessage createMessage(Session session, Object source, Map parameters)
                throws MessagingException
            {
                return new MimeMessage(session);
            }
        };

        Session session = Session.getInstance(new Properties());

        this.parameters = new HashMap<>();
        this.parameters.put("parameters", Collections.EMPTY_MAP);
        this.parameters.put("session", session);

        this.componentManager = mock(ComponentManager.class);
    }

    @Test
    public void createUsersIterator() throws Exception
    {
        DocumentReference userReference1 = new DocumentReference("xwiki", "XWiki", "JohnDoe");
        DocumentReference userReference2 = new DocumentReference("xwiki", "XWiki", "JaneDoe");
        DocumentReference userReference3 = new DocumentReference("xwiki", "XWiki", "JonnieDoe");
        List<DocumentReference> userReferences = Arrays.asList(userReference1, userReference2, userReference3);

        UsersMimeMessageIteratorFactory userFactory = mock(UsersMimeMessageIteratorFactory.class);
        when(this.componentManager.getInstance(eq(UsersMimeMessageIteratorFactory.class))).thenReturn(userFactory);

        Iterator<MimeMessage> iterator = MimeMessageIteratorFactoryProvider
            .get("users", userReferences, this.mimeMessageFactory, this.parameters, this.componentManager);

        verify(this.componentManager).getInstance(UsersMimeMessageIteratorFactory.class);
        verify(userFactory).create(eq(userReferences), eq(this.mimeMessageFactory), eq(this.parameters));
    }

    @Test
    public void createGroupIterator() throws Exception
    {
        DocumentReference groupReference = new DocumentReference("xwiki", "XWiki", "Marketing");

        GroupMimeMessageIteratorFactory groupFactory = mock(GroupMimeMessageIteratorFactory.class);
        when(this.componentManager.getInstance(eq(GroupMimeMessageIteratorFactory.class))).thenReturn(groupFactory);

        Iterator<MimeMessage> iterator = MimeMessageIteratorFactoryProvider
            .get("group", groupReference, this.mimeMessageFactory, this.parameters, this.componentManager);

        verify(this.componentManager).getInstance(GroupMimeMessageIteratorFactory.class);
        verify(groupFactory).create(eq(groupReference), eq(this.mimeMessageFactory), eq(this.parameters));
    }

    @Test
    public void createSerializedIterator() throws Exception
    {
        UUID batchId = UUID.randomUUID();

        SerializedFilesMimeMessageIteratorFactory filesFactory = mock(SerializedFilesMimeMessageIteratorFactory.class);
        when(this.componentManager.getInstance(eq(SerializedFilesMimeMessageIteratorFactory.class)))
            .thenReturn(filesFactory);

        Iterator<MimeMessage> iterator = MimeMessageIteratorFactoryProvider
            .get("files", batchId, this.mimeMessageFactory, this.parameters, this.componentManager);

        verify(this.componentManager).getInstance(SerializedFilesMimeMessageIteratorFactory.class);
        verify(filesFactory).create(eq(batchId), eq(this.parameters));
    }
}