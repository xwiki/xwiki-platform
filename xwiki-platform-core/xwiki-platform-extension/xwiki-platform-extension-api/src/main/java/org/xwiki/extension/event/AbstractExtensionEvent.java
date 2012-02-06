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
package org.xwiki.extension.event;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.ExtensionId;

/**
 * Base class for all {@link ExtensionEvent}.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionEvent implements ExtensionEvent
{
    /**
     * @see #getExtensionId()
     */
    private ExtensionId extensionId;

    /**
     * @see #getNamespace()
     */
    private String namespace;

    /**
     * Required since null namespace means root namespace.
     */
    private boolean noNamespace;

    /**
     * Default constructor.
     */
    public AbstractExtensionEvent()
    {
        this.noNamespace = true;
    }

    /**
     * @param extensionId the event related extension identifier
     * @param namespace the namespace on which the event happened
     */
    protected AbstractExtensionEvent(ExtensionId extensionId, String namespace)
    {
        this.extensionId = extensionId;
        this.namespace = namespace;
        this.noNamespace = false;
    }

    // ExtensionEvent

    @Override
    public ExtensionId getExtensionId()
    {
        return this.extensionId;
    }

    @Override
    public String getNamespace()
    {
        return this.namespace;
    }

    @Override
    public boolean hasNamespace()
    {
        return !this.noNamespace;
    }

    // Event

    @Override
    public boolean matches(Object event)
    {
        return this.getClass() == event.getClass()
            && matchesExtensionId(((AbstractExtensionEvent) event).getExtensionId())
            && matchesNamespace(((AbstractExtensionEvent) event).getNamespace());
    }

    /**
     * @param extensionId the event related extension identifier
     * @return <code>true</code> if the passed event matches this event, <code>false</code> otherwise.
     */
    private boolean matchesExtensionId(ExtensionId extensionId)
    {
        return this.extensionId == null || this.extensionId.equals(extensionId)
            || (this.extensionId.getVersion() == null && this.extensionId.getId().equals(extensionId.getId()));
    }

    /**
     * @param namespace the event related namespace
     * @return <code>true</code> if the passed event matches this event, <code>false</code> otherwise.
     */
    private boolean matchesNamespace(String namespace)
    {
        return this.noNamespace || StringUtils.equals(this.namespace, namespace);
    }

}
