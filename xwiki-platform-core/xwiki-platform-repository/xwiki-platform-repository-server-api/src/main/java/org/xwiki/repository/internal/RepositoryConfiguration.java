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
package org.xwiki.repository.internal;

import java.util.List;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

/**
 * Contains various configuration of the repository.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Role
public interface RepositoryConfiguration
{
    /**
     * @return the default prefix used when generating the extension id
     * @throws XWikiException when failing to access the configuration
     */
    String getDefaultIdPrefix() throws XWikiException;

    /**
     * @return the valid types
     * @throws XWikiException when failing to access the configuration
     */
    List<String> getValidTypes() throws XWikiException;

    /**
     * @param type the type to validate
     * @return true of the type is valid, false otherwise
     * @throws XWikiException when failing to access the configuration
     */
    boolean isValidType(String type) throws XWikiException;
}
