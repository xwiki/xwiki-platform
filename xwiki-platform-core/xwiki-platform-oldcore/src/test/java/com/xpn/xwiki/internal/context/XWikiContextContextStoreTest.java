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
package com.xpn.xwiki.internal.context;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.transformation.RenderingContextStore;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiContextContextStore}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(RequestInitializer.class)
class XWikiContextContextStoreTest
{
    private static final String WIKI = "wiki";

    private static final String REQUESTWIKI = "requestwiki";

    @MockComponent
    private Container container;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private XWikiContextContextStore store;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    private WikiDescriptor descriptor;

    private URL wikiURL;

    private URL requestwikiURL;

    @BeforeEach
    public void beforeEach() throws WikiManagerException, ComponentLookupException, MalformedURLException
    {
        this.descriptor = new WikiDescriptor(WIKI, WIKI);
        this.descriptor.setMainPageReference(new DocumentReference(this.descriptor.getId(), "Space", "MainPage"));

        when(this.oldcore.getWikiDescriptorManager().getById(this.descriptor.getId())).thenReturn(this.descriptor);

        doReturn("webapppath").when(this.oldcore.getSpyXWiki()).getWebAppPath(this.oldcore.getXWikiContext());

        this.wikiURL = new URL("http", "host", 42, "/file");
        doReturn(this.wikiURL).when(this.oldcore.getSpyXWiki()).getServerURL(this.descriptor.getId(),
            this.oldcore.getXWikiContext());

        this.requestwikiURL = new URL("https", "host2", 84, "/file2");
        doReturn(this.requestwikiURL).when(this.oldcore.getSpyXWiki()).getServerURL(REQUESTWIKI,
            this.oldcore.getXWikiContext());
    }

    @Test
    void saveEmpty()
    {
        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, Collections.emptySet());

