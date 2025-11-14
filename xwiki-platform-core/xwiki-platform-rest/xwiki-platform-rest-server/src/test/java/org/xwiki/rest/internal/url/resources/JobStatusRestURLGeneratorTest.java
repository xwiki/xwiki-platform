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
package org.xwiki.rest.internal.url.resources;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the behaviour of {@link JobStatusRestURLGenerator}.
 *
 * @version $Id$
 * @since 11.10.5
 * @since 12.3RC1
 */
@ComponentTest
class JobStatusRestURLGeneratorTest
{
    @InjectMockComponents
    private JobStatusRestURLGenerator jobStatusRestURLGenerator;

    @BeforeEach
    void setup(ComponentManager componentManager) throws Exception
    {
        Provider<XWikiContext> xwikiContextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext xwikiContext = xwikiContextProvider.get();
        XWiki xwiki = mock(XWiki.class);
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);

        when(xwikiContext.getWiki()).thenReturn(xwiki);
        when(xwikiContext.getURLFactory()).thenReturn(urlFactory);
        when(xwiki.getWebAppPath(xwikiContext)).thenReturn("/");
        when(urlFactory.getServerURL(xwikiContext)).thenReturn(new URL("http://localhost"));
    }

    @Test
    void getURL() throws Exception
    {
        List<String> stringList = Arrays.asList("flavor", "search", "wiki:xwiki");
        URL expectedUrl = new URL("http://localhost/rest/jobstatus/flavor/search/wiki%3Asynchronizedxwiki");
        assertEquals(expectedUrl, this.jobStatusRestURLGenerator.getURL(stringList));
    }
}
