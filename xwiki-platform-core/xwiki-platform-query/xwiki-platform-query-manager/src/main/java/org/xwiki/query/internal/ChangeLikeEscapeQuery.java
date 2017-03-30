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
package org.xwiki.query.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.query.Query;
import org.xwiki.query.WrappingQuery;

/**
 * Wraps a {@link Query} to perform modifications on it in order to modify the parameters and statements to change
 * the default escape character. See {@link ChangeLikeEscapeFilter} for more details.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class ChangeLikeEscapeQuery extends WrappingQuery
{
    // TODO: Handle escapes of ' in LIKE clause
    // TODO: Handle functions other than LOWER()
    private static final Pattern LIKE_PATTERN =
        Pattern.compile("LIKE +(\\?[0-9]*|:[a-zA-Z]+|LOWER\\(\\?\\)) *", Pattern.CASE_INSENSITIVE);

    private static final Pattern POSITIONAL_INDEX_PATTERN = Pattern.compile("\\?([0-9]*)");

    private static final String ESCAPE = "ESCAPE";

    private List<String> modifiedNamedParameters;
    private List<Integer> modifiedPositionalParameters;

    /**
     * @param wrappedQuery the query to wrap and for which we're changing some behavior
     */
    public ChangeLikeEscapeQuery(Query wrappedQuery)
    {
        super(wrappedQuery);
    }

    @Override
    public Map<String, Object> getNamedParameters()
    {
        return convertParameters(modifiedNamedParameters, super.getNamedParameters());
    }

    @Override
    public Map<Integer, Object> getPositionalParameters()
    {
        return convertParameters(modifiedPositionalParameters, super.getPositionalParameters());
    }

    private <T> Map<T, Object> convertParameters(List<T> modifiedParameters, Map<T, Object> parametersToEscape)
    {
        if (modifiedParameters != null) {
            // Escape entries from the Map where needed
            Map<T, Object> escapedMap = new LinkedHashMap<>();
            for (Map.Entry<T, Object> entry : parametersToEscape.entrySet()) {
                // TODO: Also handle Arrays and collections in the future
                if (modifiedParameters.contains(entry.getKey()) && entry.getValue() instanceof String) {
                    String stringValue = (String) entry.getValue();
                    escapedMap.put(entry.getKey(), stringValue.replaceAll("(!)", "!$1"));
                } else {
                    escapedMap.put(entry.getKey(), entry.getValue());
                }
            }
            return escapedMap;
        } else {
            return parametersToEscape;
        }
    }

    @Override
    public String getStatement()
    {
        String statement;
        if (getLanguage().equals(Query.HQL)) {
            statement = addEscapeDefinition(super.getStatement());
        } else {
            statement = super.getStatement();
        }
        return statement;
    }

    /**
     * Handle the case of MySQL: in MySQL a '\' character is a special escape character. In addition we often
     * use '\' in Entity References. For example to find nested pages in a page with a dot would result in
     * something like "LIKE '.%.a\.b.%'" which wouldn't work on MySQL. Thus we need to replace the default
     * escape character with another one. To be safe we perform 2 checks:
     * <ul>
     *  <li>For each LIKE clause, verify that the statement doesn't already specify an ESCAPE term</li>
     *  <li>Verify that we pick an escape character that doesn't already exist in the statement. This could be a
     *   problem for example if we were using the "!" escape char and we had something like
     *   "... WHERE a LIKE 'c!d' AND b LIKE :value" (the value "c!d" would be understood as "cd").</li>
     * </ul>
     */
    private String addEscapeDefinition(String statement)
    {
        String amplifiedStatement;

        // Find all LIKE clauses and for each one add an ESCAPE clause if needed
        Matcher matcher = LIKE_PATTERN.matcher(statement);
        StringBuffer buffer = new StringBuffer();
        int counter = 0;
        while (matcher.find()) {
            counter++;
            // Is there an ESCAPE just after the LIKE?
            if (matcher.end() + ESCAPE.length() >= statement.length() || !ESCAPE.equalsIgnoreCase(
                statement.substring(matcher.end(), matcher.end() + ESCAPE.length())))
            {
                matcher.appendReplacement(buffer, String.format("%s ESCAPE '!' ", matcher.group().trim()));
                registerModifiedParameter(matcher.group(1), counter);
                matcher.appendTail(buffer);
            }
        }
        if (buffer.length() != 0) {
            amplifiedStatement = buffer.toString().trim();
        } else {
            amplifiedStatement = statement;
        }
        return amplifiedStatement;
    }

    private void registerModifiedParameter(String parameterMatch, int counter)
    {
        if (parameterMatch.startsWith(":")) {
            if (modifiedNamedParameters == null) {
                modifiedNamedParameters = new ArrayList<>();
            }
            modifiedNamedParameters.add(parameterMatch.substring(1));
        } else if (parameterMatch.startsWith("?") || parameterMatch.equalsIgnoreCase("LOWER(?)")) {
            if (modifiedPositionalParameters == null) {
                modifiedPositionalParameters = new ArrayList<>();
            }
            // Find if there's a position index, e.g. "?1"
            Matcher positionMatcher = POSITIONAL_INDEX_PATTERN.matcher(parameterMatch);
            positionMatcher.find();
            if (positionMatcher.group(1).length() > 0) {
                modifiedPositionalParameters.add(Integer.parseInt(positionMatcher.group(1)));
            } else {
                modifiedPositionalParameters.add(counter);
            }
        }
    }
}
