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
package org.xwiki.rest.internal.resources.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.QueryFilter;

/**
 * Used to split the values of a list property that has multiple selection enabled but doesn't use relational storage
 * (the selected values are joined by a separator configured in the property definition). This filter was designed to
 * work with com.xpn.xwiki.internal.objects.classes.UsedValuesListQueryBuilder.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
public class SplitValueQueryFilter implements QueryFilter
{
    private final String separators;

    private final int limit;

    private final String filter;

    /**
     * Creates a new filter.
     * 
     * @param separators the separators characters that were used to join the values
     * @param limit the maximum number of results to return
     * @param filter the text used to filter the values
     */
    public SplitValueQueryFilter(String separators, int limit, String filter)
    {
        this.separators = separators;
        this.limit = limit;
        this.filter = filter;
    }

    @Override
    public List<Object> filterResults(@SuppressWarnings("rawtypes") List results)
    {
        // Spit the values, remove the ones that don't match the text filter, remove duplicates and merge the occurrence
        // count.
        Map<String, Long> filteredResults = new HashMap<>();
        for (Object result : results) {
            Object[] row = (Object[]) result;
            String value = (String) row[0];
            Long count = (Long) row[1];
            String[] values = StringUtils.split(value, this.separators);
            for (String actualValue : values) {
                if (actualValue.contains(this.filter)) {
                    Long previousCount = filteredResults.getOrDefault(actualValue, 0L);
                    filteredResults.put(actualValue, previousCount + count);
                }
            }
        }

        // Sort the remaining values by their occurrence count.
        List<Entry<String, Long>> actualResults = new ArrayList<>(filteredResults.entrySet());
        actualResults.sort(Collections.reverseOrder(Entry.comparingByValue()));

        // Limit the results.
        if (actualResults.size() > this.limit) {
            actualResults = actualResults.subList(0, this.limit);
        }

        return actualResults.stream().map(entry -> new Object[] {entry.getKey(), entry.getValue()})
            .collect(Collectors.toList());
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        return statement;
    }
}
