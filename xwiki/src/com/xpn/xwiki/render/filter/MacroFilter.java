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
 * Time: 17:12:39
 */

package com.xpn.xwiki.render.filter;

import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.macro.Repository;
import com.xpn.xwiki.render.macro.MacroRepository;

public class MacroFilter extends org.radeox.filter.MacroFilter {
 private MacroRepository macros;

 public void setInitialContext(InitialRenderContext context) {
   macros = MacroRepository.getInstance();
   macros.setInitialContext(context);
 }

 protected Repository getMacroRepository() {
   return macros;
 }
}
