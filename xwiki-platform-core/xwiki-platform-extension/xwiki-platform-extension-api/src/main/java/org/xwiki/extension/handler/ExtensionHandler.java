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
package org.xwiki.extension.handler;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;

/**
 * Handle extension related tasks depending of the type (install, uninstall, etc...).
 * 
 * @version $Id$
 */
@ComponentRole
public interface ExtensionHandler
{
    /**
     * Install the provided local extension.
     * 
     * @param localExtension the extension to install
     * @param namespace the namespace where to install the extension
     * @throws InstallException error when trying to install the extension
     */
    void install(LocalExtension localExtension, String namespace) throws InstallException;

    /**
     * Uninstall the provided local extension.
     * 
     * @param localExtension the extension to uninstall
     * @param namespace the namespace from where to uninstall the extension
     * @throws UninstallException error when trying to uninstall the extension
     */
    void uninstall(LocalExtension localExtension, String namespace) throws UninstallException;

    /**
     * Upgrade the provided local extension.
     * 
     * @param previousLocalExtension the previous installed version of the extension
     * @param newLocalExtension the extension to install
     * @param namespace the namespace from where to uninstall the extension
     * @throws InstallException error when trying to upgrade the extension
     */
    void upgrade(LocalExtension previousLocalExtension, LocalExtension newLocalExtension, String namespace)
        throws InstallException;
}
