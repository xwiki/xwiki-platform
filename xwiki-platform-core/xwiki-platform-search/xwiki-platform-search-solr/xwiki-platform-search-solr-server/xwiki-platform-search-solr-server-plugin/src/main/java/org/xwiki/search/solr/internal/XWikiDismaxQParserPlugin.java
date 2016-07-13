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
package org.xwiki.search.solr.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.util.SolrPluginUtils;

/**
 * Extends {@link ExtendedDismaxQParserPlugin} in order to add dynamic aliases for multilingual fields which are
 * expanded in the search query. This way, a user can write a query on the {@code title} field and all the
 * {@code title_<language>} variations of the field will be used in the query. The list of languages for which a field
 * is expanded is taken from the {@code xwiki.supportedLocales} query parameter. If this parameter is not defined, the
 * ROOT locale is used instead. The list of multilingual fields is determined based on the
 * {@code xwiki.multilingualFields}.
 * <p>
 * The current approach is to extract the field names from the search query and to add the alias parameters before the
 * query is parsed. We tried the following solutions too, but they failed:
 * <ul>
 * <li>We tried to extended {@code ExtendedDismaxQParser} and override {@code getFieldName()} in order to detect the
 * fields that appear in the search query and add the alias parameters for them but unfortunately
 * {@code ExtendedDismaxQParser} calls {@code ExtendedSolrQueryParser#addAliasesFromRequest()} before splitting the
 * search query in clauses.</li>
 * <li>We tried to expand the {@code Query} object returned by {@code ExtendedSolrQueryParser#parse()} but it doesn't
 * support iteration and it has lots of subclasses so we had to check the type of query and perform special iteration
 * and special changes for each of these subclasses.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 5.3RC1
 */
public class XWikiDismaxQParserPlugin extends ExtendedDismaxQParserPlugin
{
    /**
     * The pattern used to split list configuration parameters.
     */
    private static final Pattern LIST_SEPARATOR = Pattern.compile("\\s*,\\s*");

    /**
     * The pattern used to extract field names from a search query. The field name starts with a lower case letter (the
     * names of dynamic fields should start with a prefix) or with underscore and can contain Unicode letters and
     * digits, plus also the following special characters: '_' (underscore), '-' (dash), '.' (dot) and '$' (dollar).
     * Also, the field name appears either at the start of the query or after one of these: '+' (plus), '-' (minus), '('
     * (round left bracket) or a white space.
     * 
     * @see ExtendedDismaxQParser#getFieldName()
     */
    private static final Pattern FIELD_PATTERN = Pattern.compile("(?:^|[+\\-(\\s])([a-z_][\\p{L}\\p{N}_\\-.$]*):");

