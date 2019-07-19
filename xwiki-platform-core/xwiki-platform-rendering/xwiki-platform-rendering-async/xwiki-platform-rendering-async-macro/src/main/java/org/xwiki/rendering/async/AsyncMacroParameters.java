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
package org.xwiki.rendering.async;

import java.util.List;
import java.util.Set;

/**
 * Parameters for the {@link org.org.xwiki.rendering.async.internal.AsyncMacro} Macro.
 * 
 * @version $Id$
 * @since 11.6RC1
 */
public class AsyncMacroParameters
{
    private boolean async = true;

    private boolean cached;

    private Set<String> contextEntries;

    private List<String> id;

    /**
     * @return true if the content should be executed asynchronously
     */
    public boolean isAsync()
    {
        return this.async;
    }

    /**
     * @param async true if the content should be executed asynchronously
     */
    public void setAsync(boolean async)
    {
        this.async = async;
    }

    /**
     * @return true if the result of the execution should be cached
     */
    public boolean isCached()
    {
        return this.cached;
    }

    /**
     * @param cached true if the result of the execution should be cached
     */
    public void setCached(boolean cached)
    {
        this.cached = cached;
    }

    /**
     * @return the context entries needed to execute the content
     */
    public Set<String> getContextEntries()
    {
        return this.contextEntries;
    }

    /**
     * @param contextEntries the context entries needed to execute the content
     */
    public void setContextEntries(Set<String> contextEntries)
    {
        this.contextEntries = contextEntries;
    }

    /**
     * @return the id to use instead of the generated one
     */
    public List<String> getId()
    {
        return this.id;
    }

    /**
     * @param id the id to use instead of the generated one
     */
    public void setId(List<String> id)
    {
        this.id = id;
    }
}
