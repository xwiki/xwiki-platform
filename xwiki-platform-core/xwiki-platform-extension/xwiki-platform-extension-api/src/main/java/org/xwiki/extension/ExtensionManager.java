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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Main entry point for some extensions management tasks.
 * 
 * @version $Id$
 */
@ComponentRole
public interface ExtensionManager
{
    /**
     * Search the provided extension among all repositories including core and local repositories and for the provided
     * namespace. Null namespace mean installed in the whole farm.
     * <p>
     * The search is done in the following order:
     * <ul>
     * <li>Is it a core extension ?</li>
     * <li>Is it a local extension installed in the provided namespace ?</li>
     * <li>Is it a remote extension in one of the configured remote repositories ?</li>
     * </ul>
     * The first one found is returned.
     * 
     * @param extensionId the extension identifier
     * @param namespace the namespace where to resolve the extension
     * @return the resolved extension
     * @throws ResolveException error when trying to resolve extension
     */
    Extension resolveExtension(ExtensionId extensionId, String namespace) throws ResolveException;
}
