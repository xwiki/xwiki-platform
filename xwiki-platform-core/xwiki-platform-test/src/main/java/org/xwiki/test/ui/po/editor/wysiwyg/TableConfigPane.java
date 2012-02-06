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
package org.xwiki.test.ui.po.editor.wysiwyg;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Models the table configuration wizard step that is accessible when inserting a table from the WYSIWYG content editor.
 * 
 * @version $Id$
 * @since 3.4RC1
 */
public class TableConfigPane extends WizardStepElement
{
    /**
     * The text input used to specify the number of table rows.
     */
    @FindBy(xpath = "//input[@title = 'Row count']")
    private WebElement rowCountInput;

    /**
     * The text input used to specify the number of table columns.
     */
    @FindBy(xpath = "//input[@title = 'Column count']")
    private WebElement columnCountInput;

    @Override
    public TableConfigPane waitToLoad()
    {
        super.waitToLoad();
        return this;
    }

    /**
     * @return the text input used to specify the number of table rows.
     */
    public WebElement getRowCountInput()
    {
        return rowCountInput;
    }

    /**
     * @return the text input used to specify the number of table columns.
     */
    public WebElement getColumnCountInput()
    {
        return columnCountInput;
    }
}
