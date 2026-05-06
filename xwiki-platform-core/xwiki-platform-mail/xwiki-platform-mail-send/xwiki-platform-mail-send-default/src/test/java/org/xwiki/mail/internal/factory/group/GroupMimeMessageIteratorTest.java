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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GroupMimeMessageIterator}.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Deprecated
class GroupMimeMessageIteratorTest
{
    @Test
    void createMessage() throws Exception
    {
        DocumentReference groupReference = new DocumentReference("xwiki", "XWiki", "Marketing");

        DocumentReference userReference1 = new DocumentReference("xwiki", "XWiki", "JohnDoe");
        DocumentReference userReference2 = new DocumentReference("xwiki", "XWiki", "JaneDoe");
        DocumentReference userReference3 = new DocumentReference("xwiki", "XWiki", "JonnieDoe");

        Session session = Session.getInstance(new Properties());

        MimeMessageFactory<MimeMessage> factory = (source, parameters) -> new ExtendedMimeMessage();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameters", Collections.emptyMap());
        parameters.put("session", session);

        DocumentAccessBridge accessBridge = mock(DocumentAccessBridge.class);
        when(accessBridge.getProperty(eq(groupReference), any(), eq(0), eq("member"))).thenReturn("XWiki.JohnDoe");
        when(accessBridge.getProperty(eq(groupReference), any(), eq(1), eq("member"))).thenReturn("XWiki.JaneDoe");
        when(accessBridge.getProperty(eq(groupReference), any(), eq(2), eq("member"))).thenReturn("XWiki.JonnieDoe");

        when(accessBridge.getProperty(eq(userReference1), any(), eq("email"))).thenReturn("john@doe.com");
        when(accessBridge.getProperty(eq(userReference2), any(), eq("email"))).thenReturn("jane@doe.com");
        when(accessBridge.getProperty(eq(userReference3), any(), eq("email"))).thenReturn("jannie@doe.com");

        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);

        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikiContext);

        XWiki xwiki = mock(XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);

        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(groupReference), eq(xwikiContext))).thenReturn(document);

        BaseObject object = mock(BaseObject.class);

        when(document.getXObjects(any(EntityReference.class))).thenReturn(List.of(object, object, object));

        DocumentReferenceResolver<String> resolver = mock(DocumentReferenceResolver.class);
        when(resolver.resolve("XWiki.JohnDoe")).thenReturn(userReference1);
        when(resolver.resolve("XWiki.JaneDoe")).thenReturn(userReference2);
        when(resolver.resolve("XWiki.JonnieDoe")).thenReturn(userReference3);

        ComponentManager componentManager = mock(ComponentManager.class);

        when(componentManager.getInstance(eq(DocumentAccessBridge.class))).thenReturn(accessBridge);
        when(componentManager.getInstance(eq(Execution.class))).thenReturn(execution);
        when(componentManager.getInstance(eq(DocumentReferenceResolver.TYPE_STRING), eq("current")))
            .thenReturn(resolver);

        GroupMimeMessageIterator iterator =
            new GroupMimeMessageIterator(groupReference, factory, parameters, componentManager);

        assertTrue(iterator.hasNext());
        MimeMessage message1 = iterator.next();
        assertArrayEquals(InternetAddress.parse("john@doe.com"), message1.getRecipients(Message.RecipientType.TO));

        assertTrue(iterator.hasNext());
        MimeMessage message2 = iterator.next();
        assertArrayEquals(InternetAddress.parse("jane@doe.com"), message2.getRecipients(Message.RecipientType.TO));

        assertTrue(iterator.hasNext());
        MimeMessage message3 = iterator.next();
        assertArrayEquals(InternetAddress.parse("jannie@doe.com"), message3.getRecipients(Message.RecipientType.TO));

        assertFalse(iterator.hasNext());
    }
}
