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
package com.xpn.xwiki.internal.render;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.render.XWikiRenderingEngine;

/**
 * Wiki based initialization of the rendering engine.
 * 
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Singleton
@Named(RenderingEngineInitializerListener.NAME)
public class RenderingEngineInitializerListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "com.xpn.xwiki.internal.render.RenderingEngineInitializerListener";

    @Inject
    private Provider<XWikiRenderingEngine> engineProvider;

    /**
     * Default constructor.
     */
    public RenderingEngineInitializerListener()
    {
        super(NAME, new WikiReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.engineProvider.get().virtualInit((XWikiContext) data);
    }
}
