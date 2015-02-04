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
package org.xwiki.skin;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 7.0M1
 */
@Role
@Unstable
public interface SkinManager
{
    /**
     * @param id the id of the skin
     * @return the skin with the provided id
     */
    Skin getSkin(String id);

    /**
     * Return the current skin.
     * 
     * @param testRights true if it should make sure to return a skin accessible to current user
     * @return the skin
     */
    Skin getCurrentSkin(boolean testRights);

    /**
     * Return the default skin.
     * 
     * @return the default skin
     */
    Skin getDefaultSkin();

    /**
     * Return the default skin to use as parent for other skins.
     * 
     * @return the default parent skin
     */
    Skin getDefaultParentSkin();
}
