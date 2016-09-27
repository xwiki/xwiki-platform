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
package com.xpn.xwiki.internal.render;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Helper component to extract blocks ({@link LinkBlock}, {@link MacroBlock}) that link to documents or spaces from a
 * given XDOM and to generically read and write the resources from and to these blocks.
 *
 * @version $Id$
 * @since 7.4.1
 * @since 8.0M1
 */
@Role
public interface LinkedResourceHelper
{
    /**
     * @param xdom the XDOM to extract from
     * @return the list of blocks that link to documents or spaces.
     */
    List<Block> getBlocks(XDOM xdom);

    /**
     * @param block the block to extract from
     * @return the resource reference string pointing to a document or space
     */
    String getResourceReferenceString(Block block);

    /**
     * @param block the block to write to
     * @param newReferenceString the new resource reference string that the block should point to
     */
    void setResourceReferenceString(Block block, String newReferenceString);

    /**
     * @param block the block to extract from
     * @return the type of the resource pointing to a document or space
     */
    ResourceType getResourceType(Block block);

    /**
     * @param block the block to write to
     * @param newResourceType the new type of resource that the block should point to
     */
    void setResourceType(Block block, ResourceType newResourceType);

    /**
     * @param block the block to extract from
     * @return the resource reference corresponding to the given block or {@code null} if none was found or if the block
     *         type is unsupported
     */
    ResourceReference getResourceReference(Block block);
}
