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
package org.xwiki.model.validation.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ComponentTest
public class ReplaceCharacterEntityNameValidationConfigurationTest
{
    @InjectMockComponents
    private ReplaceCharacterEntityNameValidationConfiguration replaceCharacterEntityNameValidationConfiguration;

    @MockComponent
    @Named("entitynamevalidation")
    private ConfigurationSource configurationSource;

    @Test
    public void getCharacterReplacementMap()
    {
        assertEquals(Collections.emptyMap(),
            this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_FORBIDDENCHARACTERS),
                eq(List.class)))
            .thenReturn(Collections.singletonList("/"));
        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_REPLACEMENTCHARACTERS),
                eq(List.class)))
            .thenReturn(Collections.emptyList());
        assertEquals(Collections.singletonMap("/", ""),
            this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_FORBIDDENCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("/",".","foo"));
        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_REPLACEMENTCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("-","-","bar"));
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("/", "-");
        expectedMap.put(".", "-");
        expectedMap.put("foo", "bar");
        assertEquals(expectedMap, this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_FORBIDDENCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("/","\\","","foo"));
        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_REPLACEMENTCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("-","-"));
        expectedMap = new HashMap<>();
        expectedMap.put("/", "-");
        expectedMap.put("\\", "-");
        expectedMap.put("foo", "");
        assertEquals(expectedMap, this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_FORBIDDENCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("/","\\","","/"));
        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_REPLACEMENTCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("-","-"));
        expectedMap = new HashMap<>();
        expectedMap.put("/", "");
        expectedMap.put("\\", "-");
        assertEquals(expectedMap, this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_FORBIDDENCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("/","\\","","/"));
        when(configurationSource
            .getProperty(eq(ReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_REPLACEMENTCHARACTERS),
                eq(List.class)))
            .thenReturn(Arrays.asList("-","-"));
        expectedMap = new HashMap<>();
        expectedMap.put("/", "");
        expectedMap.put("\\", "-");
        assertEquals(expectedMap, this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());
    }
}