        assertTrue(contextStore.isEmpty());
    }

    @Test
    void save()
    {
        this.oldcore.getXWikiContext().setWikiId(WIKI);

        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, this.store.getSupportedEntries());

        assertEquals(WIKI, contextStore.get(XWikiContextContextStore.PROP_WIKI));
    }

    @SuppressWarnings("unchecked")
    @Test
    void saveAndRestoreRequest() throws Exception
    {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("param1", new String[] {"value1", "value2"});
        Cookie[] cookies = new Cookie[] {new Cookie("color", "red")};
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("User-Agent", Collections.singletonList("test"));
        headers.put("X-Color", Arrays.asList("blue", "green"));
        HttpSession session = mock(HttpSession.class);
        XWikiServletRequestStub request = new XWikiServletRequestStub.Builder().setRequestURL(this.wikiURL)
            .setContextPath("/test").setRequestParameters(parameters).setCookies(cookies).setHeaders(headers)
            .setRemoteAddr("172.12.0.2").setHttpSession(session).build();
        this.oldcore.getXWikiContext().setRequest(request);

        Map<String, Serializable> contextStore = new HashMap<>();

        // Save

        this.store.save(contextStore, Arrays.asList(XWikiContextContextStore.PREFIX_PROP_REQUEST));

        assertEquals(7, contextStore.size());
        assertEquals(this.wikiURL.toString(), contextStore.get(XWikiContextContextStore.PROP_REQUEST_URL).toString());
        assertEquals("/test", contextStore.get(XWikiContextContextStore.PROP_REQUEST_CONTEXTPATH));

        Map<String, String[]> storedParameters =
            (Map<String, String[]>) contextStore.get(XWikiContextContextStore.PROP_REQUEST_PARAMETERS);
        assertEquals(1, storedParameters.size());
        Map.Entry<String, String[]> entry = storedParameters.entrySet().iterator().next();
        assertEquals("param1", entry.getKey());
        assertEquals(Arrays.asList(parameters.get("param1")), Arrays.asList(entry.getValue()));

        Cookie[] storedCookies = (Cookie[]) contextStore.get(XWikiContextContextStore.PROP_REQUEST_COOKIES);
        assertEquals(1, storedCookies.length);
        assertEquals(cookies[0].getName(), storedCookies[0].getName());
        assertEquals(cookies[0].getValue(), storedCookies[0].getValue());

        assertEquals(headers, contextStore.get(XWikiContextContextStore.PROP_REQUEST_HEADERS));
        assertEquals("172.12.0.2", contextStore.get(XWikiContextContextStore.PROP_REQUEST_REMOTE_ADDR));

        assertEquals(session,
            ((SerializableHttpSessionWrapper) contextStore.get(XWikiContextContextStore.PROP_REQUEST_SESSION))
                .getSession());

        // Restore

        this.oldcore.getXWikiContext().setURL(null);
        this.oldcore.getXWikiContext().setRequest(null);

        this.store.restore(contextStore);

        assertEquals(this.wikiURL, this.oldcore.getXWikiContext().getURL());

        request = (XWikiServletRequestStub) this.oldcore.getXWikiContext().getRequest();
        assertEquals(this.wikiURL.toString(), request.getRequestURL().toString());
        assertEquals("/test", request.getContextPath());
        assertArrayEquals(parameters.get("param1"), request.getParameterValues("param1"));
        assertEquals(Arrays.asList("User-Agent", "X-Color"), Collections.list(request.getHeaderNames()));
        assertEquals("test", request.getHeader("uSEr-AgenT"));
        assertEquals(Arrays.asList("blue", "green"), Collections.list(request.getHeaders("x-cOLor")));
        assertEquals("red", request.getCookie("color").getValue());
        assertEquals(1, request.getCookies().length);
        assertEquals("172.12.0.2", request.getRemoteAddr());
        assertEquals(session, request.getSession());
    }

    @Test
    void saveRequestwiki()
    {
        this.oldcore.getXWikiContext().setWikiId(WIKI);
        this.oldcore.getXWikiContext().setOriginalWikiId(this.oldcore.getXWikiContext().getWikiId());

        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, this.store.getSupportedEntries());

        assertFalse(contextStore.containsKey(XWikiContextContextStore.PROP_REQUEST_WIKI));

        this.oldcore.getXWikiContext().setOriginalWikiId(REQUESTWIKI);

        this.store.save(contextStore, this.store.getSupportedEntries());

        assertFalse(contextStore.containsKey(XWikiContextContextStore.PROP_REQUEST_WIKI));

        this.oldcore.getXWikiContext().setRequest(mock(XWikiRequest.class));

        this.store.save(contextStore, this.store.getSupportedEntries());

        assertEquals(REQUESTWIKI, contextStore.get(XWikiContextContextStore.PROP_REQUEST_WIKI));
    }

    @Test
    void restoreEmpty() throws MalformedURLException
    {
        XWikiServletRequestStub request = new XWikiServletRequestStub.Builder().setRequestURL(new URL("http://stub"))
            .setRequestParameters(Collections.singletonMap("parameter", new String[] {"value"})).build();

        this.oldcore.getXWikiContext().setRequest(request);

        this.store.restore(new HashMap<>());

        assertTrue(((XWikiServletRequestStub) this.oldcore.getXWikiContext().getRequest()).isDaemon());
    }

    @Test
    void restoreWiki()
    {
        assertNotEquals(WIKI, this.oldcore.getXWikiContext().getWikiId());
        assertNull(this.oldcore.getXWikiContext().getRequest());
        assertNull(this.oldcore.getXWikiContext().getDoc());
        assertNull(this.oldcore.getXWikiContext().getUserReference());

        Map<String, Serializable> contextStore = new HashMap<>();
        contextStore.put(XWikiContextContextStore.PROP_WIKI, WIKI);

        this.store.restore(contextStore);

        assertEquals(WIKI, this.oldcore.getXWikiContext().getWikiId());
        assertEquals(this.oldcore.getXWikiContext().getUserReference(),
            new DocumentReference("xwiki", "XWiki", XWikiRightService.SUPERADMIN_USER));
        assertEquals(this.descriptor.getMainPageReference(),
            this.oldcore.getXWikiContext().getDoc().getDocumentReference());
        assertEquals(this.wikiURL.toString(), this.oldcore.getXWikiContext().getRequest().getRequestURL().toString());
        assertFalse(((XWikiServletRequestStub) this.oldcore.getXWikiContext().getRequest()).isDaemon());
        assertNull(this.oldcore.getXWikiContext().get(XWikiDocument.CKEY_SDOC));
    }

    @Test
    void restoreDocument()
    {
        DocumentReference document = new DocumentReference("docwiki", "space", "doc");

        Map<String, Serializable> contextStore = new HashMap<>();
        contextStore.put(XWikiContextContextStore.PROP_DOCUMENT_REFERENCE, document);

        this.store.restore(contextStore);

        assertEquals(document, this.oldcore.getXWikiContext().getDoc().getDocumentReference());
        assertEquals("docwiki", this.oldcore.getXWikiContext().getWikiId());
    }

    @Test
    void restoreAuthor() throws XWikiException
    {
        assertNull(this.oldcore.getXWikiContext().getAuthorReference());
        assertNull(this.oldcore.getXWikiContext().get(XWikiDocument.CKEY_SDOC));
        assertNull(this.oldcore.getXWikiContext().getUserReference());

        DocumentReference authorReference = new DocumentReference("authorwiki", "authorspace", "author");

        Map<String, Serializable> contextStore = new HashMap<>();
        contextStore.put(XWikiContextContextStore.PROP_SECURE_AUTHOR, authorReference);

        this.store.restore(contextStore);

        assertEquals(authorReference, this.oldcore.getXWikiContext().getUserReference());

        XWikiDocument secureDocument1 = (XWikiDocument) this.oldcore.getXWikiContext().get(XWikiDocument.CKEY_SDOC);
        assertNotNull(secureDocument1);
        assertEquals(new DocumentReference("authorwiki", "SUSpace", "SUPage"), secureDocument1.getDocumentReference());
        assertEquals(authorReference, secureDocument1.getContentAuthorReference());
        assertEquals(authorReference, this.oldcore.getXWikiContext().getAuthorReference());

        this.store.restore(contextStore);

        assertEquals(authorReference, this.oldcore.getXWikiContext().getUserReference());

        XWikiDocument secureDocument2 = (XWikiDocument) this.oldcore.getXWikiContext().get(XWikiDocument.CKEY_SDOC);
        assertNotNull(secureDocument2);
        assertNotSame(secureDocument1, secureDocument2);
        assertEquals(new DocumentReference("authorwiki", "SUSpace", "SUPage"), secureDocument2.getDocumentReference());
        assertEquals(authorReference, secureDocument2.getContentAuthorReference());
        assertEquals(authorReference, this.oldcore.getXWikiContext().getAuthorReference());

        DocumentReference secureDocumentReference =
            new DocumentReference("securewiki", "securespace", "securedocument");
        contextStore.put(XWikiContextContextStore.PROP_SECURE_DOCUMENT, secureDocumentReference);

        this.store.restore(contextStore);

        assertEquals(authorReference, this.oldcore.getXWikiContext().getUserReference());

        XWikiDocument secureDocument3 = (XWikiDocument) this.oldcore.getXWikiContext().get(XWikiDocument.CKEY_SDOC);
        assertNotNull(secureDocument3);
        assertEquals(secureDocumentReference, secureDocument3.getDocumentReference());
        assertEquals(authorReference, secureDocument3.getContentAuthorReference());
        assertEquals(authorReference, this.oldcore.getXWikiContext().getAuthorReference());
        assertNotSame(this.oldcore.getSpyXWiki().getDocument(secureDocumentReference, this.oldcore.getXWikiContext()),
            secureDocument3);

        contextStore.put(RenderingContextStore.PROP_RESTRICTED, true);

        this.store.restore(contextStore);

        assertNull(this.oldcore.getXWikiContext().getUserReference());
    }

    @Test
    void saveAndRestoreDocumentRevision() throws Exception
    {
        // Save

        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        DocumentReference documentReferenceWithLocale = new DocumentReference(documentReference, Locale.FRENCH);
        XWikiDocument documentRevision = new XWikiDocument(documentReferenceWithLocale);
        documentRevision.setLocale(Locale.FRENCH);
        documentRevision.setVersion("2.5");
        this.oldcore.getXWikiContext().setDoc(documentRevision);
        this.oldcore.getXWikiContext().put("rev", documentRevision.getVersion());

        Map<String, Serializable> contextStore = new HashMap<>();
        this.store.save(contextStore, Arrays.asList(XWikiContextContextStore.PROP_DOCUMENT_REFERENCE,
            XWikiContextContextStore.PROP_DOCUMENT_REVISION));

        assertEquals(documentReferenceWithLocale, contextStore.get(XWikiContextContextStore.PROP_DOCUMENT_REFERENCE));
        assertEquals("2.5", contextStore.get(XWikiContextContextStore.PROP_DOCUMENT_REVISION));

        // Restore

        this.oldcore.getXWikiContext().setDoc(null);
        this.oldcore.getXWikiContext().remove("rev");

        XWikiDocument document = new XWikiDocument(documentReferenceWithLocale);
        when(this.oldcore.getSpyXWiki().getDocument(documentReferenceWithLocale, this.oldcore.getXWikiContext()))
            .thenReturn(document);
        when(this.documentRevisionProvider.getRevision(document, "2.5")).thenReturn(documentRevision);

        this.store.restore(contextStore);

        assertEquals("2.5", this.oldcore.getXWikiContext().get("rev"));
        assertEquals(documentRevision, this.oldcore.getXWikiContext().getDoc());
    }
}
