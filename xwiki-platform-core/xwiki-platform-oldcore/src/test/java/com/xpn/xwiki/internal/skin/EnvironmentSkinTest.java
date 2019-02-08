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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.environment.Environment;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.test.AllLogRule;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EnvironmentSkin}.
 *
 * @version $Id$
 */
public class EnvironmentSkinTest
{
    @Rule
    public AllLogRule allLogRule = new AllLogRule();

    private Skin skin;

    private Environment environment = mock(Environment.class);

    @Before
    public void setUp()
    {
        @SuppressWarnings("unchecked")
        Provider<XWikiContext> xcontextProvider = mock(Provider.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        URLConfiguration urlConfiguration = mock(URLConfiguration.class);

        this.skin = new EnvironmentSkin("test", mock(InternalSkinManager.class), mock(InternalSkinConfiguration.class),
            mock(Logger.class), this.environment, xcontextProvider, urlConfiguration);
    }

    @Test
    public void getLocalResource() throws Exception
    {
        String relativePath = "o;ne/t?w&o/../t=hr#e e";
        String fullPath = "/skins/test/" + relativePath;
        when(this.environment.getResource(fullPath)).thenReturn(new URL("http://resourceURL"));

        Resource<?> resource = skin.getLocalResource(relativePath);
        assertEquals(fullPath, resource.getPath());
    }

    @Test
    public void getLocalResourceWithBreakInAttempt() throws Exception
    {
        assertNull(skin.getLocalResource("one/../../two"));
        assertEquals("Direct access to template file [/skins/two] refused. Possible break-in attempt!",
            allLogRule.getMessage(0));
    }
}
