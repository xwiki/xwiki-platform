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
package org.xwiki.query.solr.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.WrappingQuery;

/**
 * Restricts a Solr query whose statement or parameters come from untrusted input, e.g., from the request, to the
 * query syntax and the parameters that match documents only based on their own field values. Add it with
 * {@code query.addFilter("untrustedParameters/solr")}.
 * <p>
 * The filter is applied when the query is executed, so it covers all the parameters bound to the query until then,
 * whether they were bound before or after adding the filter. It's the only query filter that the Solr query executor
 * applies.
 * <p>
 * The query statement is not checked, but it is always parsed with the {@code xdismax} query parser, which treats
 * local parameters and magic fields as plain text. Of the named parameters, only the following are kept:
 * <ul>
 * <li>the filter queries ({@code fq}), as long as they are all safe according to
 * {@link SafeSolrQueryValidator#isSafeFilterQuery(String)}. Otherwise, all of them are replaced by a filter query that
 * matches nothing, so that the query returns no results rather than more results than requested.</li>
 * <li>the boost queries ({@code bq}), keeping only those that are safe according to
 * {@link SafeSolrQueryValidator#isSafeFilterQuery(String)}; as they only affect the score, the unsafe ones are
 * dropped.</li>
 * <li>the field list ({@code fl}) and the sort order ({@code sort}), keeping only the items that are safe according
 * to {@link SafeSolrQueryValidator}; the unsafe items are dropped.</li>
 * <li>the parameters that only control how the query statement is matched and scored ({@code qf}, {@code pf},
 * {@code pf2}, {@code pf3}, {@code ps}, {@code ps2}, {@code ps3}, {@code qs}, {@code mm}, {@code q.op},
 * {@code tie}), without checking their values.</li>
 * <li>the time allowed for the search ({@code timeAllowed}), which can only reduce the work done by Solr.</li>
 * </ul>
 * All the other parameters are removed, including {@code rows}, {@code start}, {@code qt} and {@code defType}. The
 * limit and the offset set on the query object ({@link Query#setLimit(int)}, {@link Query#setOffset(int)}) are not
 * affected. This filter doesn't filter the results: checking the view right of the documents of the response is done by
 * the query executor, with or without this filter.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.9
 * @since 17.10.16
 * @since 16.10.20
 */
@Component
@Singleton
@Named(SolrUntrustedParametersQueryFilter.HINT)
public class SolrUntrustedParametersQueryFilter implements QueryFilter
{
    /**
     * The hint of this component.
     */
    public static final String HINT = "untrustedParameters/solr";

    private static final String FILTER_QUERY = "fq";

    private static final String FIELD_LIST = "fl";

    private static final String SORT = "sort";

    private static final String BOOST_QUERY = "bq";

    private static final String TIME_ALLOWED = "timeAllowed";

    private static final String QUERY_PARSER = "defType";

    /**
     * The query parser configured by default for the search core. Unlike the default Solr query parser, it doesn't
     * parse local parameters and, as the allowed user fields ({@code uf}) are not specified, magic fields.
     */
    private static final String DISMAX_QUERY_PARSER = "xdismax";

    private static final Set<String> SCORING_PARAMETERS =
        Set.of("qf", "pf", "pf2", "pf3", "ps", "ps2", "ps3", "qs", "mm", "q.op", "tie");

    /**
     * A filter query that doesn't match any document.
     */
    private static final String NO_RESULTS = "-*:*";

    private static final Pattern FIELD_LIST_SEPARATOR = Pattern.compile("[,\\s]+");

    private static final Pattern SORT_SEPARATOR = Pattern.compile("\\s*,\\s*");

    /**
     * A sort order item: a field and the sort direction, which is case-insensitive.
     */
    private static final Pattern SORT_ITEM = Pattern.compile("(\\S+)\\s+(asc|desc)", Pattern.CASE_INSENSITIVE);

    @Inject
    private SafeSolrQueryValidator validator;

    @Override
    public String filterStatement(String statement, String language)
    {
        return statement;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List filterResults(List results)
    {
        return results;
    }

    @Override
    public Query filterQuery(Query query)
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(QUERY_PARSER, DISMAX_QUERY_PARSER);
        boolean supported = true;

        for (Map.Entry<String, Object> parameter : query.getNamedParameters().entrySet()) {
            String name = parameter.getKey();
            if (FILTER_QUERY.equals(name)) {
                List<String> filterQueries = toStringList(parameter.getValue());
                supported &= filterQueries.stream().allMatch(this.validator::isSafeFilterQuery);
                parameters.put(name, filterQueries);
            } else if (BOOST_QUERY.equals(name)) {
                List<String> boostQueries =
                    toStringList(parameter.getValue()).stream().filter(this.validator::isSafeFilterQuery).toList();
                if (!boostQueries.isEmpty()) {
                    parameters.put(name, boostQueries);
                }
            } else if (FIELD_LIST.equals(name)) {
                putItems(parameters, name, parameter.getValue(), FIELD_LIST_SEPARATOR,
                    this.validator::isSafeFieldListItem);
            } else if (SORT.equals(name)) {
                putItems(parameters, name, parameter.getValue(), SORT_SEPARATOR, this::isSafeSortItem);
            } else if (SCORING_PARAMETERS.contains(name) || TIME_ALLOWED.equals(name)) {
                parameters.put(name, parameter.getValue());
            }
        }

        if (!supported) {
            parameters.put(FILTER_QUERY, List.of(NO_RESULTS));
        }

        return new SupportedParametersQuery(query, parameters);
    }

    private boolean isSafeSortItem(String item)
    {
        Matcher matcher = SORT_ITEM.matcher(item);
        return matcher.matches() && this.validator.isSafeSortField(matcher.group(1));
    }

    private void putItems(Map<String, Object> parameters, String name, Object value, Pattern separator,
        Predicate<String> isSafeItem)
    {
        List<String> items = new ArrayList<>();
        for (String itemList : toStringList(value)) {
            for (String itemValue : separator.split(itemList.trim())) {
                if (isSafeItem.test(itemValue)) {
                    items.add(itemValue);
                }
            }
        }

        if (!items.isEmpty()) {
            parameters.put(name, String.join(",", items));
        }
    }

    private List<String> toStringList(Object value)
    {
        List<String> values = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> values.add(String.valueOf(item)));
        } else if (value != null && value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) {
                values.add(String.valueOf(Array.get(value, i)));
            }
        } else if (value != null) {
            values.add(String.valueOf(value));
        }
        return values;
    }

    /**
     * The query with the supported parameters. The parameters bound afterwards (e.g., by other query filters) are
     * kept.
     */
    private static final class SupportedParametersQuery extends WrappingQuery
    {
        private final Map<String, Object> parameters;

        SupportedParametersQuery(Query wrappedQuery, Map<String, Object> parameters)
        {
            super(wrappedQuery);
            this.parameters = parameters;
        }

        @Override
        public Query bindValue(String variable, Object val)
        {
            this.parameters.put(variable, val);
            return this;
        }

        @Override
        public Map<String, Object> getNamedParameters()
        {
            return this.parameters;
        }
    }
}
