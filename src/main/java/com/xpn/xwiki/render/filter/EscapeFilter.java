/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This file was initally copied from Radeox. See below for the Radeox license and
 * copyright.
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
 *
 */

/*
 * This file is part of "SnipSnap Radeox Rendering Engine".
 *
 * Copyright (c) 2002 Stephan J. Schmidt, Matthias L. Jugel
 * All Rights Reserved.
 *
 * Please visit http://radeox.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * --LICENSE NOTICE--
 */
package com.xpn.xwiki.render.filter;

import org.radeox.filter.CacheFilter;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.LocaleRegexTokenFilter;
import org.radeox.regex.MatchResult;
import org.radeox.util.Encoder;

/*
 * Transforms multiple \ into single backspaces and escapes other characters.
 */
public class EscapeFilter extends LocaleRegexTokenFilter implements CacheFilter
{
    protected String getLocaleKey()
    {
        return "filter.escape";
    }

    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
        buffer.append(handleMatch(result, context));                    
    }

    public String handleMatch(MatchResult result, FilterContext context)
    {
        if (result.group(1) == null) {
            String match = result.group(2);
            if ("\\".equals(match)) {
                return "\\\\";
            }
            return Encoder.toEntity(match.charAt(0));
        } else {
            return "&#92;";
        }
    }
}
