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

package org.xwiki.wikistream.xar.internal.input;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;


/**
 * @version $Id$
 * @since 5.2RC1
 */
public abstract class AbstractReader
{
    private SyntaxFactory syntaxFactory;

    public AbstractReader()
    {
    }

    public AbstractReader(SyntaxFactory syntaxFactory)
    {
        this.syntaxFactory = syntaxFactory;
    }

    protected Object convert(Class< ? > type, String source) throws ParseException
    {
        Object value = source;

        if (type == Locale.class) {
            value = toLocale(source);
        } else if (type == Date.class) {
            value = new Date(Long.parseLong(source));
        } else if (type == Boolean.class) {
            value = Boolean.valueOf(source).booleanValue();
        } else if (type == Syntax.class) {
            value = this.syntaxFactory.createSyntaxFromIdString(source);
        } else if (type == Integer.class) {
            value = Integer.parseInt(source);
        }

        return value;
    }

    protected Locale toLocale(String value)
    {
        Locale locale = null;
        if (value != null) {
            String valueString = value.toString();
            if (valueString.length() == 0) {
                locale = Locale.ROOT;
            } else {
                locale = LocaleUtils.toLocale(valueString);
            }
        }

        return locale;
    }
}
