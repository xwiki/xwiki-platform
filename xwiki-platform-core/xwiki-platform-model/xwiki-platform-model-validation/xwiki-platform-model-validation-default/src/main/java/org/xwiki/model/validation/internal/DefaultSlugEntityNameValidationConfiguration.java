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
package org.xwiki.model.validation.internal;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Configuration component for {@link SlugEntityNameValidation}.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component
@Singleton
public class DefaultSlugEntityNameValidationConfiguration
    implements SlugEntityNameValidationConfiguration
{
    private static final String LOWERCASE_PROPERTY_KEY = "slug.lowercase";

    private static final String DOTSBETWEENDIGITS_PROPERTY_KEY = "slug.dotsBetweenDigits";

    private static final String FORBIDDENWORDS_PROPERTY_KEY = "slug.forbiddenWords";

    @Inject
    @Named("entitynamevalidation")
    private ConfigurationSource configurationSource;

    @Override
    public boolean convertToLowercase()
    {
        return this.configurationSource.getProperty(LOWERCASE_PROPERTY_KEY, Boolean.class);
    }

    @Override
    public boolean isDotAllowedBetweenDigits()
    {
        return this.configurationSource.getProperty(DOTSBETWEENDIGITS_PROPERTY_KEY, Boolean.class);
    }

    @Override
    public Set<String> getForbiddenWords()
    {
        String words = this.configurationSource.getProperty(FORBIDDENWORDS_PROPERTY_KEY, String.class);
        return words == null ? Set.of() : Arrays.stream(words.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }
}
