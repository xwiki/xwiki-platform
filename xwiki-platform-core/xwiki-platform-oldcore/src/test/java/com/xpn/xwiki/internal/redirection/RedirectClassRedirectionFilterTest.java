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
package com.xpn.xwiki.internal.redirection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.RedirectClassDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link RedirectClassRedirectionFilter}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@ComponentTest
class RedirectClassRedirectionFilterTest
{
    private static final WikiReference WIKI_TEST = new WikiReference("wikiTest");

    private static final DocumentReference NEW_LOCATION = new DocumentReference("wikiTest", "location", "newLocation");

    @InjectMockComponents
    private RedirectClassRedirectionFilter redirectClassRedirectionFilter;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private ResourceReferenceManager resourceReferenceManager;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject redirectObj;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiResponse response;

    @BeforeEach
    void setUp()
    {
        when(this.context.getWikiReference()).thenReturn(WIKI_TEST);
        when(this.context.getDoc()).thenReturn(this.doc);
        when(this.context.getAction()).thenReturn("view");
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.context.getResponse()).thenReturn(this.response);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.resolver.resolve("newLocation", WIKI_TEST)).thenReturn(NEW_LOCATION);
        EntityResourceReference entityResourceReference =
            new EntityResourceReference(new DocumentReference("wikiTest", "location", "WebHome"),
                new EntityResourceAction("view"));
        when(this.resourceReferenceManager.getResourceReference()).thenReturn(entityResourceReference);
    }

    @Test
    void redirectNoObject() throws Exception
    {
        assertFalse(this.redirectClassRedirectionFilter.redirect(this.context));
    }

    @Test
    void redirectNoLocation() throws Exception
    {
        when(this.doc.getXObject(RedirectClassDocumentInitializer.REFERENCE)).thenReturn(this.redirectObj);
        assertFalse(this.redirectClassRedirectionFilter.redirect(this.context));
        verify(this.redirectObj).getStringValue("location");
    }

    @Test
    void redirect() throws Exception
    {
        when(this.doc.getXObject(RedirectClassDocumentInitializer.REFERENCE)).thenReturn(this.redirectObj);
        when(this.redirectObj.getStringValue("location")).thenReturn("newLocation");
        when(this.request.getQueryString()).thenReturn("key=value");
        when(this.wiki.getURL(any(EntityReference.class), eq("view"), eq("key=value"), isNull(),
            eq(this.context))).thenReturn("http://newurl");
        assertTrue(this.redirectClassRedirectionFilter.redirect(this.context));
        verify(this.response).sendRedirect("http://newurl");
    }
}
