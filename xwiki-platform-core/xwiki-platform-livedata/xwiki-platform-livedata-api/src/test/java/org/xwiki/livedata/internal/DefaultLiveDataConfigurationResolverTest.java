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
package org.xwiki.livedata.internal;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLiveDataConfigurationResolver}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
@ComponentList(StringLiveDataConfigurationResolver.class)
class DefaultLiveDataConfigurationResolverTest
{
    private static final ConfigurationResolverTestDataProvider DATA_PROVIDER =
        new ConfigurationResolverTestDataProvider();

    @InjectMockComponents
    private DefaultLiveDataConfigurationResolver resolver;

    @MockComponent
    private ContextualLocalizationManager l10n;

    @MockComponent
    private IconManager iconManager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JSONMerge jsonMerge = new JSONMerge();

    @BeforeEach
    void before(MockitoComponentManager componentManager) throws Exception
    {
        when(this.componentManagerProvider.get()).thenReturn(componentManager);

        when(this.iconManager.getMetaData("table"))
            .thenReturn(Collections.singletonMap(IconManager.META_DATA_CSS_CLASS, "fa fa-table"));
        when(this.iconManager.getMetaData("eye"))
            .thenReturn(Collections.singletonMap(IconManager.META_DATA_CSS_CLASS, "fa fa-eye"));

        when(this.l10n.getTranslationPlain("liveData.layout.table")).thenReturn("Table");
        when(this.l10n.getTranslationPlain("liveData.operator.equals")).thenReturn("Equals");
        when(this.l10n.getTranslationPlain("liveData.action.edit")).thenReturn("Edit");
        when(this.l10n.getTranslationPlain("liveData.action.edit.hint")).thenReturn("Edit entry");

        LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
        entryDescriptor.setIdProperty("entryId");

        Map<String, Object> fileIconMetaData = new HashMap<>();
        fileIconMetaData.put(IconManager.META_DATA_ICON_SET_TYPE, "font");
        fileIconMetaData.put(IconManager.META_DATA_ICON_SET_NAME, "Font Awesome");
        fileIconMetaData.put(IconManager.META_DATA_CSS_CLASS, "fa fa-file-o");

        LiveDataPropertyDescriptor propertyDescriptor = new LiveDataPropertyDescriptor();
        propertyDescriptor.setId("title");
        propertyDescriptor.setDescription("The title of the book");
        propertyDescriptor.setName("Title");
        propertyDescriptor.setSortable(true);
        propertyDescriptor.setType("String");
        propertyDescriptor.setIcon(fileIconMetaData);
        propertyDescriptor.setDisplayer(new DisplayerDescriptor("link"));
        propertyDescriptor.getDisplayer().setParameter("propertyHref", "doc_url");
        propertyDescriptor.setFilter(new FilterDescriptor("text"));
        propertyDescriptor.getFilter().setDefaultOperator("contains");

        LiveDataPropertyDescriptor propertyType = new LiveDataPropertyDescriptor();
        propertyType.setId("String");
        propertyType.setDescription("A string property");
        propertyType.setName("String");
        propertyType.setSortable(true);
        propertyType.setIcon(fileIconMetaData);
        propertyType.setDisplayer(new DisplayerDescriptor("link"));
        propertyType.getDisplayer().setParameter("propertyHref", "doc_url");
        propertyType.setFilter(new FilterDescriptor("text"));
        propertyType.getFilter().setDefaultOperator("contains");

        LiveDataMeta meta = new LiveDataMeta();
        meta.setEntryDescriptor(entryDescriptor);
        meta.setPropertyDescriptors(Arrays.asList(propertyDescriptor));
        meta.setPropertyTypes(Arrays.asList(propertyType));

        LiveDataConfiguration sourceDefaultConfig = new LiveDataConfiguration();
        sourceDefaultConfig.setId("defaultConfigResolverTest");
        sourceDefaultConfig.setMeta(meta);

        Type role =
            new DefaultParameterizedType(null, LiveDataConfigurationResolver.class, LiveDataConfiguration.class);
        LiveDataConfigurationResolver<LiveDataConfiguration> sourceDefaultConfigResolver =
            componentManager.registerMockComponent(role, "test");
        when(sourceDefaultConfigResolver.resolve(any()))
            .thenAnswer(invocation -> this.jsonMerge.merge(sourceDefaultConfig, invocation.getArgument(0)));
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void resolve(String message, String input, String output) throws Exception
    {
        LiveDataConfiguration liveDataConfig = this.objectMapper.readValue(input, LiveDataConfiguration.class);

        LiveDataConfiguration expected = this.objectMapper.readValue(output, LiveDataConfiguration.class);
        LiveDataConfiguration actual = this.resolver.resolve(liveDataConfig);
        assertEquals(expected, actual, message);
    }

    @Test
    void withInitialize() throws Exception
    {
        // Test with a default minimally initialized config. This is useful to simulate a custom source with its own way
        // to initialize its config.
        LiveDataConfiguration config = new LiveDataConfiguration();
        config.initialize();
        String expected = FileUtils.readFileToString(new File("src/test/resources/withInitialize.json"),
            Charset.defaultCharset());
        assertEquals(this.objectMapper.readValue(expected, LiveDataConfiguration.class),
            this.resolver.resolve(this.resolver.resolve(config)));
    }

    /**
     * Test to avoid regression on XWIKI-23523.
     * We need to ensure that we can resolve a configuration with meta=null.
     */
    @Test
    void withMetaNull() throws Exception
    {
        LiveDataConfiguration config = new LiveDataConfiguration();
        config.initialize();
        config.setMeta(null);
        String expected = FileUtils.readFileToString(new File("src/test/resources/withInitialize.json"),
            Charset.defaultCharset());
        assertEquals(this.objectMapper.readValue(expected, LiveDataConfiguration.class),
            this.resolver.resolve(this.resolver.resolve(config)));
    }

    private static Stream<String[]> getTestData() throws Exception
    {
        return DATA_PROVIDER.getTestData(new File("src/test/resources/DefaultLiveDataConfigurationResolver.test"));
    }
}
