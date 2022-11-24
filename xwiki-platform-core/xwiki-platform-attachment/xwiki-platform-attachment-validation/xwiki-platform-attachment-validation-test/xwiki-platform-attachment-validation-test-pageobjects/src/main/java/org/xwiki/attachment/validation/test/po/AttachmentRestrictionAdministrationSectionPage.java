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
package org.xwiki.attachment.validation.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object for the attachment restriction page administration.
 *
 * @version $Id$
 * @since 14.10
 */
public class AttachmentRestrictionAdministrationSectionPage extends ViewPage
{
    private AdministrationSectionPage attachmentsrestriction;

    private AttachmentRestrictionAdministrationSectionPage(AdministrationSectionPage attachmentsrestriction)
    {
        this.attachmentsrestriction = attachmentsrestriction;
    }

    /**
     * Go to the attachment restriction section of the administration for a given space.
     *
     * @param spaceReference the space reference to configure
     * @return a page object to manipulate the attachment restriction administration
     */
    public static AttachmentRestrictionAdministrationSectionPage goToPage(SpaceReference spaceReference)
    {
        AdministrationSectionPage attachmentsrestriction =
            AdministrationSectionPage.gotoSpaceAdministration(spaceReference, "attachmentsrestriction");

        return new AttachmentRestrictionAdministrationSectionPage(attachmentsrestriction);
    }

    /**
     * Define the allowed mimetypes.
     *
     * @param value the value of the allowed mimetypes (e.g., "image/png", "image/.*|text/plain")
     */
    public void setAllowedMimetypes(String value)
    {
        setValue(value, "XWiki.Attachment.Validation.Code.AttachmentMimetypeRestrictionClass_0_allowedMimetypes");
    }

    /**
     * Define the blocker mimetypes.
     *
     * @param value the value of the blocker mimetypes (e.g., "image/png", "image/.*|text/plain")
     */
    public void setBlockerMimetypes(String value)
    {
        setValue(value, "XWiki.Attachment.Validation.Code.AttachmentMimetypeRestrictionClass_0_blockedMimetypes");
    }

    /**
     * Save the updated configuration.
     */
    public void save()
    {
        this.attachmentsrestriction.clickSave();
        getDriver().waitUntilPageIsReloaded();
    }

    private void setValue(String value, String id)
    {
        WebElement element = getDriver().findElement(By.id(id));
        element.clear();
        element.sendKeys(value);
    }
}
