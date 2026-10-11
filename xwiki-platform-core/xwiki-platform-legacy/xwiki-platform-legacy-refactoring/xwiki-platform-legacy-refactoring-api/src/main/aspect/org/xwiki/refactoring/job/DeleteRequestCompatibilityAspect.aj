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
package org.xwiki.refactoring.job;

/**
 * Add a backward compatibility layer to the {@link DeleteRequest} class.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public privileged aspect DeleteRequestCompatibilityAspect
{
    declare parents : DeleteRequest implements CompatibilityDeleteRequestConstants;

    /**
     * @return {@code true} if the job should update the links that target the old entity reference (before the delete)
     *         from anywhere on the farm, {@code false} if the job should update only the links from the wiki where the
     *         entity was located before the delete
     * @deprecated not taken into account anymore
     */
    @Deprecated(since = "14.8RC1")
    public boolean DeleteRequest.isUpdateLinksOnFarm()
    {
        return true;
    }

    /**
     * Sets whether the job should update the links that target the old entity reference (before the delete) from
     * anywhere on the farm, or only from the wiki where the entity was located before the delete.
     *
     * @param updateLinksOnFarm {@code true} to update the links from anywhere on the farm, {@code false} to update only
     *            the links from the wiki where the entity is located
     * @deprecated not taken into account anymore
     */
    @Deprecated(since = "14.8RC1")
    public void DeleteRequest.setUpdateLinksOnFarm(boolean updateLinksOnFarm)
    {
        // Ignored
    }
}
