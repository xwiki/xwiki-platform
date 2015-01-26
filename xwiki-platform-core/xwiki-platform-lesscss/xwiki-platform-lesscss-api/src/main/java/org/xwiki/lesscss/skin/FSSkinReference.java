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
package org.xwiki.lesscss.skin;

import org.xwiki.stability.Unstable;

/**
 * A specialized implementation of {@link SkinReference} for any skin stored in the filesystem.
 *
 * @version $Id$
 * @since 6.4M2
 */
@Unstable
public class FSSkinReference implements SkinReference
{
    private String skinName;

    /**
     * Construct a new reference to a filesystem skin.
     * @param skinName name of the skin on the filesystem
     */
    public FSSkinReference(String skinName)
    {
        this.skinName = skinName;
    }

    /**
     * @return the name of the skin on the filesystem
     */
    public String getSkinName()
    {
        return skinName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FSSkinReference) {
            FSSkinReference fsSkinReference = (FSSkinReference) o;
            return skinName.equals(fsSkinReference.skinName);
        }
        return false;
    };

    @Override
    public int hashCode() {
        return skinName.hashCode();
    }

    @Override
    public String toString()
    {
        return String.format("SkinFS[%s]", skinName);
    }

}
