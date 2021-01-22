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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private LiveDataPropertyDescriptorStore propertyStore;

    @MockComponent
    @Named("liveTable")
    private Provider<LiveDataConfiguration> defaultConfigProvider;

    private LiveDataConfiguration defaultConfig = new LiveDataConfiguration();

    private LiveDataConfiguration config = new LiveDataConfiguration();

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void configure()
    {
        this.defaultConfig.initialize();
        when(this.defaultConfigProvider.get()).thenReturn(this.defaultConfig);

        this.config.initialize();
        this.objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
    }

    @Test
    void setDefaultSort() throws Exception
    {
        this.config.getQuery().setProperties(Arrays.asList("_one", "two", "three"));

        // Verify when no sort is set.
        this.config.getQuery().setSort(null);
        LiveDataConfiguration actualConfig = this.resolver.resolve(this.config);

        SortEntry sortEntry = actualConfig.getQuery().getSort().get(0);
        assertEquals("two", sortEntry.getProperty());
        assertFalse(sortEntry.isDescending());

        // Verify when only the sort order is set.
        this.config.getQuery().setSort(Collections.singletonList(new SortEntry(null, true)));
        actualConfig = this.resolver.resolve(this.config);

        sortEntry = actualConfig.getQuery().getSort().get(0);
        assertEquals("two", sortEntry.getProperty());
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
        when(this.propertyStore.get()).thenReturn(Arrays.asList(docTitle, count));

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
    }
}
