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
package org.xwiki.wikistream.xar.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.wikistream.model.filter.WikiObjectFilter;
import org.xwiki.wikistream.xar.internal.XARUtils.Parameter;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XARObjectModel
{
    public static final String ELEMENT_OBJECT = "object";

    public static final String ELEMENT_NAME = "name";

    public static final String ELEMENT_NUMBER = "number";

    public static final String ELEMENT_CLASSNAME = "className";

    public static final String ELEMENT_GUID = "guid";

    // Utils

    public static final Map<String, Parameter> OBJECT_PARAMETERS = new HashMap<String, Parameter>()
    {
        {
            put(ELEMENT_CLASSNAME, new Parameter(WikiObjectFilter.PARAMETER_CLASS_REFERENCE));
            put(ELEMENT_GUID, new Parameter(WikiObjectFilter.PARAMETER_GUID));
            put(ELEMENT_NUMBER, new Parameter(WikiObjectFilter.PARAMETER_NUMBER, Integer.class));
        }
    };
}
