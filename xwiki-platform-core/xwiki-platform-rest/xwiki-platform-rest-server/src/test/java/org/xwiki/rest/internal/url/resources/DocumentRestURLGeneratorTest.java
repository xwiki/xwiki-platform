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
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentRestURLGenerator}.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@ComponentTest
class DocumentRestURLGeneratorTest
{
    @InjectMockComponents
    private DocumentRestURLGenerator generator;

    private XWikiContext xwikiContext;

    private XWiki xwiki;

    private XWikiURLFactory urlFactory;

    @BeforeEach
    void setup(ComponentManager componentManager) throws Exception
    {
        Provider<XWikiContext> xwikiContextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        xwikiContext = xwikiContextProvider.get();
        xwiki = mock(XWiki.class);
        urlFactory = mock(XWikiURLFactory.class);

        when(xwikiContext.getWiki()).thenReturn(xwiki);
        when(xwikiContext.getURLFactory()).thenReturn(urlFactory);
        when(xwiki.getWebAppPath(xwikiContext)).thenReturn("/");
        when(urlFactory.getServerURL(xwikiContext)).thenReturn(new URL("http://localhost"));
    }

    @Test
    void getURLWithoutLocale() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        assertEquals("http://localhost/rest/wikis/wiki/spaces/space/pages/page",
            this.generator.getURL(documentReference).toString());
    }

    @Test
    void getURLWitTrailingServerURLSlash() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(urlFactory.getServerURL(xwikiContext)).thenReturn(new URL("http://localhost2/"));
        assertEquals("http://localhost2/rest/wikis/wiki/spaces/space/pages/page",
            this.generator.getURL(documentReference).toString());
    }

    @Test
    void getURLWithLocale() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page", Locale.FRENCH);
        assertEquals("http://localhost/rest/wikis/wiki/spaces/space/pages/page/translations/fr",
            this.generator.getURL(documentReference).toString());
    }

    @Test
    void getURLWithSlash() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "pa/ge");
        assertEquals("http://localhost/rest/wikis/wiki/spaces/space/pages/pa%2Fge",
            this.generator.getURL(documentReference).toString());
    }

    @Test
    void getURLWithSeveralSpaces() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", List.of("space1", "space2"), "page");
        assertEquals("http://localhost/rest/wikis/wiki/spaces/space1/spaces/space2/pages/page",
            this.generator.getURL(documentReference).toString());
    }
}
