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
package org.xwiki.test.docker.internal.junit5.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.test.docker.junit5.DockerTestException;

/**
 * Merges XWiki properties and handles special case such as the {@code xwikiCfgPlugins} property that needs to be
 * merged if it doesn't start with the {@code ^} character.
 *
 * @version $Id$
 * @since 11.3RC1
 */
public class PropertiesMerger
{
    private static final String COMMA = ",";

    private static final String NEWLINE = "\n";

    private static final String CARET = "^";

    private static final List<String> KNOWN_LIST_KEYS = Arrays.asList(
        "xwikiCfgPlugins",
        "xwikiDbHbmCommonExtraMappings");

    private static final List<String> KNOWN_PROPERTIES_KEYS = Arrays.asList(
        "xwikiPropertiesAdditionalProperties");

    /**
     * @param originalProperties the properties onto which the merge will happen
     * @param newProperties the properties to merge
     * @param override if true then override existing properties, if false then throw an exception if the property
     *        already exists and cannot be merged
     * @return the merged properties
     * @throws DockerTestException if one of the properties cannot be merged (when override is false)
     */
    public Properties merge(Properties originalProperties, Properties newProperties, boolean override)
        throws DockerTestException
    {
        Properties properties = new Properties();

        // Add default properties
        properties.putAll(originalProperties);

        // Add user-specified properties (with possible overrides for default properties)
        if (newProperties != null) {
            for (String key : newProperties.stringPropertyNames()) {
                String originalValue = properties.getProperty(key);
                String newValue = newProperties.getProperty(key);
                // If the property already exists and override is true then replace. Otherwise if the property is a list
                // type, try to merge it.
                if (properties.containsKey(key)) {
                    mergeExistingProperty(key, originalValue, newValue, properties, override);
                } else {
                    properties.setProperty(key, newValue);
                }
            }
        }

        return properties;
    }

    private void mergeExistingProperty(String key, String originalValue, String newValue, Properties properties,
        boolean override) throws DockerTestException
    {
        if (!originalValue.equals(newValue)) {
            // Not the same value for the same key.
            // If list type, then merge the lists
            if (isListProperty(key) || isPropertiesProperty(key)) {
                // Allow full override if key starts with "^" character.
                if (!newValue.startsWith(CARET)) {
                    String separator = isListProperty(key) ? COMMA : NEWLINE;
                    Set<String> merge = mergeList(originalValue, newValue, separator);
                    properties.setProperty(key, StringUtils.join(merge, separator));
                } else {
                    properties.setProperty(key, StringUtils.removeStart(newValue, CARET));
                }
            } else {
                if (override) {
                    properties.setProperty(key, newValue);
                } else {
                    throw new DockerTestException(
                        String.format("Cannot merge property [%s] = [%s] since it was already specified with "
                            + "value [%s]", key, newValue, originalValue));
                }
            }
        }

    }

    private boolean isListProperty(String key)
    {
        return KNOWN_LIST_KEYS.contains(key);
    }

    private boolean isPropertiesProperty(String key)
    {
        return KNOWN_PROPERTIES_KEYS.contains(key);
    }

    private Set<String> mergeList(String list1, String list2, String separator)
    {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(split(list1, separator));
        result.addAll(split(list2, separator));
        return result;
    }

    private List<String> split(String list, String separator)
    {
        List<String> result = new ArrayList<>();
        for (String value : StringUtils.split(list, separator)) {
            result.add(StringUtils.trim(value));
        }
        return result;
    }
}
