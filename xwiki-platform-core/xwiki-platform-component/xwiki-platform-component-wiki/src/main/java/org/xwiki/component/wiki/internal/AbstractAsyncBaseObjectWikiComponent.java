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

import org.xwiki.component.wiki.WikiComponentException;

import com.xpn.xwiki.internal.mandatory.AbstractAsyncClassDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class helper to implement xobject based asynchronous wiki component.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractAsyncBaseObjectWikiComponent extends AbstractBaseObjectWikiComponent
{
    protected boolean asyncAllowed;

    protected boolean cacheAllowed;

    protected Set<String> contextEntries;

    @Override
    protected void initialize(BaseObject baseObject, Type roleType, String roleHint) throws WikiComponentException
    {
        super.initialize(baseObject, roleType, roleHint);

        this.asyncAllowed =
            baseObject.getIntValue(AbstractAsyncClassDocumentInitializer.XPROPERTY_ASYNC_ENABLED, 0) == 1;
        this.cacheAllowed =
            baseObject.getIntValue(AbstractAsyncClassDocumentInitializer.XPROPERTY_ASYNC_CACHED, 0) == 1;
        List<String> contextEntriesList =
            baseObject.getListValue(AbstractAsyncClassDocumentInitializer.XPROPERTY_ASYNC_CONTEXT);
        this.contextEntries = contextEntriesList != null ? new HashSet<>(contextEntriesList) : null;
    }
}
