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

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Component role dedicated to instantiate a xproperty and set its value based on the given string.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@Role
public interface ObjectPropertyParser
{
    /**
     * Creates a new property instance (the actual used class depends on the concrete implementation of the
     * component) and set its value based on the parsing of the given string value.
     * @param value the actual value to be parsed for setting the property value.
     * @return an instance of an xproperty with the value properly set.
     * @throws XWikiException in case of problem to create the instance or parse its value.
     */
    BaseProperty<?> fromString(String value) throws XWikiException;

    /**
     * Creates a new property instance (the actual used class depends on the concrete implementation of the
     * component) and set its value based on the given value.
     * @param value the actual value to be parsed for setting the property value.
     * @return an instance of an xproperty with the value properly set.
     * @throws XWikiException in case of problem to create the instance or parse its value.
     */
    BaseProperty<?> fromValue(Object value) throws XWikiException;
}
