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
package com.xpn.xwiki.internal.store.hibernate.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.Query;
import org.xwiki.query.WrappingQuery;

/**
 * Provide various SQL related utilities.
 * 
 * @version $Id$
 * @since 7.2M2
 */
public final class HqlQueryUtils
{
    private static final Pattern LEGACY_ORDINAL_PARAMS_PATTERN = Pattern.compile("([=\\s,\\(<>])\\?([=\\s,\\)<>]|$)");

    private HqlQueryUtils()
    {

    }

    /**
     * @param statement the statement to evaluate
     * @return true if the statement is complete, false otherwise
     */
    public static boolean isShortFormStatement(String statement)
    {
        return StringUtils.startsWithAny(statement.trim().toLowerCase(), ",", "from", "where", "order");
    }

    /**
     * Hibernate 5.3 removed support for "legacy HQL-style positional parameters (?)" but it's too much a breakage for
     * XWiki extension so we try to limit the damages as much as possible by converting the statements. See
     * https://hibernate.atlassian.net/browse/HHH-12101.
     * 
     * @param queryString the statement to convert
     * @return the converted statement
     * @since 11.5RC1
     */
    public static String replaceLegacyQueryParameters(String queryString)
    {
        String convertedString = queryString;

        for (int index = 1;; ++index) {
            Matcher matcher = LEGACY_ORDINAL_PARAMS_PATTERN.matcher(convertedString);

            if (!matcher.find()) {
                return convertedString;
            }

            StringBuilder builder = new StringBuilder();

            builder.append(convertedString, 0, matcher.end(1));
            builder.append('?');
            builder.append(index);
            builder.append(convertedString, matcher.start(2), convertedString.length());

            convertedString = builder.toString();
        }
    }

    /**
     * @param statement a potentially short form statement to complete
     * @return a complete version of the input {@link Query} (or the {@link Query} as is if it's already complete)
     * @since 17.0.0RC1
     * @since 16.10.2
     * @since 15.10.16
     * @since 16.4.6
     */
    public static String toCompleteStatement(String statement)
    {
        String completeStatement = statement;

        if (StringUtils.isEmpty(statement) || isShortFormStatement(statement)) {
            completeStatement = "select doc.fullName from XWikiDocument doc " + statement.trim();
        }

        return completeStatement;
    }

    /**
     * @param query a potentially short form query to complete
     * @return a complete version of the input {@link Query} (or the {@link Query} as is if it's already complete)
     * @since 17.0.0RC1
     * @since 16.10.2
     * @since 15.10.16
     * @since 16.4.6
     */
    public static Query toCompleteQuery(Query query)
    {
        Query completeQuery = query;

        String completeStatement = toCompleteStatement(query.getStatement());
        // Check of toCompleteStatement returned a different String (which means it completed it)
        @SuppressWarnings("java:S4973")
        boolean isCompleted = completeStatement != query.getStatement();
        if (isCompleted) {
            completeQuery = new WrappingQuery(query)
            {
                @Override
                public String getStatement()
                {
                    return completeStatement;
                }
            };
        }

        return completeQuery;
    }
}
