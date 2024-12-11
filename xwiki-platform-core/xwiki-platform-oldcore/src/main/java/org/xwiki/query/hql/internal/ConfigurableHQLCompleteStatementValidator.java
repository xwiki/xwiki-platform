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
package org.xwiki.query.hql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

/**
 * A validator which can be configured. This validator is executer before the default one using component priorities
 * 
 * @version $Id$
 * @since 17.0.0RC1
 * @since 16.10.2
 * @since 15.10.16
 * @since 16.4.6
 */
@Component
@Singleton
@Named("configuration")
@Priority(ComponentDescriptor.DEFAULT_PRIORITY - 100)
public class ConfigurableHQLCompleteStatementValidator implements HQLCompleteStatementValidator, Initializable
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Inject
    private Logger logger;

    private List<Pattern> unsafe;

    private List<Pattern> safe;

    @Override
    public void initialize() throws InitializationException
    {
        this.unsafe = getPatterns("query.hql.unsafe");
        this.safe = getPatterns("query.hql.safe");
    }

    private List<Pattern> getPatterns(String key)
    {
        List<String> patternStrings = this.configuration.getProperty(key, List.class);

        List<Pattern> patterns = new ArrayList<>(patternStrings.size());
        for (String patternString : patternStrings) {
            try {
                patterns.add(Pattern.compile(patternString));
            } catch (Exception e) {
                this.logger.warn("Failed to parse pattern [{}] for configuration [{}]: {}", patternString, key,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return patterns;
    }

    @Override
    public Optional<Boolean> isSafe(String statement)
    {
        for (Pattern pattern : this.unsafe) {
            if (pattern.matcher(statement).matches()) {
                return Optional.of(Boolean.FALSE);
            }
        }

        for (Pattern pattern : this.safe) {
            if (pattern.matcher(statement).matches()) {
                return Optional.of(Boolean.TRUE);
            }
        }

        return Optional.empty();
    }
}
