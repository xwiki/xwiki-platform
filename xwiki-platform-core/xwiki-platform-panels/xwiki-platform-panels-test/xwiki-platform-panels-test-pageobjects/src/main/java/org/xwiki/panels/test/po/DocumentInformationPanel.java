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

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.Select;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object for the DocumentInformation panel that allows to switch syntax in a document.
 *
 * @version $Id$
 * @since 11.5RC1
 */
public class DocumentInformationPanel extends ViewPage
{
    private Select syntaxSelect;

    public DocumentInformationPanel()
    {
        this.syntaxSelect = new Select(getDriver().findElement(By.id("xwikidocsyntaxinput2")));
    }

    public List<String> getAvailableSyntaxes()
    {
        return this.syntaxSelect.getOptions().stream()
            .map(item -> item.getAttribute("value"))
            .collect(Collectors.toList());
    }

    public String getSelectedSyntax()
    {
        return this.syntaxSelect.getFirstSelectedOption().getAttribute("value");
    }

    public void selectSyntax(String syntax)
    {
        this.syntaxSelect.selectByValue(syntax);
    }
}
