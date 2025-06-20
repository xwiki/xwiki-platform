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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.icon.IconManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StringLiveDataConfigurationResolver}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class StringLiveDataConfigurationResolverTest
{
    private static final ConfigurationResolverTestDataProvider DATA_PROVIDER =
        new ConfigurationResolverTestDataProvider();

    @InjectMockComponents
    private StringLiveDataConfigurationResolver resolver;

    @MockComponent
    private IconManager iconManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before() throws Exception
    {
        this.objectMapper.setSerializationInclusion(Include.NON_NULL);

        Map<String, Object> fileIconMetaData = new HashMap<>();
        fileIconMetaData.put(IconManager.META_DATA_ICON_SET_TYPE, "font");
        fileIconMetaData.put(IconManager.META_DATA_ICON_SET_NAME, "Font Awesome");
        fileIconMetaData.put(IconManager.META_DATA_CSS_CLASS, "fa fa-file-o");

        Map<String, Object> tableIconMetaData = new HashMap<>();
        tableIconMetaData.put(IconManager.META_DATA_ICON_SET_TYPE, "image");
        tableIconMetaData.put(IconManager.META_DATA_ICON_SET_NAME, "Silk");
        tableIconMetaData.put(IconManager.META_DATA_URL, "/path/to/table.png");

        Map<String, Object> crossIconMetadata = Map.of(
            IconManager.META_DATA_ICON_SET_TYPE, "font",
            IconManager.META_DATA_ICON_SET_NAME, "Font Awesome",
            IconManager.META_DATA_CSS_CLASS, "fa fa-times"
        );
        
        Map<String, Object> testIconMetadata = Map.of(
            IconManager.META_DATA_ICON_SET_TYPE, "test",
            IconManager.META_DATA_ICON_SET_NAME, "Test"
        );

        when(this.iconManager.getMetaData("file")).thenReturn(fileIconMetaData);
        when(this.iconManager.getMetaData("table")).thenReturn(tableIconMetaData);
        when(this.iconManager.getMetaData("cross")).thenReturn(crossIconMetadata);
        when(this.iconManager.getMetaData("test")).thenReturn(testIconMetadata);
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void resolve(String message, String input, String output) throws Exception
    {
        JsonNode expect = this.objectMapper.readValue(output, JsonNode.class);
        JsonNode actual =
            this.objectMapper.readValue(this.objectMapper.writeValueAsString(this.resolver.resolve(input)),
                JsonNode.class);
        assertEquals(expect, actual, message);
    }

    private static Stream<String[]> getTestData() throws Exception
    {
        return DATA_PROVIDER.getTestData(new File("src/test/resources/StringLiveDataConfigurationResolver.test"));
    }
}
