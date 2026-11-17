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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.UnknownPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.logging.LoggerConfiguration;

import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;

/**
 * Retro compatibility layer for features removed in Hibernate:
 * <ul>
 * <li>Try to limit the damages of the removed support for "Legacy-style positional parameters" (`?`). See
 * https://hibernate.atlassian.net/browse/HHH-12101.</li>
 * <li>Support the {@code prop.id.id} and {@code prop.id.name} paths to the identifier of the properties of the XWiki
 * objects, which are not valid anymore with the identifier mapped directly on the {@code BaseProperty} entity
 * ({@code prop.id} and {@code prop.name} should be used instead).</li>
 * </ul>
 * 
 * @version $Id$
 * @since 11.5RC1
 * @deprecated
 */
@Deprecated
public class LegacySessionImplementor extends SessionDelegatorBaseImpl
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacySessionImplementor.class);

    private static final String LEGACY_ORDINAL_PARAMS_ERROR = "Unlabeled ordinal parameter";

    private static final Pattern LEGACY_MATCHER = Pattern.compile("\\?($|[^\\d])");

    private static final Pattern LEGACY_PROPERTY_ID_MATCHER = Pattern.compile("\\b(\\w+)\\.id\\.(id|name)\\b");

    private static final Pattern LEGACY_PROPERTY_ID_ERROR =
        Pattern.compile("Could not interpret attribute '(id|name)' of basic-valued path '[^']*\\.id'");

    private static final int CONVERSIONS_CACHE_SIZE = 1000;

    /**
     * The result of the check of the statements containing legacy syntax, to avoid parsing them twice each time they
     * are executed.
     */
    private static final Map<String, Conversion> CONVERSIONS =
        Collections.synchronizedMap(new LinkedHashMap<>(CONVERSIONS_CACHE_SIZE, 0.75F, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Conversion> eldest)
            {
                return size() > CONVERSIONS_CACHE_SIZE;
            }
        });

    private static final Conversion NO_CONVERSION = new Conversion(null, false);

    /**
     * @param statement the statement to execute instead, or {@code null} if the statement does not need to be
     *            converted
     * @param ordinalParameters true if legacy-style positional parameters were converted
     */
    private record Conversion(String statement, boolean ordinalParameters)
    {
    }

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

    /**
     * @param statement the statement to parse
     * @return true if the statement might contain legacy paths to the identifier of the properties of the XWiki
     *         objects ({@code prop.id.id} or {@code prop.id.name})
     * @since 18.10.0RC1
     */
    public static boolean containsLegacyPropertyIdentifier(String statement)
    {
        return LEGACY_PROPERTY_ID_MATCHER.matcher(statement).find();
    }

    private String replaceLegacyPropertyIdentifier(String queryString)
    {
        String convertedQueryString = LEGACY_PROPERTY_ID_MATCHER.matcher(queryString).replaceAll("$1.$2");

        if (this.loggerConfiguration.isDeprecatedLogEnabled()) {
            LOGGER.warn("Deprecated usage of the HQL paths [prop.id.id] and [prop.id.name] to the identifier of the"
                + " object properties; use [prop.id] and [prop.name] instead. Query [{}] has been converted to [{}]",
                queryString, convertedQueryString);
        }

        return convertedQueryString;
    }

    private Conversion checkStatement(String statement)
    {
        // Check if the statement might (it's not using the real hql parser to limit the number of fully parser
        // statements) contain legacy syntax
        if (!containsLegacyOrdinalStatement(statement) && !containsLegacyPropertyIdentifier(statement)) {
            return NO_CONVERSION;
        }

        Conversion conversion = CONVERSIONS.get(statement);
        if (conversion == null) {
            conversion = convert(statement);
            CONVERSIONS.put(statement, conversion);
        }

        return conversion;
    }

    private Conversion convert(String statement)
    {
        // Check if the statement is valid and if not convert it, until it's valid or can't be converted anymore.
        // The statement is validated before creating the query because when createQuery fail it's too late and the
        // session is dead (marked as rollback only) and it's not possible to avoid it without reimplementing a lot of
        // stuff.
        String convertedStatement = statement;
        boolean ordinalParameters = false;
        boolean propertyIdentifier = false;
        while (true) {
            try {
                getFactory().getQueryEngine().getHqlTranslator().translate(convertedStatement, null);

                return ordinalParameters || propertyIdentifier
                    ? new Conversion(convertedStatement, ordinalParameters) : NO_CONVERSION;
            } catch (ParameterLabelException e) {
                if (ordinalParameters || !isLegacyOrdinalParametersError(e)) {
                    throw e;
                }

                convertedStatement = replaceLegacyQueryParameters(convertedStatement);
                ordinalParameters = true;
            } catch (UnknownPathException e) {
                if (propertyIdentifier || !isLegacyPropertyIdentifierError(e)) {
                    throw e;
                }

                convertedStatement = replaceLegacyPropertyIdentifier(convertedStatement);
                propertyIdentifier = true;
            }
        }
    }

    private static boolean isLegacyOrdinalParametersError(ParameterLabelException e)
    {
        return e.getMessage() != null && e.getMessage().contains(LEGACY_ORDINAL_PARAMS_ERROR);
    }

    private static boolean isLegacyPropertyIdentifierError(UnknownPathException e)
    {
        return e.getMessage() != null && LEGACY_PROPERTY_ID_ERROR.matcher(e.getMessage()).find();
    }

    private <T> QueryImplementor<T> wrap(QueryImplementor<T> query, Conversion conversion)
    {
        return conversion.ordinalParameters() ? LegacyQueryImplementor.wrap(query) : query;
    }

    @Override
    public QueryImplementor createQuery(String queryString)
    {
        Conversion conversion = checkStatement(queryString);

        if (conversion.statement() != null) {
            return wrap(super.createQuery(conversion.statement()), conversion);
        } else {
            return super.createQuery(queryString);
        }
    }

    @Override
    public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultType)
    {
        Conversion conversion = checkStatement(queryString);

        if (conversion.statement() != null) {
            return wrap(super.createQuery(conversion.statement(), resultType), conversion);
        } else {
            return super.createQuery(queryString, resultType);
        }
    }
}
