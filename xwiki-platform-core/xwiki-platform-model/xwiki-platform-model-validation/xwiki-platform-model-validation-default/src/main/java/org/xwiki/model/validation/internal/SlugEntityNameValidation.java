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

import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.validation.AbstractEntityNameValidation;

/**
 * A name strategy that will remove all accents and replace special characters (including spaces) with "-".
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
    private static final Pattern VALIDATION_PATTERN = Pattern.compile("^\\w+(-\\w+)*$");
    private static final String REPLACEMENT_CHARACTER = "-";

    @Override
    public String transform(String name)
    {
        String strippedAccents = StringUtils.stripAccents(name);
        String result = RegExUtils.replaceAll(strippedAccents, "[\\W]", REPLACEMENT_CHARACTER);
        result = RegExUtils.replaceAll(result, "[-]+", REPLACEMENT_CHARACTER);
        if (result.endsWith(REPLACEMENT_CHARACTER)) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.startsWith(REPLACEMENT_CHARACTER)) {
            result = result.substring(1);
        }
        return result;
    }

    @Override
    public boolean isValid(String name)
    {
        return VALIDATION_PATTERN.matcher(name).matches();
    }
}
