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
package com.xpn.xwiki.internal.skin;

import java.net.URL;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.environment.Environment;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EnvironmentSkinResource}.
 *
 * @version $Id$
 */
@ComponentTest
class EnvironmentSkinResourceTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private SkinActionURLParameters skinActionURLParameters;

    private EnvironmentSkinResource resource;

    private XWikiContext xcontext;

    private XWikiURLFactory urlFactory;

    @BeforeEach
    void setUp()
    {
        Utils.setComponentManager(this.componentManager);

        @SuppressWarnings("unchecked")
        Provider<XWikiContext> xcontextProvider = mock(Provider.class);
        this.xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getVersion()).thenReturn("1.0");
        this.urlFactory = mock(XWikiURLFactory.class);
        when(this.xcontext.getURLFactory()).thenReturn(this.urlFactory);

        ResourceRepository repository = mock(ResourceRepository.class);
        when(repository.getId()).thenReturn("flamingo");

        this.resource = new EnvironmentSkinResource("/skins/flamingo/test.js", "test.js", repository,
            mock(Environment.class), xcontextProvider, mock(URLConfiguration.class));

        doAnswer(invocation -> {
            Map<String, Object> parameters = invocation.getArgument(0);
            parameters.put("iconTheme", "Silk");
            return null;
        }).when(this.skinActionURLParameters).addParameters(any());
    }

    @Test
    void getURLWithSkinAction() throws Exception
    {
        URL url = new URL("http://host/xwiki/bin/skin/skins/flamingo/test.js?cache-version=1.0&iconTheme=Silk");
        when(this.urlFactory.createSkinURL("test.js", "skins", "flamingo", this.xcontext,
            Map.of("cache-version", "1.0", "iconTheme", "Silk"))).thenReturn(url);
        when(this.urlFactory.getURL(url, this.xcontext)).thenReturn(url.toString());

        assertEquals(url.toString(), this.resource.getURL(true));
    }

    @Test
    void getURLWithoutSkinAction() throws Exception
    {
        URL url = new URL("http://host/xwiki/skins/flamingo/test.js?cache-version=1.0");
        when(this.urlFactory.createSkinURL("test.js", "flamingo", this.xcontext, Map.of("cache-version", "1.0")))
            .thenReturn(url);
        when(this.urlFactory.getURL(url, this.xcontext)).thenReturn(url.toString());

        assertEquals(url.toString(), this.resource.getURL(false));

        // The resource is not evaluated by the skin action, so there's no need to differentiate its URL.
        verify(this.skinActionURLParameters, never()).addParameters(any());
    }
}
