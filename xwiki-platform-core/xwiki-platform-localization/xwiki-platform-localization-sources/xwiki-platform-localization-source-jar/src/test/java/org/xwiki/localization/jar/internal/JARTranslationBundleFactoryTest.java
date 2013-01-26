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

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.internal.embed.EmbeddableComponentManagerFactory;
import org.xwiki.component.internal.multi.DefaultComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtension;
import org.xwiki.extension.repository.internal.local.DefaultLocalExtension;
import org.xwiki.extension.test.ExtensionPackager;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactoryDoesNotExistsException;
import org.xwiki.localization.internal.DefaultLocalizationManager;
import org.xwiki.localization.internal.DefaultTranslationBundleContext;
import org.xwiki.localization.messagetool.internal.MessageToolTranslationMessageParser;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

@ComponentList({JARTranslationBundleFactory.class, MessageToolTranslationMessageParser.class,
PlainTextBlockParser.class, ContextComponentManagerProvider.class, DefaultLocalizationManager.class,
DefaultTranslationBundleContext.class, DefaultExecution.class, DefaultObservationManager.class,
JARTranslationBundleFactoryListener.class, DefaultComponentManagerManager.class,
EmbeddableComponentManagerFactory.class})
public class JARTranslationBundleFactoryTest
{
    @Rule
    public final MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private LocalizationManager localizationManager;

    private ObservationManager observationManager;

    private InstalledExtensionRepository mockInstalledExtensionRepository;

    private ExtensionPackager extensionPackager;

    @Before
    public void setUp() throws Exception
    {
        this.extensionPackager = new ExtensionPackager(null, new File("target/test-" + new Date().getTime()));
        this.extensionPackager.generateExtensions();

        // Mocks

        this.mockInstalledExtensionRepository =
            this.componentManager.registerMockComponent(InstalledExtensionRepository.class);

        // Components

        this.localizationManager = this.componentManager.getInstance(LocalizationManager.class);
    }

    private ObservationManager getObservationManager() throws ComponentLookupException
    {
        if (this.observationManager == null) {
            this.observationManager = this.componentManager.getInstance(ObservationManager.class);
        }

        return this.observationManager;
    }

    private DefaultInstalledExtension mockInstalledExtension(ExtensionId extensionId, String namespace)
    {
        DefaultLocalExtension localExtension = new DefaultLocalExtension(null, extensionId, "jar");
        localExtension.setFile(this.extensionPackager.getExtensionFile(extensionId));

        DefaultInstalledExtension installedExtension = new DefaultInstalledExtension(localExtension, null);

        installedExtension.setInstalled(true, namespace);

        return installedExtension;
    }

    private void mockInstallExtension(ExtensionId extensionId, String namespace) throws ComponentLookupException
    {
        DefaultInstalledExtension installedExtension = mockInstalledExtension(extensionId, namespace);

        getObservationManager().notify(new ExtensionInstalledEvent(extensionId, namespace), installedExtension);
    }

    private void mockUninstallExtension(ExtensionId extensionId, String namespace) throws ComponentLookupException
    {
        DefaultInstalledExtension installedExtension = mockInstalledExtension(extensionId, namespace);

        getObservationManager().notify(new ExtensionUninstalledEvent(extensionId, namespace), installedExtension);
    }

    private void assertTranslation(String key, String message, Locale locale)
    {
        Translation translation = this.localizationManager.getTranslation(key, locale);

        if (message != null) {
            Assert.assertNotNull("Could not find translation for key [" + key + "] and locale [" + locale + "]",
                translation);
            Assert.assertEquals(message, translation.getRawSource());
        } else {
            Assert.assertNull("Found translation for key [" + key + "] and locale [" + locale + "]", translation);
        }
    }

    // tests

    @Test
    public void installEmptyJar() throws TranslationBundleDoesNotExistsException,
        TranslationBundleFactoryDoesNotExistsException, ComponentLookupException
    {
        ExtensionId extensionId = new ExtensionId("emptyjar", "1.0");

        mockInstallExtension(extensionId, null);

        Assert.assertNotNull(this.localizationManager.getTranslationBundle("jar", this.extensionPackager
            .getExtensionFile(extensionId).toURI().toString()));
    }

    @Test
    public void installJar() throws ComponentLookupException
    {
        ExtensionId extensionId = new ExtensionId("jar", "1.0");

        mockInstallExtension(extensionId, null);

        assertTranslation("test.key", "default translation", Locale.ROOT);
        assertTranslation("test.key", "en translation", Locale.ENGLISH);
        assertTranslation("test.key", "en_US translation", Locale.US);
    }

    @Test
    public void uninstallExtension() throws ComponentLookupException
    {
        ExtensionId extensionId = new ExtensionId("jar", "1.0");

        mockInstallExtension(extensionId, null);

        assertTranslation("test.key", "default translation", Locale.ROOT);

        mockUninstallExtension(extensionId, null);

        assertTranslation("test.key", null, Locale.ROOT);
    }

    @Test
    public void getInstalledJarTranslations() throws ComponentLookupException
    {
        ExtensionId extensionId = new ExtensionId("jar", "1.0");

        Mockito.when(mockInstalledExtensionRepository.getInstalledExtensions()).thenReturn(
            Arrays.<InstalledExtension> asList(mockInstalledExtension(extensionId, null)));

        // Trigger initialization
        getObservationManager();

        assertTranslation("test.key", "default translation", Locale.ROOT);
        assertTranslation("test.key", "en translation", Locale.ENGLISH);
        assertTranslation("test.key", "en_US translation", Locale.US);
    }
}
