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
package org.xwiki.extension.repository;

import java.util.Collection;
import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;

@ComponentRole
public interface LocalExtensionRepository extends ExtensionRepository
{
    Collection<LocalExtension> getLocalExtensions();

    Collection<LocalExtension> getInstalledExtensions(String namespace);

    Collection<LocalExtension> getInstalledExtensions();

    LocalExtension getInstalledExtension(String id, String namespace);

    LocalExtension installExtension(Extension extension, boolean dependency, String namespace) throws InstallException;

    void uninstallExtension(LocalExtension extension, String namespace) throws UninstallException;

    /**
     * Only look at the backward dependencies in the provided namespace. To get dependencies of a root extension
     * (namespace=null) use {@link #getBackwardDependencies(ExtensionId)} instead.
     */
    Collection<LocalExtension> getBackwardDependencies(String id, String namespace) throws ResolveException;

    /**
     * Get all backward dependencies by namespace for the provided extension.
     */
    Map<String, Collection<LocalExtension>> getBackwardDependencies(ExtensionId extensionId) throws ResolveException;
}
