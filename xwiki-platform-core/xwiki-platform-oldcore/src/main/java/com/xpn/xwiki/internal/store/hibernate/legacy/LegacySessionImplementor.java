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

import java.util.Collections;
import java.util.regex.Pattern;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.query.spi.QueryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @since 11.4RC1
 * @deprecated
 */
@Deprecated
public class LegacySessionImplementor extends SessionDelegatorBaseImpl
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWiki.class);

    private static final String LEGACY_ORDINAL_PARAMS_PREFIX =
        QueryTranslator.ERROR_LEGACY_ORDINAL_PARAMS_NO_LONGER_SUPPORTED.substring(0, 115);

    private static final Pattern LEGACY_MATCHER = Pattern.compile("\\?($|[^\\d])");

    /**
     * @param delegate the actual session
     */
    public LegacySessionImplementor(SessionImplementor delegate)
    {
        super(delegate);
    }

    private String replaceLegacyQueryParameters(String queryString)
    {
        String convertedQueryString = HqlQueryUtils.replaceLegacyQueryParameters(queryString);

        LOGGER.warn(
            "Deprecated usage legacy-style HQL ordinal parameters (`?`);"
                + " use JPA-style ordinal parameters (e.g., `?1`) instead. Query [{}] has been converted to [{}]",
            queryString, convertedQueryString);

        return convertedQueryString;
    }

    private String checkStatement(String statement)
    {
        // Check if the statement might (it's not using the real hql parser to limit the number of fully parser
        // statements) contain legacy HQL ordinal parameters
        if (LEGACY_MATCHER.matcher(statement).find()) {
            // Check if the statement is valid and if not translate it
            // FIXME: find a more efficient way (we currently parse and validate the statement twice, plus the if which
            // is parsing the statement String...). The problem is that when createQuery fail it's too late and the
            // session is dead (marked as rollback only) and it's not possible to avoid it without reimplementing a lot
            // of stuff.
            try {
                getFactory().getQueryPlanCache().getHQLQueryPlan(statement, false, Collections.emptyMap());
            } catch (QueryException e) {
                if (e.getMessage() != null && e.getMessage().contains(LEGACY_ORDINAL_PARAMS_PREFIX)) {
                    return replaceLegacyQueryParameters(statement);
                }

                throw e;
            }
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
