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
package org.xwiki.filter.event.user;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;

/**
 * User related events.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public interface UserFilter
{
    // Properties

    /**
     * @type {@link String}
     */
    String PARAMETER_FIRSTNAME = "firstname";

    /**
     * @type {@link String}
     */
    String PARAMETER_LASTNAME = "lastname";

    /**
     * @type {@link String}
     */
    String PARAMETER_EMAIL = "email";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_CREATION_DATE = "creation_date";

    /**
     * @type {@link java.util.Date}
     */
    String PARAMETER_REVISION_DATE = "revision_date";

    /**
     * @type {@link Boolean}
     */
    String PARAMETER_ACTIVE = "active";

    // Events

    /**
     * @param name the name of the user
     * @param parameters the parameters of the user
     * @throws FilterException when failing to send event
     */
    void beginUser(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws FilterException;

    /**
     * @param name the name of the user
     * @param parameters the parameters of the user
     * @throws FilterException when failing to send event
     */
    void endUser(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws FilterException;
}
