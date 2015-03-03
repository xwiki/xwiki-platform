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
package org.xwiki.lesscss.internal.compiler;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiEngineContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.compiler.DefaultLESSSkinFileCompiler}.
 *
 * @since 6.1M1
 * @version $Id$
 */
public class DefaultLESSSkinFileCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSSkinFileCompiler> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSSkinFileCompiler.class);

    private LESSCompiler lessCompiler;

    private LESSResourcesCache cache;

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private CurrentColorThemeGetter currentColorThemeGetter;

    private DocumentReferenceResolver<String> referenceResolver;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSCompiler.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        cache = mocker.getInstance(LESSResourcesCache.class);
        currentColorThemeGetter = mocker.getInstance(CurrentColorThemeGetter.class);
        referenceResolver = mocker.getInstance(new DefaultParameterizedType(null, DocumentReferenceResolver.class,
                String.class));
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        when(xwiki.getSkin(xcontext)).thenReturn("skin");

        when(currentColorThemeGetter.getCurrentColorTheme(true, "default")).thenReturn("wikiId:ColorTheme.MyColorTheme");

    }
}
