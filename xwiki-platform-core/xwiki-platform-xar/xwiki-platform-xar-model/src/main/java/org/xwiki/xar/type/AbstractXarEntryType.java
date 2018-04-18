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
package org.xwiki.xar.type;

import org.xwiki.stability.Unstable;
import org.xwiki.xar.XarEntryType;

/**
 * Base class helper to implement {@link XarEntryType}.
 * 
 * @version $Id$
 * @since 10.3
 */
@Unstable
public abstract class AbstractXarEntryType implements XarEntryType
{
    private String name;

    private boolean editAllowed;

    private boolean deleteAllowed;

    private UpgradeType upgradeType = UpgradeType.THREEWAYS;

    /**
     * @param name the name of the type
     */
    public AbstractXarEntryType(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public boolean isEditAllowed()
    {
        return this.editAllowed;
    }

    /**
     * @param editAllowed true if editing this document is allowed
     */
    public void setEditAllowed(boolean editAllowed)
    {
        this.editAllowed = editAllowed;
    }

    @Override
    public boolean isDeleteAllowed()
    {
        return this.deleteAllowed;
    }

    /**
     * @param deleteAllowed true if deleting this document is allowed
     */
    public void setDeleteAllowed(boolean deleteAllowed)
    {
        this.deleteAllowed = deleteAllowed;
    }

    @Override
    public UpgradeType getUpgradeType()
    {
        return this.upgradeType;
    }

    /**
     * @param upgradeType the upgrade behavior
     */
    public void setUpgradeType(UpgradeType upgradeType)
    {
        this.upgradeType = upgradeType;
    }
}
