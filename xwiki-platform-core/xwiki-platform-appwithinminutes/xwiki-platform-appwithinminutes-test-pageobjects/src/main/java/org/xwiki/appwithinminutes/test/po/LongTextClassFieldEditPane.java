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
package org.xwiki.appwithinminutes.test.po;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents the pane used to edit a 'Long Text' class field.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class LongTextClassFieldEditPane extends ClassFieldEditPane
{
    /**
     * Creates a new instance.
     * 
     * @param fieldName the name of the long text field
     */
    public LongTextClassFieldEditPane(String fieldName)
    {
        super(fieldName);
    }

    /**
     * Sets the number of rows.
     * 
     * @param rows the new number of rows
     */
    public void setRows(int rows)
    {
        WebElement rowsInput = getPropertyInput("rows");
        rowsInput.clear();
        rowsInput.sendKeys(String.valueOf(rows));
    }

    /**
     * Sets the editor to be used to edit the value of the long text field.
     * 
     * @param editor the editor to be used to edit the value of this field
     */
    public void setEditor(String editor)
    {
        Select editorSelect = new Select(getPropertyInput("editor"));
        editorSelect.selectByVisibleText(editor);
    }

    /**
     * @return the value of the "rows" attribute on the text area used to input the default field value
     */
    public int getPreviewRows()
    {
        String rows = getDefaultValueInput().getAttribute("rows");
        return StringUtils.isEmpty(rows) ? -1 : Integer.parseInt(rows);
    }
}
