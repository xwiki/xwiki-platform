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
package com.xpn.xwiki.internal.store.hibernate.legacy;

import java.util.regex.Pattern;

import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.logging.LoggerConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;

/**
 * Retro compatibility layer for features removed in Hibernate:
 * <ul>
 * <li>Try to limit the damages of the removed support for "Legacy-style positional parameters" (`?`). See
 * https://hibernate.atlassian.net/browse/HHH-12101.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 11.5RC1
 * @deprecated
 */
@Deprecated
public class LegacySessionImplementor extends SessionDelegatorBaseImpl
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWiki.class);

    private static final Pattern LEGACY_MATCHER = Pattern.compile("\\?($|[^\\d])");

    private final LoggerConfiguration loggerConfiguration;

    /**
     * @param delegate the actual session
     * @param loggerConfiguration use to know if a warning should be logged when deprecated API is used
     */
    public LegacySessionImplementor(SessionImplementor delegate, LoggerConfiguration loggerConfiguration)
    {
        super(delegate);

        this.loggerConfiguration = loggerConfiguration;
    }

    /**
     * @param statement the statement to parse
     * @return true if the statement contains legacy-style positional parameters" (`?`)
     */
    public static boolean containsLegacyOrdinalStatement(String statement)
    {
        return LEGACY_MATCHER.matcher(statement).find();
    }

    private String replaceLegacyQueryParameters(String queryString)
    {
        String convertedQueryString = HqlQueryUtils.replaceLegacyQueryParameters(queryString);

        if (this.loggerConfiguration.isDeprecatedLogEnabled()) {
            LOGGER.warn(
                "Deprecated usage legacy-style HQL ordinal parameters (`?`);"
                    + " use JPA-style ordinal parameters (e.g., `?1`) instead. Query [{}] has been converted to [{}]",
                queryString, convertedQueryString);
        }

        return convertedQueryString;
    }

    private String checkStatement(String statement)
    {
        // Check if the statement might (it's not using the real hql parser to limit the number of fully parsed
        // statements) contain legacy HQL ordinal parameters.
        // Note: Hibernate 6 removed org.hibernate.hql.spi.QueryTranslator (and its
        // ERROR_LEGACY_ORDINAL_PARAMS_NO_LONGER_SUPPORTED marker) as well as the SessionFactory query plan cache API
        // that were previously used to confirm that the statement actually failed because of legacy ordinal
        // parameters before converting it. As a faithful equivalent we now rely solely on the heuristic matcher to
        // decide whether to convert the legacy ordinal parameters.
        if (containsLegacyOrdinalStatement(statement)) {
            return replaceLegacyQueryParameters(statement);
        }

        return null;
    }

    @Override
    public QueryImplementor createQuery(String queryString)
    {
        String convertedStatement = checkStatement(queryString);

        if (convertedStatement != null) {
            return new LegacyQueryImplementor(super.createQuery(convertedStatement));

        } else {
            return super.createQuery(queryString);
        }
    }

    @Override
    public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultType)
    {
        String convertedStatement = checkStatement(queryString);

        if (convertedStatement != null) {
            return new LegacyQueryImplementor(super.createQuery(convertedStatement, resultType));

        } else {
            return super.createQuery(queryString, resultType);
        }
    }
}
