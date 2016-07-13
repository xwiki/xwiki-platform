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
package org.xwiki.wiki.internal.descriptor.properties;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.properties.WikiPropertyGroupException;

/**
 * Component to load and save all property groups for a given descriptor.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Role
public interface WikiPropertyGroupManager
{
    /**
     * Load all property groups for the given descriptor.
     * @param descriptor descriptor to initialize
     * @throws WikiPropertyGroupException if problems occur
     */
    void loadForDescriptor(WikiDescriptor descriptor) throws WikiPropertyGroupException;

    /**
     * Save all property groups for the given descriptor.
     * @param descriptor descriptor to save
     * @throws WikiPropertyGroupException if problems occur
     */
    void saveForDescriptor(WikiDescriptor descriptor) throws WikiPropertyGroupException;
}
