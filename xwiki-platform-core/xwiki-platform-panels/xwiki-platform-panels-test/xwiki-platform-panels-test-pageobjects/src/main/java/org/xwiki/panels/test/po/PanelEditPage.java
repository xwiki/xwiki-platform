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
package org.xwiki.panels.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.InlinePage;

/**
 * Represents a panel in edit mode.
 * 
 * @version $Id$
 * @since 4.3.1
 */
public class PanelEditPage extends InlinePage
{
    /**
     * Panel default content format.
     */
    public static final String DEFAULT_CONTENT_FORMAT =
        "{{velocity}}\n#panelheader('%s')\n%s\n#panelfooter()\n{{/velocity}}";

    /**
     * The text area used to specify the panel content.
     */
    @FindBy(id = "Panels.PanelClass_0_content")
    private WebElement contentTextArea;

    /**
     * Sets the content of the panel.
     * 
     * @param content the panel content
     */
    public void setContent(String content)
    {
        contentTextArea.clear();
        contentTextArea.sendKeys(content);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected PanelViewPage createViewPage()
    {
        return new PanelViewPage();
    }
}
