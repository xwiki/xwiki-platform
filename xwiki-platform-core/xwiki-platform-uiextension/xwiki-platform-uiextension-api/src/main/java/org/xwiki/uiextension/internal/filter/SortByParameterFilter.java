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
package org.xwiki.uiextension.internal.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;

/**
 * Sort a list of {@link UIExtension} by the value of one of their parameters. The values of the chosen parameter must
 * be a string.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Singleton
@Named("sortByParameter")
public class SortByParameterFilter implements UIExtensionFilter
{
    /**
     * Comparator comparing the values of the parameter with the key set at the {@link SortByParameterFilter} level.
     * Parameter values can be String or Integers, if both types are mixed the Integer values will be sorted first, in
     * the correct numeric order.
     */
    public class UIExtensionParameterComparator implements Comparator<UIExtension>
    {
        /**
         * The key of the parameter to compare.
         */
        private final String parameterKey;

        /**
         * Default constructor.
         *
         * @param parameterKey The key of the parameter to use for the comparison
         */
        public UIExtensionParameterComparator(String parameterKey)
        {
            this.parameterKey = parameterKey;
        }

        @Override
        public int compare(UIExtension source, UIExtension target)
        {
            int result = 0;

            String sourceValue = source.getParameters().get(parameterKey);
            String targetValue = target.getParameters().get(parameterKey);
            if (sourceValue == null) {
                // If the source extensions doesn't have the parameter we want it to be put at the end of the collection
                result = Integer.MAX_VALUE;
            } else if (targetValue == null) {
                // If the target extensions doesn't have the parameter we want it to be put at the end of the collection
                result = Integer.MIN_VALUE;
            } else {
                try {
                    // The parameter values might be integers.
                    int sourceInt = Integer.parseInt(sourceValue);
                    int targetInt = Integer.parseInt(targetValue);
                    result = sourceInt - targetInt;
                } catch (NumberFormatException e) {
                    // They're not both integers so we compare the 2 string values instead.
                    result = sourceValue.compareToIgnoreCase(targetValue);
                }
            }

            return result;
        }
    }

    /**
     *
     * @param extensions The list of {@link UIExtension}s to filter
     * @param parameterKey The first argument will be used as the key to use to select the parameter used for ordering.
     *        Additional arguments are ignored.
     * @return The filtered list
     */
    @Override
    public List<UIExtension> filter(List<UIExtension> extensions, String... parameterKey)
    {
        List<UIExtension> results = new ArrayList<>();
        results.addAll(extensions);

        if (parameterKey.length > 0 && !StringUtils.isBlank(parameterKey[0])) {
            Collections.sort(results, new UIExtensionParameterComparator(parameterKey[0]));
        }

        return results;
    }
}
