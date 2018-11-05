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
package org.xwiki.rendering.async.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.AsyncRendererCache;
import org.xwiki.script.service.ScriptService;

/**
 * A {@link ScriptService} to manipulate the asynchronous rendering features.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Singleton
@Named("async")
public class AsyncScriptService implements ScriptService
{
    @Inject
    private AsyncContext asyncContext;

    @Inject
    private AsyncRendererCache cache;

    /**
     * @return true if it's allowed to render content asynchronously
     */
    public boolean isEnabled()
    {
        return this.asyncContext.isEnabled();
    }

    /**
     * @param enabled true if it's allowed to render content asynchronously
     */
    public void setEnabled(boolean enabled)
    {
        this.asyncContext.setEnabled(enabled);
    }

    /**
     * Remove all entries from the asynchronous renderers cache.
     */
    public void flushCache()
    {
        this.cache.flush();
    }
}
