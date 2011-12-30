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
package org.xwiki.extension.job;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.extension.ExtensionId;

/**
 * Base class for extension manipulation related {@link Request} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionRequest extends AbstractRequest implements ExtensionRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getExtensions()
     */
    private List<ExtensionId> extensions = new ArrayList<ExtensionId>();

    /**
     * @see #getNamespaces()
     */
    private List<String> namespaces;

    @Override
    public List<ExtensionId> getExtensions()
    {
        return this.extensions;
    }

    @Override
    public List<String> getNamespaces()
    {
        return this.namespaces;
    }

    @Override
    public boolean hasNamespaces()
    {
        return this.namespaces != null && !this.namespaces.isEmpty();
    }

    /**
     * @param extensionId the extension identifier
     */
    public void addExtension(ExtensionId extensionId)
    {
        this.extensions.add(extensionId);
    }

    /**
     * @param namespace the namespace
     */
    public void addNamespace(String namespace)
    {
        if (this.namespaces == null) {
            this.namespaces = new ArrayList<String>();
        }

        this.namespaces.add(namespace);
    }
}
