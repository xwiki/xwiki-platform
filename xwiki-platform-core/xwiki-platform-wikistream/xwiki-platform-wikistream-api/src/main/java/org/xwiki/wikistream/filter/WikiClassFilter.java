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
package org.xwiki.wikistream.filter;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;

/**
 * Class related events.
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public interface WikiClassFilter
{
    public static final String PARAMETER_CUSTOMCLASS = "customclass";

    public static final String PARAMETER_CUSTOMMAPPING = "custommapping";

    public static final String PARAMETER_SHEET_DEFAULTVIEW = "sheet_defaultview";

    public static final String PARAMETER_SHEET_DEFAULTEDIT = "sheet_defaultedit";

    public static final String PARAMETER_DEFAULTSPACE = "defaultspace";

    public static final String PARAMETER_NAMEFIELD = "namefield";

    public static final String PARAMETER_VALIDATIONSCRIPT = "validationscript";

    void beginWikiClass(
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;

    void endWikiClass(
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws WikiStreamException;
}
