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

import org.xwiki.filter.event.model.WikiClassFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.xar.internal.model.XarClassModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class XARClassModel extends XarClassModel
{
    // Utils

    public static final Map<String, EventParameter> CLASS_PARAMETERS = new HashMap<String, EventParameter>()
    {
        {
            put(ELEMENT_CUSTOMCLASS, new EventParameter(WikiClassFilter.PARAMETER_CUSTOMCLASS));
            put(ELEMENT_CUSTOMMAPPING, new EventParameter(WikiClassFilter.PARAMETER_CUSTOMMAPPING));
            put(ELEMENT_SHEET_DEFAULTVIEW, new EventParameter(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW));
            put(ELEMENT_SHEET_DEFAULTEDIT, new EventParameter(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT));
            put(ELEMENT_DEFAULTSPACE, new EventParameter(WikiClassFilter.PARAMETER_DEFAULTSPACE));
            put(ELEMENT_NAMEFIELD, new EventParameter(WikiClassFilter.PARAMETER_NAMEFIELD));
            put(ELEMENT_VALIDATIONSCRIPT, new EventParameter(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT));
        }
    };
}
