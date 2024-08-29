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
package org.xwiki.rest.internal.resources;

import java.net.URI;

import javax.inject.Provider;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Client;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientResourceImpl}.
 * 
 * @version $Id$
 */
@ComponentTest
class ClientResourceImplTest
{
    @InjectMockComponents
    private ClientResourceImpl clientResource;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWikiRequest request;

    @BeforeComponent
    void beforeComponent()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);

        this.request = mock(XWikiRequest.class);
        when(this.xcontext.getRequest()).thenReturn(this.request);
    }

    @BeforeEach
    void configure() throws Exception
    {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(new URI("/xwiki/rest"));
        when(uriInfo.getAbsolutePath()).thenReturn(new URI("/xwiki/rest/client"));
        ReflectionUtils.setFieldValue(this.clientResource, "uriInfo", uriInfo);
    }

    @Test
    void getClient()
    {
        when(this.request.getRemoteAddr()).thenReturn("172.12.0.1");

        Client client = this.clientResource.getClient();
        assertEquals("172.12.0.1", client.getIp());

        assertEquals(2, client.getLinks().size());

        Link parentLink = client.getLinks().get(0);
        assertEquals(Relations.PARENT, parentLink.getRel());
        assertEquals("/xwiki/rest/", parentLink.getHref());

        Link selfLink = client.getLinks().get(1);
        assertEquals(Relations.SELF, selfLink.getRel());
        assertEquals("/xwiki/rest/client", selfLink.getHref());
    }
}
