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
package org.xwiki.extension.task;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.extension.ExtensionId;

public class AbstractExtensionRequest extends AbstractRequest
{
    private List<ExtensionId> extensions = new ArrayList<ExtensionId>();

    private List<String> namespaces;

    public List<ExtensionId> getExtensions()
    {
        return this.extensions;
    }

    public List<String> getNamespaces()
    {
        return this.namespaces;
    }

    public boolean hasNamespaces()
    {
        return this.namespaces != null && !this.namespaces.isEmpty();
    }

    public boolean addExtension(ExtensionId extensionId)
    {
        return this.extensions.add(extensionId);
    }

    public boolean addNamespace(String namespace)
    {
        if (this.namespaces == null) {
            this.namespaces = new ArrayList<String>();
        }

        return this.namespaces.add(namespace);
    }
}
