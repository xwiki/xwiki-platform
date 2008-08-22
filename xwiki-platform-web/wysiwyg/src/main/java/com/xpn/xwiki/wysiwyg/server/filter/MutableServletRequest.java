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
package com.xpn.xwiki.wysiwyg.server.filter;

import javax.servlet.ServletRequest;

/**
 * A servlet request that can be modified. It is very useful, for instance, when you need to change the values of some
 * request parameters, inside a filter.
 */
public interface MutableServletRequest extends ServletRequest
{
    /**
     * Sets the value of a request parameter.
     * 
     * @param name The name of the request parameter.
     * @param value The new value of the request parameter.
     * @return The old value of the specified request parameter, or <code>null</code> if this is the first time we set
     *         its value.
     */
    String setParameter(String name, String value);

    /**
     * Sets the values of a request parameter.
     * 
     * @param name The name of the request parameter.
     * @param values The new array of values for the specified request parameter.
     * @return The old values of the specified request parameter, or <code>null</code> if this is the first time we
     *         set its values.
     */
    String[] setParameterValues(String name, String[] values);
}