    /**
     * The string used to define a dynamic field.
     */
    private static final String WILDCARD = "*";

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req)
    {
        return super.createParser(qstr, localParams, withFieldAliases(qstr, params), req);
    }

    /**
     * Extends the given search parameters with aliases for the fields that appear in the given search query.
     * 
     * @param query the search query where to look for field names
     * @param parameters the search parameters to extend
     * @return the extended search parameters
     */
    public SolrParams withFieldAliases(String query, SolrParams parameters)
    {
        Set<String> fieldNames = extractFieldNames(query);
        // Add default query fields (these fields are used to search for free text that appears in the query).
        String defaultQueryFields = parameters.get("qf");
        if (defaultQueryFields != null) {
            fieldNames.addAll(SolrPluginUtils.parseFieldBoosts(defaultQueryFields).keySet());
        }
        if (fieldNames.isEmpty()) {
            return parameters;
        }

        Map<String, String> aliasParameters = new HashMap<String, String>();
        addMultilingualFieldAliases(fieldNames, aliasParameters, parameters);
        addTypedDynamicFieldAliases(fieldNames, aliasParameters, parameters);

        return aliasParameters.isEmpty() ? parameters : SolrParams.wrapDefaults(new MapSolrParams(aliasParameters),
            parameters);
    }

    /**
     * Adds aliases for multilingual fields.
     * 
     * @param fieldNames the set of field names to add aliases for
     * @param aliasParameters the map where the aliases are collected
     * @param parameters the search query parameters used to extract the list of multilingual fields and the list of
     *            supported locales
     */
    private void addMultilingualFieldAliases(Set<String> fieldNames, Map<String, String> aliasParameters,
        SolrParams parameters)
    {
        List<String> multilingualFields = getListParameter("xwiki.multilingualFields", parameters);
        if (multilingualFields.isEmpty()) {
            return;
        }

        // There is at least one supported locale, the ROOT locale.
        List<String> supportedLocales = getSupportedLocales(parameters);

        for (String fieldName : fieldNames) {
            if (matchesFieldName(fieldName, multilingualFields)) {
                addAliases(fieldName, supportedLocales, aliasParameters);
            }
        }
    }

    /**
     * Adds aliases for typed dynamic fields.
     * <p>
     * The names of the non-string dynamic fields must be suffixed with the data type (instead of the locale) in order
     * for them to be indexed correctly. Thus we need to add aliases for dynamic field names that will match the
     * configured data types.
     * 
     * @param fieldNames the set of field names to add aliases for
     * @param aliasParameters the map where the aliases are collected
     * @param parameters the search query parameters used to extract the list of typed dynamic fields and the list of
     *            supported data types
     */
    private void addTypedDynamicFieldAliases(Set<String> fieldNames, Map<String, String> aliasParameters,
        SolrParams parameters)
    {
        List<String> typedDynamicFields = getListParameter("xwiki.typedDynamicFields", parameters);
        List<String> dynamicFieldTypes = getListParameter("xwiki.dynamicFieldTypes", parameters);
        if (typedDynamicFields.isEmpty() || dynamicFieldTypes.isEmpty()) {
            return;
        }

        for (String fieldName : fieldNames) {
            if (matchesFieldName(fieldName, typedDynamicFields)) {
                addAliases(fieldName, dynamicFieldTypes, aliasParameters);
            }
        }
    }

    /**
     * Extracts the field names from the given search query.
     * 
     * @param query the search query
     * @return the set of field names
     */
    public Set<String> extractFieldNames(String query)
    {
        Set<String> fieldNames = new HashSet<String>();
        Matcher matcher = FIELD_PATTERN.matcher(query);
        while (matcher.find()) {
            fieldNames.add(matcher.group(1));
        }
        return fieldNames;
    }

    /**
     * Get the value of a list parameter.
     * 
     * @param parameter the name of a list parameter (its value is a comma-separated list of strings)
     * @param parameters the query parameters
     * @return the list value
     */
    private static List<String> getListParameter(String parameter, SolrParams parameters)
    {
        String value = parameters.get(parameter);
        if (value != null) {
            return Arrays.asList(LIST_SEPARATOR.split(value));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @param parameters the query parameters
     * @return the list of supported locales
     */
    private static List<String> getSupportedLocales(SolrParams parameters)
    {
        List<String> supportedLocalesList = new ArrayList<String>();
        supportedLocalesList.add("_");
        String supportedLocales = parameters.get("xwiki.supportedLocales");
        if (supportedLocales != null) {
            supportedLocalesList.addAll(Arrays.asList(LIST_SEPARATOR.split(supportedLocales)));
        }
        return supportedLocalesList;
    }

    /**
     * @param fieldName the field name to match
     * @param fieldNamePatterns the list of field name patterns; a field name pattern is a string that can start or
     *        end with a {@link #WILDCARD}.
     * @return {@code true} if at least one of the field name patterns matches the given field name, {@code false}
     *         otherwise
     */
    private boolean matchesFieldName(String fieldName, List<String> fieldNamePatterns)
    {
        for (String fieldNamePattern : fieldNamePatterns) {
            if (fieldNamePattern.equals(fieldName)) {
                return true;
            } else if (fieldNamePattern.endsWith(WILDCARD)) {
                if (fieldName.startsWith(fieldNamePattern.substring(0, fieldNamePattern.length() - 1))) {
                    return true;
                }
            } else if (fieldNamePattern.startsWith(WILDCARD)) {
                if (fieldName.endsWith(fieldNamePattern.substring(1))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds aliases for the specified field to the given parameters.
     * 
     * @param fieldName a field name
     * @param suffixes the list of alias suffixes
     * @param aliasParameters where to add the aliases
     */
    private void addAliases(String fieldName, List<String> suffixes, Map<String, String> aliasParameters)
    {
        String aliasParameterName = String.format("f.%s.qf", fieldName);
        StringBuilder aliasParameterValue = new StringBuilder();
        for (String suffix : suffixes) {
            aliasParameterValue.append(' ').append(fieldName).append('_').append(suffix);
        }
        String previousValue = aliasParameters.get(aliasParameterName);
        aliasParameters.put(aliasParameterName, previousValue == null ? aliasParameterValue.substring(1)
            : previousValue + aliasParameterValue.toString());
    }
}
