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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLiveDataConfigurationResolver}.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@ComponentTest
@ComponentList(StringLiveDataConfigurationResolver.class)
class DefaultLiveDataConfigurationResolverTest extends AbstractLiveDataConfigurationResolverTest
{
    @InjectMockComponents
    private DefaultLiveDataConfigurationResolver resolver;

    @MockComponent
    private LiveDataSourceManager sourceManager;

    @MockComponent
    private ContextualLocalizationManager l10n;

    @MockComponent
    private IconManager iconManager;

    @Mock
    private LiveDataSource liveDataSource;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before() throws Exception
    {
        Source source = new Source();
        source.setId("test");
        when(this.sourceManager.get(source)).thenReturn(Optional.of(this.liveDataSource));

        LiveDataEntryStore entryStore = mock(LiveDataEntryStore.class);
        when(entryStore.getIdProperty()).thenReturn("entryId");
        when(this.liveDataSource.getEntries()).thenReturn(entryStore);

        LiveDataPropertyDescriptorStore propertyStore = mock(LiveDataPropertyDescriptorStore.class, "properties");
        when(this.liveDataSource.getProperties()).thenReturn(propertyStore);

        LiveDataPropertyDescriptorStore propertyTypeStore =
            mock(LiveDataPropertyDescriptorStore.class, "propertyTypes");
        when(this.liveDataSource.getPropertyTypes()).thenReturn(propertyTypeStore);

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

        when(propertyStore.get()).thenReturn(Arrays.asList(propertyDescriptor));

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

        when(propertyTypeStore.get()).thenReturn(Arrays.asList(propertyType));
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void resolve(String message, String input, String output) throws Exception
    {
        LiveDataConfiguration liveDataConfig = this.objectMapper.readValue(input, LiveDataConfiguration.class);

        assertEquals(output, this.objectMapper.writeValueAsString(this.resolver.resolve(liveDataConfig)), message);
    }

    private static Stream<String[]> getTestData() throws Exception
    {
        return getTestData(new File("src/test/resources/DefaultLiveDataConfigurationResolver.test"));
    }
}
