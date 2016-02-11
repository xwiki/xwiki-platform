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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiContextCopier}.
 *
 * @version $Id$
 */
public class XWikiContextCopierTest
{
    private static final String HIBSESSION = "hibsession";

    @Rule
    public MockitoComponentMockingRule<XWikiContextCopier> mocker = new MockitoComponentMockingRule<>(
        XWikiContextCopier.class);

    XWikiServletResponseStub originalResponse;

    XWikiRequest originalRequest;

    XWikiStoreInterface store;

    XWikiContext original;

    @Before
    public void setup() throws Exception
    {
        Utils.setComponentManager(mocker);

        originalResponse = new XWikiServletResponseStub();

        original = new XWikiContext();

        // Set some values
        original.setWikiId("wiki");

        DocumentReference userReference = new DocumentReference("wiki", "Space", "Page");
        EntityReferenceSerializer<String> serializer =
            mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(serializer.serialize(userReference)).thenReturn("wiki:Space.Page");

        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

        original.setUserReference(userReference);

        // Set the mock request
        this.originalRequest = mock(XWikiRequest.class);
        original.setRequest(this.originalRequest);
        Copier<XWikiRequest> requestCopier =
            mocker.getInstance(new DefaultParameterizedType(null, Copier.class, XWikiRequest.class));
        when(requestCopier.copy(this.originalRequest)).thenReturn(this.originalRequest);

        // Set the stubbed response
        original.setResponse(originalResponse);

        // XWiki mock
        XWiki xwiki = mock(XWiki.class);
        original.setWiki(xwiki);

        // Store mock
        // Simulate the existence of a hibernate session in context
        original.put(HIBSESSION,"opened session");
        store = mock(XWikiStoreInterface.class);
        // clean up will remove the session in the given context
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                XWikiContext context = (XWikiContext) invocation.getArguments()[0];
                context.put(HIBSESSION, null);
                return null;
            }
        }).when(store).cleanUp(any(XWikiContext.class));
        when(xwiki.getStore()).thenReturn(store);

        // URL factory mock
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);

        XWikiURLFactoryService urlFactoryService = mock(XWikiURLFactoryService.class);
        when(urlFactoryService.createURLFactory(Matchers.anyInt(), any(XWikiContext.class))).thenReturn(
            urlFactory);
        when(xwiki.getURLFactoryService()).thenReturn(urlFactoryService);
    }

    @Test
    public void copyContext() throws Exception
    {
        XWikiContext copy = mocker.getComponentUnderTest().copy(original);

        // Check that the response is not the same.
        assertNotSame(originalResponse, copy.getResponse());

        // Check that the context values are cloned.
        assertEquals(original.getUserReference(), copy.getUserReference());
        assertEquals(original.getWikiId(), copy.getWikiId());
        // No URL was present in the original context so a stub is used.
        // Note: for some reason, comparing 2 URLs takes ages (~19 seconds) on my machine. Comparing strings instead for
        // performance.
        assertEquals("http://www.mystuburl.com/", copy.getURL().toString());
        // Actually, all the context keys should be copied.
        assertNotSame(original.entrySet(), copy.entrySet());
        assertEquals(original.entrySet(), copy.entrySet());

        // Some things are not cloned.
        assertSame(original.getWiki(), copy.getWiki());

        // Verify that the store session has been cleaned for both context.
        assertNull(original.get(HIBSESSION));
        assertNull(copy.get(HIBSESSION));

        // Check that the URLFactory is cloned.
        assertNotNull(copy.getURLFactory());
        assertNotSame(original.getURLFactory(), copy.getURLFactory());
    }
}
