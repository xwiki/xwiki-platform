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

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.model.validation.internal.DefaultReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_FORBIDDENCHARACTERS;
import static org.xwiki.model.validation.internal.DefaultReplaceCharacterEntityNameValidationConfiguration.PROPERTY_KEY_REPLACEMENTCHARACTERS;

@ComponentTest
class DefaultReplaceCharacterEntityNameValidationConfigurationTest
{
    @InjectMockComponents
    private DefaultReplaceCharacterEntityNameValidationConfiguration replaceCharacterEntityNameValidationConfiguration;

    @MockComponent
    @Named("entitynamevalidation")
    private ConfigurationSource configurationSource;

    @Test
    void getCharacterReplacementMap()
    {
        assertEquals(Map.of(), this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(this.configurationSource.getProperty(PROPERTY_KEY_FORBIDDENCHARACTERS, List.class))
            .thenReturn(List.of("/"));
        when(this.configurationSource.getProperty(PROPERTY_KEY_REPLACEMENTCHARACTERS, List.class))
            .thenReturn(List.of());
        assertEquals(Map.of("/", ""),
            this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(this.configurationSource.getProperty(PROPERTY_KEY_FORBIDDENCHARACTERS, List.class))
            .thenReturn(List.of("/", ".", "foo"));
        when(this.configurationSource.getProperty(PROPERTY_KEY_REPLACEMENTCHARACTERS, List.class))
            .thenReturn(List.of("-", "-", "bar"));

        assertEquals(Map.of(
            "/", "-",
            ".", "-",
            "foo", "bar"
        ), this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(this.configurationSource.getProperty(PROPERTY_KEY_FORBIDDENCHARACTERS, List.class))
            .thenReturn(List.of("/", "\\", "", "foo"));
        when(this.configurationSource.getProperty(PROPERTY_KEY_REPLACEMENTCHARACTERS, List.class))
            .thenReturn(List.of("-", "-"));
        assertEquals(Map.of(
            "/", "-",
            "\\", "-",
            "foo", ""
        ), this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(this.configurationSource.getProperty(PROPERTY_KEY_FORBIDDENCHARACTERS, List.class))
            .thenReturn(List.of("/", "\\", "", "/"));
        when(this.configurationSource.getProperty(PROPERTY_KEY_REPLACEMENTCHARACTERS, List.class))
            .thenReturn(List.of("-", "-"));
        assertEquals(Map.of(
            "/", "",
            "\\", "-"), this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());

        when(this.configurationSource.getProperty(PROPERTY_KEY_FORBIDDENCHARACTERS, List.class))
            .thenReturn(List.of("/", "\\", "", "/"));
        when(this.configurationSource.getProperty(PROPERTY_KEY_REPLACEMENTCHARACTERS, List.class))
            .thenReturn(List.of("-", "-"));
        assertEquals(Map.of(
            "/", "",
            "\\", "-"
        ), this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap());
    }
}
