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
package org.xwiki.wikistream.filter.user;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;

/**
 * User related events.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Unstable
public interface UserFilter
{
    // Properties

    /**
     * @type String
     */
    String PARAMETER_FIRSTNAME = "firstname";

    /**
     * @type String
     */
    String PARAMETER_LASTNAME = "lastname";

    /**
     * @type String
     */
    String PARAMETER_EMAIL = "email";

    /**
     * @type String
     */
    String PARAMETER_CREATION_DATE = "creation_date";

    /**
     * @type String
     */
    String PARAMETER_REVISION_DATE = "revision_date";

    /**
     * @type Boolean
     */
    String PARAMETER_ACTIVE = "active";

    // Events

    void beginUser(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void endUser(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;
}
