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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiContextContextStore}.
 * 
 * @version $Id$
 */
@OldcoreTest
public class XWikiContextContextStoreTest
{
    private static final String WIKI = "wiki";

    private static final String REQUESTWIKI = "requestwiki";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private XWikiContextContextStore store;

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

        this.wikiURL = new URL("http", "host", 42, "file");
        doReturn(this.wikiURL).when(this.oldcore.getSpyXWiki()).getServerURL(this.descriptor.getId(),
            this.oldcore.getXWikiContext());

        this.requestwikiURL = new URL("https", "host2", 84, "file2");
        doReturn(this.requestwikiURL).when(this.oldcore.getSpyXWiki()).getServerURL(REQUESTWIKI,
            this.oldcore.getXWikiContext());
    }

    @Test
    public void saveEmpty()
    {
        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, Collections.emptySet());

        assertTrue(contextStore.isEmpty());
    }

    @Test
    public void save()
    {
        this.oldcore.getXWikiContext().setWikiId(WIKI);

        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, this.store.getSupportedEntries());

        assertEquals(WIKI, contextStore.get(XWikiContextContextStore.PROP_WIKI));
    }

    @Test
    public void saveRequestwiki()
    {
        this.oldcore.getXWikiContext().setWikiId(WIKI);
        this.oldcore.getXWikiContext().setOriginalWikiId(this.oldcore.getXWikiContext().getWikiId());

        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, this.store.getSupportedEntries());

        assertFalse(contextStore.containsKey(XWikiContextContextStore.PROP_REQUEST_WIKI));

        this.oldcore.getXWikiContext().setOriginalWikiId(REQUESTWIKI);

        this.store.save(contextStore, this.store.getSupportedEntries());

        assertEquals(REQUESTWIKI, contextStore.get(XWikiContextContextStore.PROP_REQUEST_WIKI));
    }

    @Test
    public void restoreEmpty()
    {
        this.store.restore(new HashMap<>());
    }

    @Test
    public void restoreWiki()
    {
        assertNotEquals(WIKI, this.oldcore.getXWikiContext().getWikiId());
        assertNull(this.oldcore.getXWikiContext().getRequest());
        assertNull(this.oldcore.getXWikiContext().getDoc());

        Map<String, Serializable> contextStore = new HashMap<>();
        contextStore.put(XWikiContextContextStore.PROP_WIKI, WIKI);

        this.store.restore(contextStore);

        assertEquals(WIKI, this.oldcore.getXWikiContext().getWikiId());
        assertEquals(this.descriptor.getMainPageReference(),
            this.oldcore.getXWikiContext().getDoc().getDocumentReference());
        assertEquals(this.wikiURL.toString(), this.oldcore.getXWikiContext().getRequest().getRequestURL().toString());
        assertFalse(((XWikiServletRequestStub) this.oldcore.getXWikiContext().getRequest()).isDaemon());
    }
}
