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
package org.xwiki.rendering.internal.macro.dashboard;

import org.xwiki.rendering.macro.dashboard.Gadget;

/**
 * Specialized gadget that interprets its position as a pair of numbers, the first being the index of the column, and
 * the second being the index of the gadget inside the column.
 *
 * @version $Id$
 * @since 3.0M3
 */
class ColumnGadget extends Gadget
{
    /**
     * The index of the column of this gadget.
     */
    private Integer column;

    /**
     * The index of this gadget inside its column.
     */
    private Integer index;

    /**
     * Creates a column gadget which is a copy of the passed gadget.
     *
     * @param gadget the gadget to copy into a column gadget.
     */
    ColumnGadget(Gadget gadget)
    {
        super(gadget.getId(), gadget.getTitle(), gadget.getContent(), gadget.getPosition());
        this.setTitleSource(gadget.getTitleSource());
    }

    /**
     * @return the column
     */
    public Integer getColumn()
    {
        return this.column;
    }

    /**
     * @return the index
     */
    public Integer getIndex()
    {
        return this.index;
    }

    @Override
    public void setPosition(String position)
    {
        super.setPosition(position);

        // parse the position as a "container, index" pair and store the container number and index. <br>
        // TODO: move this code in an API class since the comma separated format is more generic than the columns layout
        // split by comma, first position is column, second position is index
        String[] split = position.split(",");
        try {
            this.column = Integer.valueOf(split[0].trim());
            this.index = Integer.valueOf(split[1].trim());
        } catch (ArrayIndexOutOfBoundsException e) {
            // nothing, just leave column and index null. Not layoutable in columns
        } catch (NumberFormatException e) {
            // same, nothing, just leave column and index null. Not layoutable in columns
        }
    }
}
