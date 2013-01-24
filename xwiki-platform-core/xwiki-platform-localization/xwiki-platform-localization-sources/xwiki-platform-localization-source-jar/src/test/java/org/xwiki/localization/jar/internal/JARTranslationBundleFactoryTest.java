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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.messagetool.internal.MessageToolTranslationMessageParser;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

@ComponentList({JARTranslationBundleFactory.class, MessageToolTranslationMessageParser.class,
PlainTextBlockParser.class, ContextComponentManagerProvider.class})
public class JARTranslationBundleFactoryTest
{
    @Rule
    public final MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private TranslationBundleFactory factory;

    private InstalledExtensionRepository mockInstalledExtensionRepository;

    @Before
    public void setUp() throws Exception
    {
        // Mocks

        this.componentManager.registerMockComponent(ComponentManagerManager.class);
        this.componentManager.registerMockComponent(ObservationManager.class);
        this.mockInstalledExtensionRepository =
            this.componentManager.registerMockComponent(InstalledExtensionRepository.class);

        // Components

        this.factory = this.componentManager.getInstance(TranslationBundleFactory.class, "jar");
    }

    // tests

    @Test
    public void getBundle() throws TranslationBundleDoesNotExistsException
    {
        Mockito.when(mockInstalledExtensionRepository.getInstalledExtensions()).thenReturn(
            Arrays.<InstalledExtension> asList());

        this.factory.getBundle("toto");
    }
}
