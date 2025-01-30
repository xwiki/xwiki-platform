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
package org.xwiki.administration.test.po;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.Select;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object for the Presentation section of the administration.
 *
 * @version $Id$
 * @since 16.4.7
 * @since 16.10.4
 * @since 17.1.0RC1
 */
public class PresentationAdministrationPage extends ViewPage
{
    /**
     * Go to the Presentation section of the administration.
     *
     * @return a {@link PresentationAdministrationPage} instance
     */
    public static PresentationAdministrationPage goToAdminSection()
    {
        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("editor", "globaladmin");
        queryParameters.put("section", "Presentation");
        DocumentReference reference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        getUtil().gotoPage(reference, "admin", queryParameters);
        return new PresentationAdministrationPage();
    }

    /**
     * Set the value of the show information field.
     *
     * @param value {@code No}, {@code Yes} or {@code ---}
     */
    public void setShowInformation(String value)
    {
        getShowInformationSelect().selectByVisibleText(value);
    }

    /**
     * Save the administration page.
     */
    public void save()
    {
        getDriver().findElement(By.cssSelector("#presentation input[value='Save']")).click();
    }

    /**
     * @return the text of the currently selected show information option
     */
    public String getShowInformation()
    {
        return getShowInformationSelect().getFirstSelectedOption().getText();
    }

    private Select getShowInformationSelect()
    {
        return new Select(getDriver().findElement(By.id("XWiki.XWikiPreferences_0_showinformation")));
    }
}
