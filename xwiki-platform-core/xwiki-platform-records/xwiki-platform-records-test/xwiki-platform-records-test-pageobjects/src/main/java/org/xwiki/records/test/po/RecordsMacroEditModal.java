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
package org.xwiki.records.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;

/**
 * The macro editor dialog of the Records macro, which spreads its optional parameters over tabs.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class RecordsMacroEditModal extends MacroDialogEditModal
{
    private static final String FILTERS_PARAMETER = "filters";

    @Override
    public RecordsMacroEditModal waitUntilReady()
    {
        super.waitUntilReady();
        return this;
    }

    /**
     * Shows the parameters of one tab.
     *
     * @param label the label of the tab, as displayed
     * @return the current page object
     */
    public RecordsMacroEditModal openTab(String label)
    {
        WebElement tab = getDriver().findElement(By.xpath("//*[contains(@class, '-editor-modal')]"
            + "//ul[contains(@class, 'macro-tabs')]//a[normalize-space(.) = '" + label + "']"));
        tab.click();
        getDriver().waitUntilElementIsVisible(By.id(tab.getDomAttribute("aria-controls")));
        return this;
    }

    /**
     * Waits for the filters picker to be enhanced. The Filter &amp; sort tab must be the one shown.
     *
     * @return the filters picker
     */
    public SuggestInputElement getFiltersPicker()
    {
        return new SuggestInputElement(getMacroParameterInput(FILTERS_PARAMETER));
    }

    /**
     * @return the value the dialog saves for the filters parameter, as the picker builds it
     */
    public String getFilters()
    {
        return getMacroParameter(FILTERS_PARAMETER);
    }
}
