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
package org.xwiki.webjars;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.webjars.script.WebJarsScriptService;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.webjars.script.WebJarsScriptService}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class WebJarsScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<WebJarsScriptService> mocker =
        new MockitoComponentMockingRule<WebJarsScriptService>(WebJarsScriptService.class);

    private XWikiContext xcontext = mock(XWikiContext.class);

    private XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);

    @Before
    public void configure() throws Exception
    {
        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getURLFactory()).thenReturn(this.urlFactory);
    }

    @Test
    public void computeURLWithVersion() throws Exception
    {
        URL url = new URL("http://www.xwiki.org");
        when(urlFactory.createURL("resources", "path", "webjars", "value=ang%3Aular%2F2.1.11%2Fangular.js",
            null, xcontext)).thenReturn(url);
        when(urlFactory.getURL(url, xcontext)).thenReturn("foo");

        // Test that colon is not interpreted as groupId/artifactId separator (for backwards compatibility).
        assertEquals("foo", this.mocker.getComponentUnderTest().url("ang:ular/2.1.11/angular.js"));
    }

    @Test
    public void computeURLWithoutVersion() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("math");

        InstalledExtensionRepository installedExtensionRepository =
            this.mocker.getInstance(InstalledExtensionRepository.class);
        InstalledExtension extension = mock(InstalledExtension.class);
        when(installedExtensionRepository.getInstalledExtension("org.webjars:angular", "wiki:math")).thenReturn(
            extension);
        when(extension.getId()).thenReturn(new ExtensionId("bar", "2.1.11"));

        URL url = new URL("http://www.xwiki.org");
        when(urlFactory.createURL("resources", "path", "webjars", "value=angular%2F2.1.11%2Fangular.js",
            null, xcontext)).thenReturn(url);
        when(urlFactory.getURL(url, xcontext)).thenReturn("foo");

        assertEquals("foo", this.mocker.getComponentUnderTest().url("angular", "angular.js"));
    }

    @Test
    public void computeURLWithParameters() throws Exception
    {
        URL url = new URL("http://www.xwiki.org");
        when(urlFactory.createURL("resources", "path", "webjars",
            "value=angular%2F2.1.11%2Fangular.js&evaluate=true&list=one&list=two", null, xcontext)).thenReturn(url);
        when(urlFactory.getURL(url, xcontext)).thenReturn("foo");

        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("version", "2.1.11");
        params.put("value", "will be overwritten");
        params.put("evaluate", true);
        params.put("list", new String[] {"one", "two"});
        assertEquals("foo", this.mocker.getComponentUnderTest().url("angular", "angular.js", params));
    }
}
