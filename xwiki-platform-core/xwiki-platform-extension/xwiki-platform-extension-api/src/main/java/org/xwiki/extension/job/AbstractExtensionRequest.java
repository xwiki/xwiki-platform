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
import java.util.Collection;

import org.xwiki.extension.ExtensionId;

/**
 * Base class for extension manipulation related {@link Request} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionRequest extends AbstractRequest implements ExtensionRequest
{
    /**
     * @see #getExtensions()
     */
    public static final String PROPERTY_EXTENSIONS = "extensions";

    /**
     * @see #getNamespaces()
     */
    public static final String PROPERTY_NAMESPACES = "namespaces";

    /**
     * Default constructor.
     */
    public AbstractExtensionRequest()
    {
        setProperty(PROPERTY_EXTENSIONS, new ArrayList<ExtensionId>());
    }

    /**
     * @param request the request to copy
     */
    public AbstractExtensionRequest(Request request)
    {
        super(request);

        Collection<ExtensionId> extensions = getExtensions();
        if (extensions == null) {
            setProperty(PROPERTY_EXTENSIONS, new ArrayList<ExtensionId>());
        }
    }

    @Override
    public Collection<ExtensionId> getExtensions()
    {
        return getProperty(PROPERTY_EXTENSIONS);
    }

    @Override
    public Collection<String> getNamespaces()
    {
        return getProperty(PROPERTY_NAMESPACES);
    }

    @Override
    public boolean hasNamespaces()
    {
        Collection<String> namespaces = getNamespaces();

        return namespaces != null && !namespaces.isEmpty();
    }

    /**
     * @param extensionId the extension identifier
     */
    public void addExtension(ExtensionId extensionId)
    {
        getExtensions().add(extensionId);
    }

    /**
     * @param namespace the namespace
     */
    public void addNamespace(String namespace)
    {
        Collection<String> namespaces = getNamespaces();

        if (namespaces == null) {
            namespaces = new ArrayList<String>();
            setProperty(PROPERTY_NAMESPACES, namespaces);
        }

        namespaces.add(namespace);
    }
}
