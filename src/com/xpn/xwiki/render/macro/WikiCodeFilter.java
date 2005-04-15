/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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

 * Created by
 * User: Ludovic Dubost
 * Date: 11 août 2004
 * Time: 23:46:59
 */
package com.xpn.xwiki.render.macro;

import org.radeox.macro.code.DefaultRegexCodeFormatter;
import org.radeox.macro.code.SourceCodeFormatter;
import org.radeox.filter.context.FilterContext;

public class WikiCodeFilter implements SourceCodeFormatter {

  public WikiCodeFilter() {
  }

  public String getName() {
    return "wiki";
  }

  public int getPriority() {
        return 0;
    }

  public String filter(String input, FilterContext context) {
      return input;
  }

}
