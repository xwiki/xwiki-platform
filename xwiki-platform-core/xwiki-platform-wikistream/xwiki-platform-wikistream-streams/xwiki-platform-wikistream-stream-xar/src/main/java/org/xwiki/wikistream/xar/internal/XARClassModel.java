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

import org.xwiki.wikistream.filter.WikiClassFilter;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XARClassModel
{
    public static final String ELEMENT_CLASS = "class";

    public static final String ELEMENT_NAME = "name";

    public static final String ELEMENT_CUSTOMCLASS = "customClass";

    public static final String ELEMENT_CUSTOMMAPPING = "customMapping";

    public static final String ELEMENT_SHEET_DEFAULTVIEW = "defaultViewSheet";

    public static final String ELEMENT_SHEET_DEFAULTEDIT = "defaultEditSheet";

    public static final String ELEMENT_DEFAULTSPACE = "defaultWeb";

    public static final String ELEMENT_NAMEFIELD = "nameField";

    public static final String ELEMENT_VALIDATIONSCRIPT = "validationScript";

    // Utils

    public static final Map<String, String> XARTOEVENTPARAMETERS = new HashMap<String, String>()
    {
        {
            put(ELEMENT_NAME, null);
            put(ELEMENT_CUSTOMCLASS, WikiClassFilter.PARAMETER_CUSTOMCLASS);
            put(ELEMENT_CUSTOMMAPPING, WikiClassFilter.PARAMETER_CUSTOMMAPPING);
            put(ELEMENT_SHEET_DEFAULTVIEW, WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW);
            put(ELEMENT_SHEET_DEFAULTEDIT, WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT);
            put(ELEMENT_DEFAULTSPACE, WikiClassFilter.PARAMETER_DEFAULTSPACE);
            put(ELEMENT_NAMEFIELD, WikiClassFilter.PARAMETER_NAMEFIELD);
            put(ELEMENT_VALIDATIONSCRIPT, WikiClassFilter.PARAMETER_VALIDATIONSCRIPT);
        }
    };
}
