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
package org.xwiki.localization.jar.internal;

import java.util.Locale;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.internal.DefaultLocalizationManager;
import org.xwiki.localization.internal.DefaultTranslationBundleContext;
import org.xwiki.localization.messagetool.internal.MessageToolTranslationMessageParser;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ComponentList({MessageToolTranslationMessageParser.class, PlainTextBlockParser.class,
    ContextComponentManagerProvider.class, DefaultLocalizationManager.class, DefaultTranslationBundleContext.class,
    DefaultExecution.class, DefaultModelContext.class, RootClassLoaderTranslationBundle.class})
@ComponentTest
public class RootClassLoaderTranslationBundleTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("plain/1.0")
    private BlockRenderer plainRenderer;

    private LocalizationManager localizationManager;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        // Components

        this.localizationManager = this.componentManager.getInstance(LocalizationManager.class);
    }

    private void assertTranslation(String key, String message, Locale locale)
    {
        Translation translation = this.localizationManager.getTranslation(key, locale);

        if (message != null) {
            assertNotNull(translation, "Could not find translation for key [" + key + "] and locale [" + locale + "]");
            assertEquals(message, translation.getRawSource());
        } else {
            assertNull(translation, "Found translation for key [" + key + "] and locale [" + locale + "]");
        }
    }

    // tests

    @Test
    void getTranslations()
    {
        assertTranslation("test.key", "default translation", Locale.ROOT);
        assertTranslation("test.key", "en translation", Locale.ENGLISH);
        assertTranslation("test.key", "en_US translation", Locale.US);
    }
}
