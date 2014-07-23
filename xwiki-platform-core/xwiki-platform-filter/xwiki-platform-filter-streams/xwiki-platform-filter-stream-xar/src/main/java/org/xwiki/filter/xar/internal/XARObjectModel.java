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
package org.xwiki.filter.xar.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.xar.internal.model.XarObjectModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class XARObjectModel extends XarObjectModel
{
    // Utils

    public static final Map<String, EventParameter> OBJECT_PARAMETERS = new HashMap<String, EventParameter>()
    {
        {
            put(ELEMENT_CLASSNAME, new EventParameter(WikiObjectFilter.PARAMETER_CLASS_REFERENCE));
            put(ELEMENT_GUID, new EventParameter(WikiObjectFilter.PARAMETER_GUID));
            put(ELEMENT_NUMBER, new EventParameter(WikiObjectFilter.PARAMETER_NUMBER, Integer.class));
        }
    };
}
