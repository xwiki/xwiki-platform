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

package org.xwiki.filter.xar.internal.input;

import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.filter.FilterException;

/**
 * @version $Id$
 * @since 6.2M1
 */
public abstract class AbstractReader
{
    @Inject
    protected SyntaxFactory syntaxFactory;

    protected Object convert(Class< ? > type, String source) throws FilterException
    {
        Object value = source;

        if (type == Locale.class) {
            value = toLocale(source);
        } else if (type == Date.class) {
            value = StringUtils.isNotEmpty(source) ? new Date(Long.parseLong(source)) : null;
        } else if (type == Boolean.class) {
            value = StringUtils.isNotEmpty(source) ? Boolean.valueOf(source).booleanValue() : null;
        } else if (type == Syntax.class) {
            if (StringUtils.isNotEmpty(source)) {
                try {
                    value = this.syntaxFactory.createSyntaxFromIdString(source);
                } catch (ParseException e) {
                    throw new FilterException(String.format("Failed to create Syntax istance for [%s]", source), e);
                }
            } else {
                value = null;
            }
        } else if (type == Integer.class) {
            value = StringUtils.isNotEmpty(source) ? Integer.parseInt(source) : null;
        }

        return value;
    }

    protected Locale toLocale(String value)
    {
        Locale locale = null;
        if (value != null) {
            if (value.length() == 0) {
                locale = Locale.ROOT;
            } else {
                locale = LocaleUtils.toLocale(value);
            }
        }

        return locale;
    }
}
