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

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;

/**
 * A repository containing extensions.
 * 
 * @version $Id$
 */
public interface ExtensionRepository
{
    /**
     * @return the repository identifier.
     */
    ExtensionRepositoryId getId();

    /**
     * Return extension descriptor from the repository. If the extension can't be found a {@link ResolveException} is
     * thrown.
     * 
     * @param extensionId the extension identifier
     * @return the found extension descriptor
     * @throws ResolveException failed to find extension in the repository
     */
    Extension resolve(ExtensionId extensionId) throws ResolveException;

    /**
     * @param extensionId the extension identifier
     * @return true if the extension exists in the repository
     */
    boolean exists(ExtensionId extensionId);
}
