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

import java.util.Locale;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.validation.AbstractEntityNameValidation;

/**
 * A kebab-style name strategy that will remove all accents and replace special characters (including spaces) with
 * the {@code -} character.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Singleton
@Named(SlugEntityNameValidation.COMPONENT_NAME)
public class SlugEntityNameValidation extends AbstractEntityNameValidation
{
    protected static final String COMPONENT_NAME = "SlugEntityNameValidation";

    private static final String REPLACEMENT_CHARACTER = "-";
    private static final Pattern DASH_PATTERN = Pattern.compile("-+");
    private static final Pattern NONWORDCHARACTERS_PATTERN = Pattern.compile("[\\W]");
    private static final Pattern DOTSBETWEENDIGITS = Pattern.compile("(?<=\\d)\\.(?=\\d)");
    private static final Pattern NONWORD_PATTERN = Pattern.compile("\\W");
    private static final String PROTECTED_DOT = "__DOT__";

    @Inject
    private SlugEntityNameValidationConfiguration configuration;

    @Override
    public String transform(String name)
    {
        // Remove accents.
        String result = StringUtils.stripAccents(name);
        // Replace non-word characters by a REPLACEMENT_CHARACTER.
        if (this.configuration.isDotAllowedBetweenDigits()) {
            // 1) Protect dots between digits
            result = DOTSBETWEENDIGITS.matcher(result).replaceAll(PROTECTED_DOT);
            // 2) Replace all non-word characters
            result = NONWORD_PATTERN.matcher(result).replaceAll(REPLACEMENT_CHARACTER);
            // 3) Restore protected dots
            result = result.replace(PROTECTED_DOT, ".");
        } else {
            result = NONWORDCHARACTERS_PATTERN.matcher(result).replaceAll(REPLACEMENT_CHARACTER);
        }
        if (this.configuration.convertToLowercase()) {
            result = result.toLowerCase(Locale.ROOT);
        }
        // Remove forbidden words.
        for (String forbiddenWord : this.configuration.getForbiddenWords()) {
            // Note: the replacement is case-insensitive.
            result = result.replaceAll(String.format("(?i)(?:(?<=^)|(?<=-))\\Q%s\\E(?:(?=$)|(?=-))",
                forbiddenWord), "");
        }
        // Replace several REPLACEMENT_CHARACTER characters with a single one.
        result = DASH_PATTERN.matcher(result).replaceAll(REPLACEMENT_CHARACTER);
        // Remove leading and trailing REPLACEMENT_CHARACTER characters.
        result = Strings.CS.removeEnd(result, REPLACEMENT_CHARACTER);
        result = Strings.CS.removeStart(result, REPLACEMENT_CHARACTER);
        return result;
    }

    @Override
    public boolean isValid(String name)
    {
        return transform(name).equals(name);
    }
}
