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
package org.xwiki.extension.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;

/**
 * Basic handler used for tests.
 */
@Component("type")
public class TestExtensionHandler extends AbstractExtensionHandler
{
    private Map<String, Set<LocalExtension>> extensions = new HashMap<String, Set<LocalExtension>>();

    public Map<String, Set<LocalExtension>> getExtensions()
    {
        return this.extensions;
    }

    @Override
    public void install(LocalExtension localExtension, String namespace) throws InstallException
    {
        Set<LocalExtension> namespaceExtensions = this.extensions.get(namespace);
        if (namespaceExtensions == null) {
            namespaceExtensions = new HashSet<LocalExtension>();
            this.extensions.put(namespace, namespaceExtensions);
        }

        namespaceExtensions.add(localExtension);
    }

    @Override
    public void uninstall(LocalExtension localExtension, String namespace) throws UninstallException
    {
        this.extensions.get(namespace).remove(localExtension);
    }

    @Override
    public void initialize(LocalExtension localExtension, String namespace) throws ExtensionException
    {
        install(localExtension, namespace);
    }
}
