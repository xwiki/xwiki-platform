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
package org.xwiki.gwt.user.client;

import java.util.Set;

/**
 * Interface for retrieving the configuration parameters of the WYSIWYG editor.
 * 
 * @version $Id$
 */
public interface Config
{
    /**
     * Returns the value of the specified parameter, defaulting to <code>null</code> if the parameter doesn't exist.
     * 
     * @param paramName The name of the parameter.
     * @return The value of the given parameter or <code>null</code> if there's no such parameter.
     */
    String getParameter(String paramName);

    /**
     * Returns the value of the specified parameter, defaulting to the given value if the parameter doesn't exist.
     * 
     * @param paramName The name of the parameter.
     * @param defaultValue The default value if the parameter doesn't exist.
     * @return The value of the given parameter or the specified default value if there's no such parameter.
     */
    String getParameter(String paramName, String defaultValue);

    /**
     * @return The set of all the parameter names.
     */
    Set<String> getParameterNames();
}
