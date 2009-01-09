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
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

/**
 * Represents a head of a row or column of a table.
 * 
 * @version $Id$
 * @since 1.6M2
 */
public class TableHeadCellBlock extends TableCellBlock
{
    /**
     * @param list the list of children blocks of the table cell block.
     * @param parameters the parameters of the table head cell.
     */
    public TableHeadCellBlock(List<Block> list, Map<String, String> parameters)
    {
        super(list, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.TableCellBlock#before(org.xwiki.rendering.listener.Listener)
     */
    @Override
    public void before(Listener listener)
    {
        listener.beginTableHeadCell(getParameters());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.TableCellBlock#after(org.xwiki.rendering.listener.Listener)
     */
    @Override
    public void after(Listener listener)
    {
        listener.endTableHeadCell(getParameters());
    }
}
