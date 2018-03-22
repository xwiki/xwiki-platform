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
package org.xwiki.localization.script;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import javax.inject.Provider;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.environment.Environment;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalizationScriptServiceTest
{

    @Rule
    public MockitoComponentMockingRule<ScriptService> mocker =
        new MockitoComponentMockingRule<ScriptService>(LocalizationScriptService.class);

    private BlockRenderer renderer;

    private ComponentManager componentManager;

    private LocalizationContext localizationContext;

    private LocalizationManager localizationManager;

    private LocalizationScriptService localizationScriptService;

    private Translation translation;

    private Environment environment;

    @Before
    public void setUp() throws Exception
    {
        componentManager = mock(ComponentManager.class);
        Provider<ComponentManager> componentManagerProvider = mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(componentManager);

        renderer = mock(BlockRenderer.class, Syntax.PLAIN_1_0.toIdString());
        when(componentManager.getInstance(BlockRenderer.class, Syntax.PLAIN_1_0.toIdString())).thenReturn(renderer);

        localizationContext = mocker.getInstance(LocalizationContext.class);
        localizationManager = mocker.getInstance(LocalizationManager.class);
        localizationScriptService = (LocalizationScriptService) mocker.getComponentUnderTest();
        translation = mock(Translation.class);

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                WikiPrinter printer = (WikiPrinter) invocation.getArguments()[1];
                printer.print("print result");
                return null;
            }
        }).when(renderer).render(eq(new WordBlock("message")), any(WikiPrinter.class));
        when(translation.render(Locale.ROOT, ArrayUtils.EMPTY_OBJECT_ARRAY)).thenReturn(new WordBlock("message"));
        when(localizationManager.getTranslation("key", Locale.ROOT)).thenReturn(translation);
        when(localizationContext.getCurrentLocale()).thenReturn(Locale.ROOT);

        environment = mocker.getInstance(Environment.class);
    }

    @Test
    public void render() throws Exception
    {
        assertEquals("print result", localizationScriptService.render("key"));
        assertEquals("print result",
            localizationScriptService.render(Arrays.asList("not existing key", "key", "another key")));
    }

    @Test
    public void renderWithSyntax() throws Exception
    {
        assertEquals("print result", localizationScriptService.render("key", Syntax.PLAIN_1_0));
        assertEquals("print result", localizationScriptService
            .render(Arrays.asList("not existing key", "key", "another key"), Syntax.PLAIN_1_0));
    }

    @Test
    public void renderWithSyntaxAndParameters() throws Exception
    {
        assertEquals("print result", localizationScriptService.render("key", Syntax.PLAIN_1_0, Arrays.asList()));
        assertEquals("print result", localizationScriptService
            .render(Arrays.asList("not existing key", "key", "another key"), Syntax.PLAIN_1_0, Arrays.asList()));
    }

    @Test
    public void renderWithParameters() throws Exception
    {
        assertEquals("print result", localizationScriptService.render("key", Arrays.asList()));
        assertEquals("print result",
            localizationScriptService.render(Arrays.asList("not existing key", "key", "another key"), Arrays.asList()));
    }

    @Test
    public void getCurrentLocale() throws Exception
    {
        when(localizationContext.getCurrentLocale()).thenReturn(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, localizationScriptService.getCurrentLocale());
    }

    @Test
    public void getAvailableLocales() throws Exception
    {
        when(environment.getResourceAsStream(eq("/WEB-INF/xwiki-locales.txt")))
            .thenReturn(getClass().getResourceAsStream("/xwiki-locales.txt"));
        Set<Locale> locales = localizationScriptService.getAvailableLocales();
        assertNotNull(locales);
        assertFalse(locales.isEmpty());
        assertTrue(locales.contains(new Locale("fr")));
        assertTrue(locales.contains(new Locale("it")));
        assertTrue(locales.contains(new Locale("mr_IN")));
        assertFalse(locales.contains(new Locale("whatever")));
    }
}
