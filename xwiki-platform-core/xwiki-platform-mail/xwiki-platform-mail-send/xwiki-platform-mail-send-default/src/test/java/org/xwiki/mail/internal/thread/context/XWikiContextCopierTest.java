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
package org.xwiki.mail.internal.thread.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiContextCopier}.
 *
 * @version $Id$
 */
@ComponentTest
public class XWikiContextCopierTest
{
    private static final String HIBSESSION = "hibsession";

    @InjectMockComponents
    private XWikiContextCopier copier;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    XWikiServletResponseStub originalResponse;

    XWikiRequest originalRequest;

    XWikiStoreInterface store;

    XWikiContext original;

    @BeforeEach
    public void setup() throws Exception
    {
        Utils.setComponentManager(this.componentManager);

        this.originalResponse = new XWikiServletResponseStub();

        this.original = new XWikiContext();

        // Set some values
        this.original.setWikiId("wiki");

        DocumentReference userReference = new DocumentReference("wiki", "Space", "Page");
        EntityReferenceSerializer<String> serializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(serializer.serialize(userReference)).thenReturn("wiki:Space.Page");

        this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

        this.original.setUserReference(userReference);

        // Set the mock request
        this.originalRequest = mock(XWikiRequest.class);
        this.original.setRequest(this.originalRequest);
        Copier<XWikiRequest> requestCopier =
            this.componentManager.getInstance(new DefaultParameterizedType(null, Copier.class, XWikiRequest.class));
        when(requestCopier.copy(this.originalRequest)).thenReturn(this.originalRequest);

        // Set the stubbed response
        this.original.setResponse(this.originalResponse);

        // XWiki mock
        XWiki xwiki = mock(XWiki.class);
        this.original.setWiki(xwiki);

        // Store mock
        // Simulate the existence of a hibernate session in context
        this.original.put(HIBSESSION, "opened session");
        this.store = mock(XWikiStoreInterface.class);
        // clean up will remove the session in the given context
        doAnswer(new Answer<Void>()
        {
            public Void answer(InvocationOnMock invocation)
            {
                XWikiContext context = (XWikiContext) invocation.getArguments()[0];
                context.put(HIBSESSION, null);
                return null;
            }
        }).when(this.store).cleanUp(any(XWikiContext.class));
        when(xwiki.getStore()).thenReturn(this.store);

        // URL factory mock
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);

        XWikiURLFactoryService urlFactoryService = mock(XWikiURLFactoryService.class);
        when(urlFactoryService.createURLFactory(anyInt(), any(XWikiContext.class))).thenReturn(
            urlFactory);
        when(xwiki.getURLFactoryService()).thenReturn(urlFactoryService);
    }

    @Test
    public void copyContext()
    {
        XWikiContext copy = this.copier.copy(this.original);

        // Check that the response is not the same.
        assertNotSame(this.originalResponse, copy.getResponse());

        // Check that the context values are cloned.
        assertEquals(this.original.getUserReference(), copy.getUserReference());
        assertEquals(this.original.getWikiId(), copy.getWikiId());
        // No URL was present in the original context so a stub is used.
        // Note: for some reason, comparing 2 URLs takes ages (~19 seconds) on my machine. Comparing strings instead for
        // performance.
        assertEquals("http://www.mystuburl.com/", copy.getURL().toString());
        // Actually, all the context keys should be copied.
        assertNotSame(this.original.entrySet(), copy.entrySet());
        assertEquals(this.original.entrySet(), copy.entrySet());

        // Some things are not cloned.
        assertSame(this.original.getWiki(), copy.getWiki());

        // Verify that the store session has been cleaned for both context.
        assertNull(this.original.get(HIBSESSION));
        assertNull(copy.get(HIBSESSION));

        // Check that the URLFactory is cloned.
        assertNotNull(copy.getURLFactory());
        assertNotSame(this.original.getURLFactory(), copy.getURLFactory());
    }
}
