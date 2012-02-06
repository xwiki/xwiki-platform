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
package org.xwiki.extension;

import org.xwiki.extension.repository.LocalExtensionRepository;

public class TestResources
{
    // Core

    public static final ExtensionId CORE_ID = new ExtensionId("coreextension", "version");

    // Installed

    public static final ExtensionId INSTALLED_ID = new ExtensionId("installedextension", "version");

    public static final ExtensionId INSTALLED_DEPENDENCY_ID = new ExtensionId("installedextensiondependency", "version");

    public LocalExtension installed;

    public LocalExtension installedDependency;

    // Remote

    public static final ExtensionId REMOTE_SIMPLE_ID = new ExtensionId("rsimple", "version");

    public static final ExtensionId REMOTE_WITHRDEPENDENCY_ID = new ExtensionId("rwithrdependency", "version");

    public static final ExtensionId REMOTE_WITHCDEPENDENCY_ID = new ExtensionId("rwithcdependency", "version");

    public static final ExtensionId REMOTE_WITHLDEPENDENCY_ID = new ExtensionId("rwithldependency", "version");

    public static final ExtensionId REMOTE_WITHRANDCDEPENDENCIES_ID = new ExtensionId("rwithrandcdependencies", "version");

    // Methods

    public void init(LocalExtensionRepository localExtensionRepository) throws ResolveException
    {
        this.installed = (LocalExtension) localExtensionRepository.resolve(INSTALLED_ID);
        this.installedDependency = (LocalExtension) localExtensionRepository.resolve(INSTALLED_DEPENDENCY_ID);
    }
}
