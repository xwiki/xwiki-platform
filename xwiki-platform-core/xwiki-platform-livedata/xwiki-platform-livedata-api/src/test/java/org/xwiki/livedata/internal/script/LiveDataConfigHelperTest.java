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
package org.xwiki.livedata.internal.script;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveDataConfigHelper}.
 * 
 * @version $Id$
 */
@ComponentTest
class LiveDataConfigHelperTest
{
    @InjectMockComponents
    private LiveDataConfigHelper configHelper;

    @MockComponent
    private LiveDataSourceManager sourceManager;

    @MockComponent
    private IconManager iconManager;

    @Mock
    private LiveDataSource liveDataSource;

    @BeforeEach
    void configure() throws Exception
    {
        Map<String, Object> fileIconMetaData = new HashMap<>();
        fileIconMetaData.put(IconManager.META_DATA_ICON_SET_TYPE, "font");
        fileIconMetaData.put(IconManager.META_DATA_ICON_SET_NAME, "Font Awesome");
        fileIconMetaData.put(IconManager.META_DATA_CSS_CLASS, "fa fa-file-o");

        when(this.iconManager.getMetaData("file")).thenReturn(fileIconMetaData);

        Source source = new Source();
        source.setId("test");
        when(this.sourceManager.get(source)).thenReturn(Optional.of(this.liveDataSource));

        LiveDataPropertyDescriptorStore propertyStore = mock(LiveDataPropertyDescriptorStore.class, "properties");
        when(this.liveDataSource.getProperties()).thenReturn(propertyStore);

        LiveDataPropertyDescriptorStore propertyTypeStore =
            mock(LiveDataPropertyDescriptorStore.class, "propertyTypes");
        when(this.liveDataSource.getPropertyTypes()).thenReturn(propertyTypeStore);

        LiveDataPropertyDescriptor propertyDescriptor = new LiveDataPropertyDescriptor();
        propertyDescriptor.setId("title");
        propertyDescriptor.setDescription("The title of the book");
        propertyDescriptor.setName("Title");
        propertyDescriptor.setSortable(true);
        propertyDescriptor.setType("String");
        propertyDescriptor.getIcon().putAll(fileIconMetaData);
        propertyDescriptor.getDisplayer().setId("link");
        propertyDescriptor.getDisplayer().put("propertyHref", "doc_url");
        propertyDescriptor.getFilter().setId("text");
        propertyDescriptor.getFilter().setDefaultOperator("contains");

        when(propertyStore.get()).thenReturn(Arrays.asList(propertyDescriptor));

        LiveDataPropertyDescriptor propertyType = new LiveDataPropertyDescriptor();
        propertyType.setId("String");
        propertyType.setDescription("A string property");
        propertyType.setName("String");
        propertyType.setSortable(true);
        propertyType.getIcon().putAll(fileIconMetaData);
        propertyType.getDisplayer().setId("link");
        propertyType.getDisplayer().put("propertyHref", "doc_url");
        propertyType.getFilter().setId("text");
        propertyType.getFilter().setDefaultOperator("contains");

        when(propertyTypeStore.get()).thenReturn(Arrays.asList(propertyType));
    }

    @ParameterizedTest
    @MethodSource("getEffectiveConfigTestData")
    void effectiveConfig(String message, String input, String output) throws Exception
    {
        assertEquals(output, this.configHelper.effectiveConfig(input), message);
    }

    private static Stream<String[]> getEffectiveConfigTestData() throws Exception
    {
        return getTestData(new File("src/test/resources/effectiveConfig.txt"));
    }

    private static Stream<String[]> getTestData(File file) throws Exception
    {
        List<String[]> testData = new ArrayList<>();
        ListIterator<String> linesIterator = IOUtils.readLines(new FileReader(file)).listIterator();
        while (linesIterator.hasNext()) {
            String message = readTestMessage(linesIterator);
            String input = readTestInput(linesIterator);
            String output = readTestOutput(linesIterator);
            testData.add(new String[] {message, input, output});
        }
        return testData.stream();
    }

    private static String readTestMessage(ListIterator<String> linesIterator)
    {
        StringBuilder message = new StringBuilder();
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            if (line.startsWith("##")) {
                message.append(line.substring(2).trim());
            } else {
                linesIterator.previous();
                break;
            }
        }
        return message.toString();
    }

    private static String readTestInput(ListIterator<String> linesIterator)
    {
        StringBuilder input = new StringBuilder();
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            if (!line.equals("---")) {
                input.append(line.trim());
            } else {
                break;
            }
        }
        return input.toString();
    }

    private static String readTestOutput(ListIterator<String> linesIterator)
    {
        StringBuilder output = new StringBuilder();
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            if (!line.startsWith("##")) {
                output.append(line.trim());
            } else {
                linesIterator.previous();
                break;
            }
        }
        return output.toString();
    }
}
