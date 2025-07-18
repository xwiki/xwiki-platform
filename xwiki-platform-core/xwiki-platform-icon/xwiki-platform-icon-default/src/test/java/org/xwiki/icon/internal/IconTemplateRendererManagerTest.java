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
package org.xwiki.icon.internal;

import jakarta.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.icon.IconException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.internal.util.VelocityDetector;
import org.xwiki.webjars.WebJarsUrlFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IconTemplateRendererManager}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(VelocityDetector.class)
class IconTemplateRendererManagerTest
{
    @InjectMockComponents
    private IconTemplateRendererManager iconTemplateRendererManager;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private VelocityRenderer velocityRenderer;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private WebJarsUrlFactory webJarsUrlFactory;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    private Cache<IconTemplateRenderer> cache;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        CacheFactory cacheFactory = mock();
        when(this.cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        CacheConfiguration configuration = new LRUCacheConfiguration("iconset.renderer", 100);
        this.cache = mock();
        when(cacheFactory.<IconTemplateRenderer>newCache(configuration)).thenReturn(this.cache);
    }

    @Test
    void getRendererShouldRetrieveFromCache() throws Exception
    {
        String template = "icon-template";
        IconTemplateRenderer expectedRenderer = mock();
        when(this.cache.get(template)).thenReturn(expectedRenderer);

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertSame(expectedRenderer, renderer);
        verifyNoInteractions(this.velocityManager, this.velocityRenderer, this.webJarsUrlFactory);
    }

    @Test
    void getRendererShouldHandleNonVelocityTemplate() throws Exception
    {
        String template = "simple-template";

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertEquals(template, renderer.render("icon", null));
        verify(this.cache).set(template, renderer);
        verifyNoInteractions(this.velocityManager, this.velocityRenderer, this.webJarsUrlFactory);
    }

    @Test
    void getRendererShouldHandleSimpleReplacementPattern() throws Exception
    {
        String template = "Hello $icon!";

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertEquals("Hello world!", renderer.render("world", null));
        verifyNoInteractions(this.velocityManager, this.velocityRenderer, this.webJarsUrlFactory);
    }

    @Test
    void getRendererShouldHandleWebJarUrlTemplate() throws Exception
    {
        String template = "$services.webjars.url('some-library')";
        String expectedURL = "https://example.com/some-library";
        when(this.webJarsUrlFactory.url("some-library")).thenReturn(expectedURL);

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertEquals(expectedURL, renderer.render("icon", null));
        verifyNoInteractions(this.velocityManager, this.velocityRenderer);
    }

    @Test
    void getRendererShouldHandleWebJarUrlTemplateWithTwoArguments() throws Exception
    {
        String template = "$services.webjars.url('lib','ver')";
        String expectedURL = "https://example.com/lib/ver";
        when(this.webJarsUrlFactory.url("lib", "ver")).thenReturn(expectedURL);

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertEquals(expectedURL, renderer.render("icon", null));
        verifyNoInteractions(this.velocityManager, this.velocityRenderer);
    }

    @Test
    void getRendererShouldHandleWebJarUrlTemplateWithThreeArguments() throws Exception
    {
        String template = "$services.webjars.url('lib','ver','file.js')";
        String expectedURL = "https://example.com/lib/ver/file.js";
        when(this.webJarsUrlFactory.url("lib", "ver", "file.js")).thenReturn(expectedURL);

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertEquals(expectedURL, renderer.render("icon", null));
        verifyNoInteractions(this.velocityManager, this.velocityRenderer);
    }

    @Test
    void getRendererShouldHandleSkinFileTemplate() throws Exception
    {
        String template =
            "[[image:path:$xwiki.getSkinFile(\"icons/silk/${icon}.png\")||data-xwiki-lightbox=\"false\"]]";
        String expectedSkinFile = "/default/icon-path/home.png";
        String expected = "[[image:path:/default/icon-path/home.png||data-xwiki-lightbox=\"false\"]]";

        XWikiContext mockContext = mock();
        XWiki mockXWiki = mock();
        when(this.xwikiContextProvider.get()).thenReturn(mockContext);
        when(mockContext.getWiki()).thenReturn(mockXWiki);
        when(mockXWiki.getSkinFile("icons/silk/home.png", mockContext)).thenReturn(expectedSkinFile);

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertEquals(expected, renderer.render("home", null));

        verifyNoInteractions(this.velocityManager, this.velocityRenderer);
    }

    @Test
    void getRendererShouldHandleVelocityTemplate() throws Exception
    {
        String template = "#set($icon = 'cool-icon')$icon";
        VelocityTemplate velocityTemplate = mock();
        DocumentReference documentReference = mock();
        when(this.velocityManager.compile(eq(template), assertArg(reader -> {
            assertEquals(template, IOUtils.toString(reader));
        }))).thenReturn(velocityTemplate);
        when(this.velocityRenderer.render(velocityTemplate, "icon", documentReference)).thenReturn("cool-icon");

        IconTemplateRenderer renderer = this.iconTemplateRendererManager.getRenderer(template);

        assertEquals("cool-icon", renderer.render("icon", documentReference));
        verify(this.cache).set(template, renderer);
    }

    @Test
    void getRendererShouldThrowWhenVelocityParserThrows() throws Exception
    {
        String template = "#set($icon = 'fail')";
        RuntimeException velocityException = new RuntimeException("Velocity error");
        when(this.velocityManager.compile(eq(template), any())).thenThrow(velocityException);

        IconException thrown =
            assertThrows(IconException.class, () -> this.iconTemplateRendererManager.getRenderer(template));
        assertEquals("Failed to compile Velocity template: " + template, thrown.getMessage());
        assertEquals(velocityException, thrown.getCause());
        verify(this.cache, never()).set(any(), any());
    }
}
