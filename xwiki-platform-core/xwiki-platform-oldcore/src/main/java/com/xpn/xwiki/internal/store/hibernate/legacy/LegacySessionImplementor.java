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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.UnknownPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.logging.LoggerConfiguration;

import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;
import com.xpn.xwiki.objects.BaseProperty;

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

    private static final Pattern LEGACY_PROPERTY_ID_MATCHER = Pattern.compile("\\bid\\.(id|name)\\b");

    /**
     * A string literal or an identifier (possibly qualified), the only parts of a statement the conversion of the
     * legacy property identifier paths needs to distinguish.
     */
    private static final Pattern TOKEN_MATCHER = Pattern.compile("'(?:[^']|'')*'|[\\w$]+(?:\\.[\\w$]+)*");

    /**
     * The keywords which can follow an entity name in a from clause, when the entity has no alias.
     */
    private static final String AS = "as";

    private static final Set<String> NOT_ALIASES = Set.of(AS, "where", "join", "inner", "left", "right", "full",
        "cross", "outer", "fetch", "order", "group", "having", "union", "intersect", "except", "on", "with", "set",
        "limit", "offset");

    private static final Pattern LEGACY_PROPERTY_ID_ERROR =
        Pattern.compile("Could not interpret attribute '(id|name)' of basic-valued path '[^']*\\.id'");

    private static final String ID = "id";

    private static final String NAME = "name";

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
     * @since 19.0.0RC1
     */
    public static boolean containsLegacyPropertyIdentifier(String statement)
    {
        return LEGACY_PROPERTY_ID_MATCHER.matcher(statement).find();
    }

    /**
     * Replace the legacy paths to the identifier of the properties ({@code prop.id.id} and {@code prop.id.name}, or
     * {@code id.id} and {@code id.name} when the property entity has no alias) by the valid ones ({@code prop.id} and
     * {@code prop.name}). Only the paths starting with an alias declared on a property entity are converted, and
     * string literals are left untouched.
     *
     * @param queryString the statement to convert
     * @return the converted statement, or {@code null} if it does not contain any legacy property identifier path
     */
    private String replaceLegacyPropertyIdentifier(String queryString)
    {
        Set<String> propertyEntityNames = getPropertyEntityNames();

        // Find the aliases declared on property entities
        Set<String> aliases = new HashSet<>();
        boolean unaliasedProperty = false;
        List<MatchResult> tokens = TOKEN_MATCHER.matcher(queryString).results().toList();
        for (int i = 0; i < tokens.size(); ++i) {
            if (propertyEntityNames.contains(tokens.get(i).group())) {
                String alias = i + 1 < tokens.size() ? getAlias(tokens, i + 1) : null;
                if (alias != null) {
                    aliases.add(alias);
                } else {
                    unaliasedProperty = true;
                }
            }
        }

        // Convert the paths starting with one of these aliases
        StringBuilder builder = new StringBuilder(queryString.length());
        int index = 0;
        boolean converted = false;
        for (MatchResult token : tokens) {
            String convertedToken = convertPropertyIdentifierPath(token.group(), aliases, unaliasedProperty);
            if (convertedToken != null) {
                builder.append(queryString, index, token.start());
                builder.append(convertedToken);
                index = token.end();
                converted = true;
            }
        }

        if (!converted) {
            return null;
        }

        builder.append(queryString, index, queryString.length());
        String convertedQueryString = builder.toString();

        if (this.loggerConfiguration.isDeprecatedLogEnabled()) {
            LOGGER.warn("Deprecated usage of the HQL paths [prop.id.id] and [prop.id.name] to the identifier of the"
                + " object properties; use [prop.id] and [prop.name] instead. Query [{}] has been converted to [{}]",
                queryString, convertedQueryString);
        }

        return convertedQueryString;
    }

    private static String getAlias(List<MatchResult> tokens, int index)
    {
        String alias = tokens.get(index).group();
        if (AS.equalsIgnoreCase(alias) && index + 1 < tokens.size()) {
            alias = tokens.get(index + 1).group();
        }

        // The token following the entity name is the alias only if it's an identifier, and not a keyword, which is
        // only a good enough approximation since the tokens are not necessarily contiguous
        return alias.startsWith("'") || alias.contains(".") || NOT_ALIASES.contains(alias.toLowerCase(Locale.ROOT))
            ? null : alias;
    }

    private static String convertPropertyIdentifierPath(String token, Set<String> aliases, boolean unaliasedProperty)
    {
        String[] elements = StringUtils.split(token, '.');

        if (elements.length == 3 && aliases.contains(elements[0]) && isLegacyPropertyIdentifierPath(elements, 1)) {
            return elements[0] + '.' + elements[2];
        } else if (elements.length == 2 && unaliasedProperty && isLegacyPropertyIdentifierPath(elements, 0)) {
            return elements[1];
        }

        return null;
    }

    private static boolean isLegacyPropertyIdentifierPath(String[] elements, int index)
    {
        return ID.equals(elements[index]) && (ID.equals(elements[index + 1]) || NAME.equals(elements[index + 1]));
    }

    private Set<String> getPropertyEntityNames()
    {
        Set<String> names = new HashSet<>();
        getFactory().getMappingMetamodel().forEachEntityDescriptor(entity -> {
            if (BaseProperty.class.isAssignableFrom(entity.getMappedClass())) {
                String entityName = entity.getEntityName();
                names.add(entityName);
                names.add(StringUtils.substringAfterLast(entityName, '.'));
            }
        });

        return names;
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
                if (propertyIdentifier) {
                    throw e;
                }

                convertedStatement = replaceLegacyPropertyIdentifier(convertedStatement, e);
                propertyIdentifier = true;
            }
        }
    }

    private String replaceLegacyPropertyIdentifier(String statement, UnknownPathException e)
    {
        String convertedStatement =
            isLegacyPropertyIdentifierError(e) ? replaceLegacyPropertyIdentifier(statement) : null;
        if (convertedStatement == null) {
            throw e;
        }

        return convertedStatement;
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
