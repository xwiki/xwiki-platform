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
package org.xwiki.livedata.internal.livetable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataActionDescriptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.WithParameters;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLiveDataConfigurationResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultLiveDataConfigurationResolverTest
{
    @InjectMockComponents
    private DefaultLiveDataConfigurationResolver resolver;

    @MockComponent
    private ContextualLocalizationManager l10n;

    @MockComponent
    @Named("liveTable")
    private Provider<LiveDataPropertyDescriptorStore> propertyStoreProvider;

    @Mock(extraInterfaces = { LiveDataPropertyDescriptorStore.class })
    private WithParameters propertyStore;

    @MockComponent
    @Named("liveTable")
    private Provider<LiveDataConfiguration> defaultConfigProvider;

    private final LiveDataConfiguration defaultConfig = new LiveDataConfiguration();

    private final LiveDataConfiguration config = new LiveDataConfiguration();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void configure()
    {
        when(this.propertyStoreProvider.get()).thenReturn((LiveDataPropertyDescriptorStore) this.propertyStore);

        this.defaultConfig.initialize();
        when(this.defaultConfigProvider.get()).thenReturn(this.defaultConfig);

        this.config.initialize();
        this.objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
    }

    @Test
    void setDefaultSort() throws Exception
    {
        LiveDataPropertyDescriptor aliceDescriptor = new LiveDataPropertyDescriptor();
        aliceDescriptor.setId("_alice");
        aliceDescriptor.setSortable(true);
        this.config.getMeta().getPropertyDescriptors().add(aliceDescriptor);

        LiveDataPropertyDescriptor bobDescriptor = new LiveDataPropertyDescriptor();
        bobDescriptor.setId("bob");
        bobDescriptor.setSortable(false);
        this.config.getMeta().getPropertyDescriptors().add(bobDescriptor);

        LiveDataPropertyDescriptor carolDescriptor = new LiveDataPropertyDescriptor();
        carolDescriptor.setId("carol");
        carolDescriptor.setType("User");
        this.config.getMeta().getPropertyDescriptors().add(carolDescriptor);

        LiveDataPropertyDescriptor userDescriptor = new LiveDataPropertyDescriptor();
        userDescriptor.setId("User");
        userDescriptor.setSortable(true);
        this.config.getMeta().getPropertyTypes().add(userDescriptor);

        // Verify when no sort is set.
        this.config.getQuery().setSort(null);

        this.config.getQuery().setProperties(Arrays.asList("_alice", "bob", "carol"));
        LiveDataConfiguration actualConfig = this.resolver.resolve(this.config);

        // Because the live table skips properties that start with underscore..
        assertTrue(actualConfig.getQuery().getSort().isEmpty());

        this.config.getQuery().setProperties(Arrays.asList("_alice", "carol"));
        actualConfig = this.resolver.resolve(this.config);

        SortEntry sortEntry = actualConfig.getQuery().getSort().get(0);
        assertEquals("carol", sortEntry.getProperty());
        assertFalse(sortEntry.isDescending());

        // Verify when only the sort order is set.
        this.config.getQuery().setSort(Collections.singletonList(new SortEntry(null, true)));
        actualConfig = this.resolver.resolve(this.config);

        sortEntry = actualConfig.getQuery().getSort().get(0);
        assertEquals("carol", sortEntry.getProperty());
        assertTrue(sortEntry.isDescending());

        // Verify when no properties are specified.
        this.config.getQuery().setProperties(Collections.emptyList());
        this.config.getQuery().setSort(Collections.singletonList(new SortEntry(null)));
        actualConfig = this.resolver.resolve(this.config);
        assertTrue(actualConfig.getQuery().getSort().isEmpty());
    }

    @Test
    void resolvePropertyDescriptors() throws Exception
    {
        this.config.setId("test");
        this.config.getQuery().getSource().setParameter("translationPrefix", "test.liveData.");
        this.config.getQuery().setProperties(Arrays.asList("doc.title", "manager", "active"));

        LiveDataPropertyDescriptor docTitle = new LiveDataPropertyDescriptor();
        docTitle.setId("doc.title");
        LiveDataPropertyDescriptor count = new LiveDataPropertyDescriptor();
        count.setId("count");
        when(((LiveDataPropertyDescriptorStore) this.propertyStore).get()).thenReturn(Arrays.asList(docTitle, count));

        Map<String, Object> propertyStoreParams = new HashMap<>();
        when(this.propertyStore.getParameters()).thenReturn(propertyStoreParams);

        LiveDataPropertyDescriptor docName = new LiveDataPropertyDescriptor();
        docName.setId("doc.name");
        // This will get overwritten with the descriptors from the property store.
        this.defaultConfig.getMeta().getPropertyDescriptors().add(docName);

        LiveDataPropertyDescriptor manager = new LiveDataPropertyDescriptor();
        manager.setId("manager");
        manager.setName("*Manager*");
        count = new LiveDataPropertyDescriptor();
        count.setId("count");
        count.setDescription("Some count");
        this.config.getMeta().getPropertyDescriptors().add(manager);
        this.config.getMeta().getPropertyDescriptors().add(count);

        when(this.l10n.getTranslationPlain("test.liveData.doc.title")).thenReturn("Title");
        when(this.l10n.getTranslationPlain("test.liveData.doc.title.hint")).thenReturn("Page title");
        when(this.l10n.getTranslationPlain("test.liveData.count")).thenReturn("Count");
        when(this.l10n.getTranslationPlain("test.liveData.manager.hint")).thenReturn("The manager");
        when(this.l10n.getTranslationPlain("test.liveData.active")).thenReturn("Active");
        when(this.l10n.getTranslationPlain("test.liveData.active.hint")).thenReturn("Is it active?");

        LiveDataConfiguration actualConfig = this.resolver.resolve(this.config);

        StringBuilder expectedProps = new StringBuilder();
        expectedProps.append("[");
        expectedProps
            .append("{'id':'manager','name':'*Manager*','description':'The manager','type':'String','visible':true},");
        expectedProps.append("{'id':'count','name':'Count','description':'Some count'},");
        expectedProps.append("{'id':'doc.title','name':'Title','description':'Page title'},");
        expectedProps
            .append("{'id':'active','name':'Active','description':'Is it active?','type':'String','visible':true}");
        expectedProps.append("]");

        String expectedJSON = expectedProps.toString().replace('\'', '"');
        assertEquals(expectedJSON,
            this.objectMapper.writeValueAsString(actualConfig.getMeta().getPropertyDescriptors()));

        assertEquals("test.liveData.", propertyStoreParams.get("translationPrefix"));
    }

    /**
     * This test asserts that the translation is used for the name of a property, even if the property has a defined
     * pretty name.
     */
    @Test
    void propertyNameIsTranslationKey() throws Exception
    {
        this.config.getQuery().getProperties().add("releaseDate");
        this.defaultConfig.getQuery().getSource().setParameter("translationPrefix", "release.livetable.");

        LiveDataPropertyDescriptor propertyDescriptorReleaseDate = new LiveDataPropertyDescriptor();
        propertyDescriptorReleaseDate.setId("releaseDate");
        propertyDescriptorReleaseDate.setName("Release Date");
        when(((LiveDataPropertyDescriptorStore) this.propertyStore).get())
            .thenReturn(singletonList(propertyDescriptorReleaseDate));

        when(this.l10n.getTranslationPlain("release.livetable.releaseDate")).thenReturn("Released On");

        LiveDataConfiguration actualConfig = this.resolver.resolve(this.config);
        assertEquals("[{\"id\":\"releaseDate\",\"name\":\"Released On\"}]",
            this.objectMapper.writeValueAsString(actualConfig.getMeta().getPropertyDescriptors()));
    }

    /**
     * This test asserts that the default name is kept for the name of a property, even if the property has a defined
     * pretty name, or if a translation exists for the property.
     */
    @Test
    void propertyNameIsDefaultValue() throws Exception
    {
        this.config.getQuery().getProperties().add("releaseDate");
        this.config.getQuery().getSource().setParameter("translationPrefix", "release.livetable.");
        LiveDataPropertyDescriptor e = new LiveDataPropertyDescriptor();
        e.setId("releaseDate");
        e.setName("Date of release");
        this.config.getMeta().getPropertyDescriptors().add(e);

        LiveDataPropertyDescriptor propertyDescriptorReleaseDate = new LiveDataPropertyDescriptor();
        propertyDescriptorReleaseDate.setId("releaseDate");
        propertyDescriptorReleaseDate.setName("Release Date");
        when(((LiveDataPropertyDescriptorStore) this.propertyStore).get())
            .thenReturn(singletonList(propertyDescriptorReleaseDate));

        when(this.l10n.getTranslationPlain("release.livetable.releaseDate")).thenReturn("Released On");

        LiveDataConfiguration actualConfig = this.resolver.resolve(this.config);
        assertEquals("[{\"id\":\"releaseDate\",\"name\":\"Date of release\"}]",
            this.objectMapper.writeValueAsString(actualConfig.getMeta().getPropertyDescriptors()));
    }

    @Test
    void actionNameIsTranslationKey() throws Exception
    {
        this.defaultConfig.getQuery().getSource().setParameter("translationPrefix", "platform.wiki.browse.");

        this.config.getMeta().getActions().add(new LiveDataActionDescriptor("join"));

        when(this.l10n.getTranslationPlain("platform.wiki.browse._actions.join")).thenReturn("Join");

        LiveDataConfiguration actualConfig = this.resolver.resolve(this.config);
        assertEquals("[{\"id\":\"join\",\"name\":\"Join\"}]",
            this.objectMapper.writeValueAsString(actualConfig.getMeta().getActions()));
    }

    @Test
    void actionNameIsDefaultValue() throws Exception
    {
        this.defaultConfig.getQuery().getSource().setParameter("translationPrefix", "platform.wiki.browse.");

        LiveDataActionDescriptor join = new LiveDataActionDescriptor("join");
        join.setName("Join the wiki");
        this.config.getMeta().getActions().add(join);

        when(this.l10n.getTranslationPlain("platform.wiki.browse._actions.join")).thenReturn("Join");

        LiveDataConfiguration actualConfig = this.resolver.resolve(this.config);
        assertEquals("[{\"id\":\"join\",\"name\":\"Join the wiki\"}]",
            this.objectMapper.writeValueAsString(actualConfig.getMeta().getActions()));
    }
}
