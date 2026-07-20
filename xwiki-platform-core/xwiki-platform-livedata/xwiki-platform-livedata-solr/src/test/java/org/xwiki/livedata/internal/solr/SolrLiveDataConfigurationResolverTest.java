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
package org.xwiki.livedata.internal.solr;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrLiveDataConfigurationResolver}, focusing on the localization of the column headers from the
 * caller-provided {@code translationPrefix} source parameter.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@ComponentTest
class SolrLiveDataConfigurationResolverTest
{
    @InjectMockComponents
    private SolrLiveDataConfigurationResolver resolver;

    @MockComponent
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    @MockComponent
    @Named("solr")
    private Provider<LiveDataPropertyDescriptorStore> propertyStoreProvider;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    /**
     * Builds the input configuration and wires the mocked dependencies so that the property descriptors come from a
     * real {@link SolrLiveDataPropertyStore} (with unset names) and the default JSON config has an initialized meta.
     */
    private LiveDataConfiguration setUp(String translationPrefix) throws Exception
    {
        // The property descriptors are provided by the real property store, which leaves the column names unset.
        when(this.propertyStoreProvider.get()).thenReturn(new SolrLiveDataPropertyStore());

        LiveDataConfiguration defaultConfig = new LiveDataConfiguration();
        defaultConfig.initialize();
        when(this.stringLiveDataConfigResolver.resolve(anyString())).thenReturn(defaultConfig);

        LiveDataConfiguration input = new LiveDataConfiguration();
        input.initialize();
        Source source = new Source("solr");
        if (translationPrefix != null) {
            source.setParameter("translationPrefix", translationPrefix);
        }
        input.getQuery().setSource(source);
        return input;
    }

    private String getColumnName(LiveDataConfiguration config, String propertyId)
    {
        Optional<LiveDataPropertyDescriptor> descriptor = config.getMeta().getPropertyDescriptors().stream()
            .filter(it -> propertyId.equals(it.getId())).findFirst();
        return descriptor.map(LiveDataPropertyDescriptor::getName).orElse(null);
    }

    @Test
    void resolveLocalizesColumnHeadersFromTranslationPrefix() throws Exception
    {
        LiveDataConfiguration input = setUp("platform.index.");
        when(this.localizationManager.getTranslationPlain("platform.index.doc.title")).thenReturn("Title");
        when(this.localizationManager.getTranslationPlain("platform.index.doc.author")).thenReturn("Last Author");
        when(this.localizationManager.getTranslationPlain("platform.index.doc.date")).thenReturn("Date");

        LiveDataConfiguration config = this.resolver.resolve(input);

        assertEquals("Title", getColumnName(config, "doc.title"));
        assertEquals("Last Author", getColumnName(config, "doc.author"));
        assertEquals("Date", getColumnName(config, "doc.date"));
        // A property with no matching translation falls back to its id even when a prefix is set.
        assertEquals("doc.fullName", getColumnName(config, "doc.fullName"));
    }

    @Test
    void resolveFallsBackToPropertyIdWhenNoTranslationPrefix() throws Exception
    {
        LiveDataConfiguration input = setUp(null);

        LiveDataConfiguration config = this.resolver.resolve(input);

        // Without a translation prefix (and thus no translation), every column header falls back to its property id.
        assertEquals("doc.title", getColumnName(config, "doc.title"));
        assertEquals("doc.author", getColumnName(config, "doc.author"));
    }

    @Test
    void resolveTranslatesColumnDescriptionFromHintKey() throws Exception
    {
        LiveDataConfiguration input = setUp("platform.index.");
        when(this.localizationManager.getTranslationPlain("platform.index.doc.title")).thenReturn("Title");
        when(this.localizationManager.getTranslationPlain("platform.index.doc.title.hint")).thenReturn("The page title");

        LiveDataConfiguration config = this.resolver.resolve(input);

        Optional<LiveDataPropertyDescriptor> docTitle = config.getMeta().getPropertyDescriptors().stream()
            .filter(it -> "doc.title".equals(it.getId())).findFirst();
        assertEquals("The page title", docTitle.map(LiveDataPropertyDescriptor::getDescription).orElse(null));
    }

    @Test
    void resolvePassesSourceParametersToPropertyStore() throws Exception
    {
        // Sanity check that the source parameters (incl. the translation prefix) are forwarded to the property store,
        // so a future property store reacting to them would see them.
        SolrLiveDataPropertyStore propertyStore = new SolrLiveDataPropertyStore();
        when(this.propertyStoreProvider.get()).thenReturn(propertyStore);

        LiveDataConfiguration defaultConfig = new LiveDataConfiguration();
        defaultConfig.initialize();
        when(this.stringLiveDataConfigResolver.resolve(anyString())).thenReturn(defaultConfig);

        LiveDataConfiguration input = new LiveDataConfiguration();
        input.initialize();
        Source source = new Source("solr");
        source.setParameter("translationPrefix", "platform.index.");
        input.getQuery().setSource(source);

        this.resolver.resolve(input);

        Map<String, Object> propertyStoreParameters = propertyStore.getParameters();
        assertEquals("platform.index.", propertyStoreParameters.get("translationPrefix"));
    }

    @Test
    void resolveAddsDefaultDescriptorForUnknownRequestedProperty() throws Exception
    {
        LiveDataConfiguration input = setUp(null);
        // A requested property unknown to the source must still get a (default) descriptor so the widget does not
        // crash on a missing descriptor.
        input.getQuery().setProperties(List.of("doc.title", "doc.bogus"));

        LiveDataConfiguration config = this.resolver.resolve(input);

        Optional<LiveDataPropertyDescriptor> bogus = config.getMeta().getPropertyDescriptors().stream()
            .filter(it -> "doc.bogus".equals(it.getId())).findFirst();
        assertTrue(bogus.isPresent());
        assertEquals("String", bogus.get().getType());
    }
}
