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
package org.xwiki.internal.objects;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Abstract helper for {@link ObjectPropertyParser}.
 * The idea of this abstract is to always rely on {@link PropertyClass#fromString(String)}
 * for parsing the value, and to actually ask the different implementations to retrieve the
 * {@link PropertyClass} instance matching the requested hint.
 *
 * @version $Id$
 */
public abstract class AbstractObjectPropertyParser implements ObjectPropertyParser
{
    /**
     * @return an instance of {@link PropertyClass} matching the requested hint.
     */
    protected abstract PropertyClass getBaseClass() throws XWikiException;

    @Override
    public BaseProperty<?> fromString(String value) throws XWikiException
    {
        return getBaseClass().fromString(value);
    }

    @Override
    public BaseProperty<?> fromValue(Object value) throws XWikiException
    {
        if (value instanceof String stringValue) {
            return fromString(stringValue);
        } else {
            return getBaseClass().fromValue(value);
        }
    }
}
