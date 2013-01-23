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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.internal.DefaultExtensionManagerConfiguration;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

@ComponentList({
    JARTranslationBundleFactory.class,
    DefaultObservationManager.class,
    
})
public class JARTranslationBundleFactoryTest
{
    @Rule
    public final MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @Before
    public void setUp() throws Exception
    {
        this.configuration = this.componentManager.getInstance(ExtensionManagerConfiguration.class);
        
        // checking

        this.observation = getComponentManager().getInstance(ObservationManager.class);

        // Initialiaze document bundle factory
        getComponentManager().getInstance(TranslationBundleFactory.class, "document");

        this.localization = getComponentManager().getInstance(LocalizationManager.class);
    }

    private void assertTranslation(String key, String message, Locale locale)
    {
        Translation translation = this.localization.getTranslation(key, locale);

        if (message != null) {
            Assert.assertNotNull(translation);
            Assert.assertEquals(message, translation.getRawSource());
        } else {
            Assert.assertNull(translation);
        }
    }

    // tests

    @Test
    public void getTranslation() throws XWikiException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        addTranslation("wiki.translation", "Wiki translation", new DocumentReference(getContext().getDatabase(),
            "space", "translation"), Locale.ROOT, Scope.WIKI);

        assertTranslation("wiki.translation", "Wiki translation", Locale.ROOT);
    }
}
