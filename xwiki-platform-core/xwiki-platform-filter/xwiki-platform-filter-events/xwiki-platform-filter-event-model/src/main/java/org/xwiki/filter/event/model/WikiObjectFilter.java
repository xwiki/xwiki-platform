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
package org.xwiki.filter.event.model;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.annotation.Default;

/**
 * Object related events.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public interface WikiObjectFilter
{
    /**
     * @type {@link String}
     * @since 9.0RC1
     */
    String PARAMETER_NAME = "name";

    /**
     * @type {@link Integer}
     */
    String PARAMETER_NUMBER = "number";

    /**
     * @type {@link String}
     */
    String PARAMETER_CLASS_REFERENCE = "class_reference";

    /**
     * @type {@link String}
     */
    String PARAMETER_GUID = "guid";

    /**
     * @param name the name part of the {@link org.xwiki.model.reference.ObjectReference}
     * @param parameters the properties of the object
     * @throws FilterException when failing to send event
     */
    void beginWikiObject(String name, @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters)
        throws FilterException;

    /**
     * @param name the name part of the {@link org.xwiki.model.reference.ObjectReference}
     * @param parameters the properties of the object
     * @throws FilterException when failing to send event
     */
    void endWikiObject(String name, @Default(FilterEventParameters.DEFAULT) FilterEventParameters parameters)
        throws FilterException;
}
