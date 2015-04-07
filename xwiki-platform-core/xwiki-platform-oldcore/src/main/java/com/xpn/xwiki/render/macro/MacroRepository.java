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
package com.xpn.xwiki.render.macro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.macro.Macro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See {@link MacroLoader} for why we have copied this class from Radeox.
 */
public class MacroRepository extends org.radeox.macro.PluginRepository
{
    protected static Logger LOGGER = LoggerFactory.getLogger(MacroRepository.class);

    protected InitialRenderContext context;

    protected static MacroRepository instance;

    protected List loaders;

    public synchronized static MacroRepository getInstance()
    {
        if (null == instance) {
            instance = new com.xpn.xwiki.render.macro.MacroRepository();
        }
        return instance;
    }

    protected void initialize(InitialRenderContext context)
    {
        Iterator iterator = this.list.iterator();
        while (iterator.hasNext()) {
            Macro macro = (Macro) iterator.next();
            macro.setInitialContext(context);
        }
        init();
    }

    public void setInitialContext(InitialRenderContext context)
    {
        this.context = context;
        initialize(context);
    }

    protected void init()
    {
        Map newPlugins = new HashMap();

        Iterator iterator = this.list.iterator();
        while (iterator.hasNext()) {
            Macro macro = (Macro) iterator.next();
            newPlugins.put(macro.getName(), macro);
        }
        this.plugins = newPlugins;
    }

    /**
     * Loads macros from all loaders into plugins.
     */
    protected void load()
    {
        Iterator iterator = this.loaders.iterator();
        while (iterator.hasNext()) {
            MacroLoader loader = (MacroLoader) iterator.next();
            loader.setRepository(this);
            LOGGER.debug("Loading from: " + loader.getClass());
            loader.loadPlugins(this);
        }
    }

    protected MacroRepository()
    {
        this.loaders = new ArrayList();
        this.loaders.add(new MacroLoader());
        load();
    }
}
