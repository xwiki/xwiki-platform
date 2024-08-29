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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.environment.Environment;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LocalizationScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class LocalizationScriptServiceTest
{
    @InjectMockComponents
    private LocalizationScriptService localizationScriptService;

    @MockComponent
    private LocalizationContext localizationContext;

    @MockComponent
    private LocalizationManager localizationManager;

    @MockComponent
    private Environment environment;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.localizationManager.getTranslation(eq("key"), eq(Locale.ROOT), eq(Syntax.PLAIN_1_0), any()))
            .thenReturn("messageWithParam");
        when(this.localizationManager.getTranslation("key", Locale.ROOT, Syntax.PLAIN_1_0)).thenReturn(
            "messageNoParam");
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.ROOT);
    }

    @Test
    void render()
    {
        assertEquals("messageNoParam", this.localizationScriptService.render("key"));
        assertEquals("messageNoParam",
            this.localizationScriptService.render(Arrays.asList(null, "not existing key", "key", "another key")));
    }

    @Test
    void renderWithSyntax()
    {
        assertEquals("messageNoParam", this.localizationScriptService.render("key", Syntax.PLAIN_1_0));
        assertEquals("messageNoParam", this.localizationScriptService
            .render(Arrays.asList(null, "not existing key", "key", "another key"), Syntax.PLAIN_1_0));
    }

    @Test
    void renderWithSyntaxAndParameters()
    {
        assertEquals("messageWithParam",
            this.localizationScriptService.render("key", Syntax.PLAIN_1_0, List.of("param")));
        assertEquals("messageWithParam", this.localizationScriptService
            .render(Arrays.asList(null, "not existing key", "key", "another key"), Syntax.PLAIN_1_0, List.of("param")));
    }

    @Test
    void renderWithParameters()
    {
        assertEquals("messageWithParam", this.localizationScriptService.render("key", List.of("param")));
        assertEquals("messageWithParam", this.localizationScriptService
            .render(Arrays.asList(null, "not existing key", "key", "another key"), List.of("param")));
    }

    @Test
    void getCurrentLocale()
    {
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, this.localizationScriptService.getCurrentLocale());
    }

    @Test
    void getAvailableLocales()
    {
        when(this.environment.getResourceAsStream(eq("/WEB-INF/xwiki-locales.txt")))
            .thenReturn(getClass().getResourceAsStream("/xwiki-locales.txt"));
        Set<Locale> locales = this.localizationScriptService.getAvailableLocales();
        assertNotNull(locales);
        assertFalse(locales.isEmpty());
        assertTrue(locales.contains(new Locale("fr")));
        assertTrue(locales.contains(new Locale("it")));
        assertTrue(locales.contains(new Locale("mr","IN")));
        assertFalse(locales.contains(new Locale("whatever")));
    }
}
