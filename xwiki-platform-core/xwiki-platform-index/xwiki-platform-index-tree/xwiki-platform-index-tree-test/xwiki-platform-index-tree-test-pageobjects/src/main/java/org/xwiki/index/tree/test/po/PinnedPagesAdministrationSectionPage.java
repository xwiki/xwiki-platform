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
package org.xwiki.index.tree.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents the pinned pages administration section.
 *
 * @version $Id$
 * @since 16.10.10
 * @since 16.4.6
 */
public class PinnedPagesAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(id = "XWiki.PinnedChildPagesClass_0_pinnedChildPages")
    private WebElement pinnedPagesInput;

    /**
     * Default constructor.
     */
    public PinnedPagesAdministrationSectionPage()
    {
        super("index.tree.pinnedChildPages", true);
    }

    /**
     * @return the suggest input used to select the pinned pages
     */
    public SuggestInputElement getPinnedPagesPicker()
    {
        return new SuggestInputElement(this.pinnedPagesInput);
    }

    /**
     * Go to the pinned pages administration section for the given space.
     *
     * @param spaceReference the reference of the space for which to configure the pinned pages
     * @return the pinned pages administration section for the given space
     */
    public static PinnedPagesAdministrationSectionPage gotoPage(SpaceReference spaceReference)
    {
        gotoSpaceAdministration(spaceReference, "index.tree.pinnedChildPages");
        return new PinnedPagesAdministrationSectionPage();
    }
}
