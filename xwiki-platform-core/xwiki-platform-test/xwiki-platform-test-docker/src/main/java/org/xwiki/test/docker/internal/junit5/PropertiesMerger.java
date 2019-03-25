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
package org.xwiki.test.docker.internal.junit5;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.xwiki.text.StringUtils;

/**
 * Merges XWiki properties and handles special case such as the {@code xwikiCfgPlugins} property that needs to be
 * merged if it doesn't start with the {@code ^} character.
 *
 * @version $Id$
 * @since 11.3RC1
 */
public class PropertiesMerger
{
    private static final String SEPARATOR = ",";

    private static final String CARET = "^";

    /**
     * @param original the properties onto which the merge will happen
     * @param override the properties to merge
     * @return the merged properties
     */
    public Properties merge(Properties original, Properties override)
    {
        Properties properties = new Properties();

        // Add default properties
        properties.putAll(original);

        // Add user-specified properties (with possible overrides for default properties)
        properties.putAll(override);

        // Handle properties that are lists and for which we'd like to handle a merge rather than an overwrite
        for (String key : override.stringPropertyNames()) {
            if ("xwikiCfgPlugins".equals(key)) {
                // Allow full override if key starts with "^" character.
                String overrideValue = override.getProperty(key);
                String originalValue = original.getProperty(key);
                if (!overrideValue.startsWith(CARET) && original != null) {
                    Set<String> merge = mergeCommaSeparatedList(originalValue, overrideValue);
                    properties.setProperty(key, StringUtils.join(merge, SEPARATOR));
                } else {
                    properties.setProperty(key, StringUtils.removeStart(overrideValue, CARET));
                }
            }
        }

        return properties;
    }

    private Set<String> mergeCommaSeparatedList(String list1, String list2)
    {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(split(list1));
        result.addAll(split(list2));
        return result;
    }

    private List<String> split(String list)
    {
        List<String> result = new ArrayList<>();
        for (String value : StringUtils.split(list, SEPARATOR)) {
            result.add(StringUtils.trim(value));
        }
        return result;
    }
}
