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
package org.xwiki.release.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the Release.WebHome page.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class ReleaseHomePage extends ViewPage
{
    /**
     * The release application space.
     */
    public static final String RELEASE_SPACE = "Release";

    /**
     * The release application page withing {@link #RELEASE_SPACE}.
     */
    public static final String RELEASE_PAGE = "WebHome";

    @FindBy(id = "versionid")
    private WebElement releaseNameField;

    @FindBy(xpath = "//form[@id = 'newrelease']//input[@class = 'button']")
    private WebElement releaseNameButton;

    /**
     * Opens the home page.
     *
     * @return a release application home page object
     */
    public static ReleaseHomePage gotoPage()
    {
        getUtil().gotoPage(RELEASE_SPACE, RELEASE_PAGE);
        return new ReleaseHomePage();
    }

    /**
     * @param releaseName the name of the Release entry to add
     * @return the new FAQ entry page
     */
    public ReleaseEntryEditPage addRelease(String releaseName)
    {
        this.releaseNameField.clear();
        this.releaseNameField.sendKeys(releaseName);
        this.releaseNameButton.click();
        return new ReleaseEntryEditPage();
    }

    /**
     * @return the Release live table element
     */
    public LiveDataElement getReleaseLiveData()
    {
        return new LiveDataElement("releases");
    }
}
