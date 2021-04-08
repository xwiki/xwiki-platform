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
package org.xwiki.livedata;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * The component that provides the live data entries and their meta data.
 * 
 * @version $Id$
 * @since 12.10
 */
@Role
@Unstable
public interface LiveDataSource
{
    /**
     * @return the entry store, used to add, retrieve, update and delete live data entries
     */
    LiveDataEntryStore getEntries();

    /**
     * @return the property store, used to add, retrieve, update and delete live data properties
     */
    LiveDataPropertyDescriptorStore getProperties();
}
