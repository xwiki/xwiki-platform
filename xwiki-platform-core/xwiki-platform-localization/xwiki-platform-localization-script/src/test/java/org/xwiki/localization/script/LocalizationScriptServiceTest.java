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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class LocalizationScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<ScriptService> mocker =
        new MockitoComponentMockingRule<>(LocalizationScriptService.class);

    private BlockRenderer renderer;

    private ComponentManager componentManager;

    private LocalizationContext localizationContext;

    private LocalizationManager localizationManager;

    private LocalizationScriptService localizationScriptService;

    private Environment environment;

    @Before
    public void setUp() throws Exception
    {
        localizationContext = mocker.getInstance(LocalizationContext.class);
        localizationManager = mocker.getInstance(LocalizationManager.class);
        localizationScriptService = (LocalizationScriptService) mocker.getComponentUnderTest();

        when(localizationManager.getTranslation(eq("key"), eq(Locale.ROOT), eq(Syntax.PLAIN_1_0), any())).thenReturn(
            "messageWithParam");
        when(localizationManager.getTranslation(eq("key"), eq(Locale.ROOT), eq(Syntax.PLAIN_1_0))).thenReturn(
            "messageNoParam");
        when(localizationContext.getCurrentLocale()).thenReturn(Locale.ROOT);

        environment = mocker.getInstance(Environment.class);
    }

    @Test
    public void render() throws Exception
    {
        assertEquals("messageNoParam", localizationScriptService.render("key"));
        assertEquals("messageNoParam",
            localizationScriptService.render(Arrays.asList(null, "not existing key", "key", "another key")));
    }

    @Test
    public void renderWithSyntax()
    {
        assertEquals("messageNoParam", localizationScriptService.render("key", Syntax.PLAIN_1_0));
        assertEquals("messageNoParam", localizationScriptService
            .render(Arrays.asList(null, "not existing key", "key", "another key"), Syntax.PLAIN_1_0));
    }

    @Test
    public void renderWithSyntaxAndParameters()
    {
        assertEquals("messageWithParam", localizationScriptService.render("key", Syntax.PLAIN_1_0, List.of("param")));
        assertEquals("messageWithParam", localizationScriptService
            .render(Arrays.asList(null, "not existing key", "key", "another key"), Syntax.PLAIN_1_0, List.of("param")));
    }

    @Test
    public void renderWithParameters()
    {
        assertEquals("messageWithParam", localizationScriptService.render("key", List.of("param")));
        assertEquals("messageWithParam", localizationScriptService
            .render(Arrays.asList(null, "not existing key", "key", "another key"), List.of("param")));
    }

    @Test
    public void getCurrentLocale()
    {
        when(localizationContext.getCurrentLocale()).thenReturn(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, localizationScriptService.getCurrentLocale());
    }

    @Test
    public void getAvailableLocales()
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
