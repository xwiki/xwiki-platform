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
 * Time: 17:19:32
 */

package com.xpn.xwiki.render.macro;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.macro.Macro;

import java.util.*;

public class MacroRepository extends org.radeox.macro.PluginRepository {
  protected static Log log = LogFactory.getLog(MacroRepository.class);
  protected InitialRenderContext context;

  protected static MacroRepository instance;
  protected List loaders;

  public synchronized static com.xpn.xwiki.render.macro.MacroRepository getInstance() {
   if (null == instance) {
     instance = new com.xpn.xwiki.render.macro.MacroRepository();
   }
   return (com.xpn.xwiki.render.macro.MacroRepository)instance;
 }

  protected void initialize(InitialRenderContext context) {
    Iterator iterator = list.iterator();
    while (iterator.hasNext()) {
      Macro macro = (Macro) iterator.next();
      macro.setInitialContext(context);
    }
    init();
  }

  public void setInitialContext(InitialRenderContext context) {
    this.context = context;
    initialize(context);
  }

  protected void init() {
    Map newPlugins = new HashMap();

    Iterator iterator = list.iterator();
    while (iterator.hasNext()) {
      Macro macro = (Macro) iterator.next();
      newPlugins.put(macro.getName(), macro);
    }
    plugins = newPlugins;
  }

  /**
     * Loads macros from all loaders into plugins.
  */
  protected void load() {
      Iterator iterator = loaders.iterator();
      while (iterator.hasNext()) {
        MacroLoader loader = (MacroLoader) iterator.next();
        loader.setRepository(this);
        log.debug("Loading from: " + loader.getClass());
        loader.loadPlugins(this);
      }
  }

  protected MacroRepository() {
   loaders = new ArrayList();
   loaders.add(new MacroLoader());
   load();
 }
}
