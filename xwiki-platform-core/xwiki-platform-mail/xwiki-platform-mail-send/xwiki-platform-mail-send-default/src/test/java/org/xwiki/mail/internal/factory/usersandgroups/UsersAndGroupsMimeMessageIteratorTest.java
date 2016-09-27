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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UsersAndGroupsMimeMessageIterator}.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
public class UsersAndGroupsMimeMessageIteratorTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private Execution execution;

    private XWikiContext xwikiContext;

    private MimeMessageFactory<MimeMessage> factory;

    private DocumentReferenceResolver<String> resolver;

    private XWiki xwiki;

    @Before
    public void setUpBaseMocks()
    {
        this.execution = mock(Execution.class);
        this.xwikiContext = mock(XWikiContext.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xwikiContext);
        when(this.execution.getContext()).thenReturn(executionContext);
        this.xwiki = mock(XWiki.class);
        when(this.xwikiContext.getWiki()).thenReturn(this.xwiki);

        this.factory = mock(MimeMessageFactory.class);
        this.resolver = mock(DocumentReferenceResolver.class);
    }

    @Test
    public void getMimeMessageWithSingleUserReferenceAndEmail() throws Exception
    {
        DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        setUpUserPageMocks(userReference, "john@doe.com");
        Map<String, Object> source = new HashMap<>();
        source.put("users", Collections.singletonList(userReference));
        source.put("emails", Collections.singletonList("mary@doe.com"));
        DocumentReference templateReference = new DocumentReference("templatewiki", "templatespace", "templatepage");
        Map<String, Object> parameters = Collections.<String, Object>singletonMap("source", templateReference);

        MimeMessage message = mock(MimeMessage.class);
        when(this.factory.createMessage(templateReference, null)).thenReturn(message);

        Iterator<MimeMessage> iterator = new UsersAndGroupsMimeMessageIterator(source, this.factory, parameters,
            this.resolver, this.execution);

        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        assertFalse(iterator.hasNext());

        verify(message).addRecipient(Message.RecipientType.TO, new InternetAddress("john@doe.com"));
        verify(message).addRecipient(Message.RecipientType.TO, new InternetAddress("mary@doe.com"));
    }

    @Test
    public void getMimeMessageWhenErrorCreatingMessage() throws Exception
    {
        DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        setUpUserPageMocks(userReference, "john@doe.com");
        Map<String, Object> source = new HashMap<>();
        source.put("users", Collections.singletonList(userReference));
        DocumentReference templateReference = new DocumentReference("templatewiki", "templatespace", "templatepage");
        Map<String, Object> parameters = Collections.<String, Object>singletonMap("source", templateReference);

        when(this.factory.createMessage(templateReference, null)).thenThrow(
            new MessagingException("error"));

        Iterator<MimeMessage> iterator = new UsersAndGroupsMimeMessageIterator(source, this.factory, parameters,
            this.resolver, this.execution);

        assertTrue(iterator.hasNext());
        try {
            iterator.next();
        } catch (RuntimeException expected) {
            assertEquals("Failed to create Mime Message for recipient john@doe.com", expected.getMessage());
        }
    }

    private void setUpUserPageMocks(DocumentReference userReference, String email) throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(false);
        when(document.getDocumentReference()).thenReturn(userReference);
        BaseObject baseObject = mock(BaseObject.class);
        when(document.getXObject(
            new EntityReference("XWikiUsers", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE))))
            .thenReturn(baseObject);
        when(this.xwiki.getDocument(userReference, this.xwikiContext)).thenReturn(document);
        when(baseObject.getStringValue("email")).thenReturn(email);
    }
}
