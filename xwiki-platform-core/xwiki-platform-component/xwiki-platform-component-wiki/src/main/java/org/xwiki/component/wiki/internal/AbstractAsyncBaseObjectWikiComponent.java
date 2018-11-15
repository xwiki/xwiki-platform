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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class helper to implement xobject based asynchronous wiki component.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractAsyncBaseObjectWikiComponent extends AbstractBaseObjectWikiComponent
{
    /**
     * Indicate if the asynchronous execution is allowed for this extension.
     */
    public static final String XPROPERTY_ASYNC_ENABLED = "async_enabled";

    /**
     * Indicate if caching is allowed for this UI extension.
     */
    public static final String XPROPERTY_ASYNC_CACHED = "async_cached";

    /**
     * Indicate the list of context elements required by the UI extension execution.
     */
    public static final String XPROPERTY_ASYNC_CONTEXT = "async_context";

    protected final boolean async;

    protected final boolean cached;

    protected final Set<String> contextEntries;

    /**
     * @param baseObject the object containing the component definition
     * @param roleType the role Type implemented
     * @param roleHint the role hint for this role implementation
     */
    public AbstractAsyncBaseObjectWikiComponent(BaseObject baseObject, Type roleType, String roleHint)
    {
        super(baseObject, roleType, roleHint);

        this.async = baseObject.getIntValue(XPROPERTY_ASYNC_ENABLED, 0) == 1;
        this.cached = baseObject.getIntValue(XPROPERTY_ASYNC_CACHED, 0) == 1;
        List<String> contextEntriesList = baseObject.getListValue(XPROPERTY_ASYNC_CONTEXT);
        this.contextEntries = contextEntriesList != null ? new HashSet<>(contextEntriesList) : null;
    }
}
