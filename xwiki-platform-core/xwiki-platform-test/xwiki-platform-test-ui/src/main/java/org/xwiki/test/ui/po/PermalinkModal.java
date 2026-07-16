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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;

/**
 * Represents the "Permalink" modal shared by the standard Comments viewer and the annotation bubble, used to share a
 * direct link to a comment or annotation.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
public class PermalinkModal extends BaseModal
{
    public PermalinkModal()
    {
        super(By.id("permalinkModal"));
    }

    /**
     * @return the permalink value currently displayed in the modal's input field
     */
    public String getPermalinkValue()
    {
        return this.container.findElement(By.className("form-control")).getAttribute("value");
    }

    /**
     * Clicks the button that copies the displayed permalink to the clipboard.
     */
    public void clickCopyToClipboard()
    {
        this.container.findElement(By.id("permalink-copy-button")).click();
    }
}
