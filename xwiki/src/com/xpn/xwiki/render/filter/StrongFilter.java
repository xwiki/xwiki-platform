/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 17 mars 2004
 * Time: 15:26:57
 */

package com.xpn.xwiki.render.filter;

import org.radeox.filter.regex.LocaleRegexReplaceFilter;
import org.radeox.filter.CacheFilter;

public class StrongFilter extends LocaleRegexReplaceFilter implements CacheFilter {
      protected String getLocaleKey() {
        return "filter.strong";
      }
    }
