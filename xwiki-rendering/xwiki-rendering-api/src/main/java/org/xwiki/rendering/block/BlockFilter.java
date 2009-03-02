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
package org.xwiki.rendering.block;

import java.util.List;

/**
 * Filter provided block into one or more block.
 * <p>
 * See {@link AbstractBlock#clone(BlockFilter)} for an example of it's use.
 * 
 * @version $Id$
 */
public interface BlockFilter
{
    /**
     * Filter provided block into zero or more block.
     * 
     * @param block the block to filter.
     * @return should never be null. The filtered blocks or empty list.
     */
    List<Block> filter(Block block);
}
