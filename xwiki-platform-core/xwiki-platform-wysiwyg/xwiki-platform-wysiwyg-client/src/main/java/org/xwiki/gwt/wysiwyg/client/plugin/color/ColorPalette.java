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
package org.xwiki.gwt.wysiwyg.client.plugin.color;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.Cell;

/**
 * A set of colors from which the user can choose one by clicking.
 * 
 * @version $Id$
 */
public class ColorPalette extends Composite implements HasSelectionHandlers<String>, ClickHandler
{
    /**
     * The selected cell in the color grid that make up the palette.
     */
    private ColorCell selectedCell;

    /**
     * The object used to get the hex code for a given color.
     */
    private final ColorConverter converter = new ColorConverter();

    /**
     * Creates a new color palette using the specified color codes to fill the color grid.
     * 
     * @param colors the array of color codes that are used to fill the color grid.
     * @param columnCount the maximum number of columns the color grid can have.
     */
    public ColorPalette(String[] colors, int columnCount)
    {
        Grid colorGrid = new Grid(1 + (colors.length - 1) / columnCount, Math.min(columnCount, colors.length));
        colorGrid.addStyleName("xColorPalette");
        colorGrid.setBorderWidth(0);
        colorGrid.setCellPadding(0);
        colorGrid.setCellSpacing(0);
        colorGrid.addClickHandler(this);

        for (int i = 0; i < colors.length; i++) {
            colorGrid.setWidget(i / columnCount, i % columnCount, new ColorCell(colors[i]));
        }

        initWidget(colorGrid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasSelectionHandlers#addSelectionHandler(SelectionHandler)
     */
    public HandlerRegistration addSelectionHandler(SelectionHandler<String> handler)
    {
        return addHandler(handler, SelectionEvent.getType());
    }

    /**
     * @return the color grid that makes up this palette
     */
    protected Grid getColorGrid()
    {
        return (Grid) getWidget();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == getColorGrid()) {
            Cell cell = getColorGrid().getCellForEvent(event);
            if (cell != null) {
                setSelectedCell((ColorCell) getColorGrid().getWidget(cell.getRowIndex(), cell.getCellIndex()));
                SelectionEvent.fire(this, getSelectedColor());
            }
        }
    }

    /**
     * Selects a color in the color grid.
     * 
     * @param cell the color cell to be selected
     */
    private void setSelectedCell(ColorCell cell)
    {
        if (selectedCell != cell) {
            if (selectedCell != null) {
                selectedCell.setSelected(false);
            }
            selectedCell = cell;
            if (selectedCell != null) {
                selectedCell.setSelected(true);
            }
        }
    }

    /**
     * @return the color code for the grid cell that was last clicked, or {@code null} if no cell has been selected
     */
    public String getSelectedColor()
    {
        return selectedCell != null ? selectedCell.getColor() : null;
    }

    /**
     * Selects a color in the color grid by its color code.
     * 
     * @param color the code of the color to be selected
     */
    public void setSelectedColor(String color)
    {
        if (color != null) {
            String hexColorCode = converter.convertToHex(color);
            for (int i = 0; i < getColorGrid().getRowCount(); i++) {
                for (int j = 0; j < getColorGrid().getColumnCount(); j++) {
                    ColorCell cell = (ColorCell) getColorGrid().getWidget(i, j);
                    if (color.equalsIgnoreCase(cell.getColor())
                        || (hexColorCode != null && hexColorCode.equalsIgnoreCase(cell.getHexColorCode()))) {
                        setSelectedCell(cell);
                        return;
                    }
                }
            }
        }
        setSelectedCell(null);
    }
}
