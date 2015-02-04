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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MailSenderScriptService}.
 *
 * @version $Id$
 * @since 6.4.1
 */
public class MailSenderScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<MailSenderScriptService> mocker =
        new MockitoComponentMockingRule<>(MailSenderScriptService.class);

    @Test
    public void createMessagesWithListOfDocumentReferenceType() throws Exception
    {
        List<DocumentReference> users = new ArrayList<>();
        users.add(new DocumentReference("wiki", "space", "page"));

        ComponentManager cm = mock(ComponentManager.class);
        Provider<ComponentManager> cmProvider = this.mocker.registerMockComponent(new DefaultParameterizedType(null,
            Provider.class, ComponentManager.class), "context");
        when(cmProvider.get()).thenReturn(cm);

        MimeMessageFactory factory = mock(MimeMessageFactory.class);
        Type type = new DefaultParameterizedType(null, MimeMessageFactory.class,
            new DefaultParameterizedType(null, List.class, DocumentReference.class),
            new DefaultParameterizedType(null, Iterator.class, MimeMessage.class));
        when(cm.getInstance(type, "users/secure")).thenReturn(factory);

        this.mocker.getComponentUnderTest().createMessages("users", users, Collections.<String, Object>emptyMap());

        verify(cm).getInstance(type, "users/secure");
    }

    @Test
    public void createMessagesWithDocumentReferenceType() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        ComponentManager cm = mock(ComponentManager.class);
        Provider<ComponentManager> cmProvider = this.mocker.registerMockComponent(new DefaultParameterizedType(null,
            Provider.class, ComponentManager.class), "context");
        when(cmProvider.get()).thenReturn(cm);

        MimeMessageFactory factory = mock(MimeMessageFactory.class);
        Type type = new DefaultParameterizedType(null, MimeMessageFactory.class, DocumentReference.class,
            new DefaultParameterizedType(null, Iterator.class, MimeMessage.class));
        when(cm.getInstance(type, "group/secure")).thenReturn(factory);

        this.mocker.getComponentUnderTest().createMessages("group", reference, Collections.<String, Object>emptyMap());

        verify(cm).getInstance(type, "group/secure");
    }

    @Test
    public void createMessagesWithStringType() throws Exception
    {
        ComponentManager cm = mock(ComponentManager.class);
        Provider<ComponentManager> cmProvider = this.mocker.registerMockComponent(new DefaultParameterizedType(null,
            Provider.class, ComponentManager.class), "context");
        when(cmProvider.get()).thenReturn(cm);

        MimeMessageFactory factory = mock(MimeMessageFactory.class);
        Type type = new DefaultParameterizedType(null, MimeMessageFactory.class, String.class,
            new DefaultParameterizedType(null, Iterator.class, MimeMessage.class));
        when(cm.getInstance(type, "files/secure")).thenReturn(factory);

        this.mocker.getComponentUnderTest().createMessages("files", "batchid", Collections.<String, Object>emptyMap());

        verify(cm).getInstance(type, "files/secure");
    }

    @Test
    public void createMessagesWithTypePassedAsParameter() throws Exception
    {
        List<DocumentReference> users = new ArrayList<>();
        users.add(new DocumentReference("wiki", "space", "page"));

        ComponentManager cm = mock(ComponentManager.class);
        Provider<ComponentManager> cmProvider = this.mocker.registerMockComponent(new DefaultParameterizedType(null,
            Provider.class, ComponentManager.class), "context");
        when(cmProvider.get()).thenReturn(cm);

        MimeMessageFactory factory = mock(MimeMessageFactory.class);
        Type type = new DefaultParameterizedType(null, MimeMessageFactory.class,
            new DefaultParameterizedType(null, List.class, DocumentReference.class),
            new DefaultParameterizedType(null, Iterator.class, MimeMessage.class));
        when(cm.getInstance(type, "users/secure")).thenReturn(factory);

        String typeAsString = String.format("%s<%s>", List.class.getName(), DocumentReference.class.getName());
        Map<String, Object> parameters = Collections.<String, Object>singletonMap("type", typeAsString);

        ConverterManager converterManager = this.mocker.getInstance(ConverterManager.class);
        when(converterManager.convert(Type.class, typeAsString)).thenReturn(
            new DefaultParameterizedType(null, List.class, DocumentReference.class));

        this.mocker.getComponentUnderTest().createMessages("users", users, parameters);

        verify(cm).getInstance(type, "users/secure");
    }
}
