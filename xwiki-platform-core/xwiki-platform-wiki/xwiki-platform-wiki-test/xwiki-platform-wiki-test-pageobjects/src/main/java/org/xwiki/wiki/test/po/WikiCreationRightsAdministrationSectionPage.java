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
package org.xwiki.wiki.test.po;

import org.xwiki.test.ui.po.EditRightsPane;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the Wikis &gt; Creation Right administration section of the main wiki, where the right to create wikis is
 * granted.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class WikiCreationRightsAdministrationSectionPage extends ViewPage
{
    /**
     * The name of the right granted in this section, as displayed in the header of its column.
     */
    public static final String CREATE_WIKI_RIGHT = "Create Wiki";

    private final EditRightsPane editRightsPane = new EditRightsPane();

    /**
     * Opens the Creation Right administration section of the main wiki.
     *
     * @return the page object of the section
     */
    public static WikiCreationRightsAdministrationSectionPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=wikis.rights");
        WikiCreationRightsAdministrationSectionPage page = new WikiCreationRightsAdministrationSectionPage();
        page.editRightsPane.getRightsTable().waitUntilReady();
        return page;
    }

    /**
     * @return the pane used to grant the right, to users or to groups
     */
    public EditRightsPane getEditRightsPane()
    {
        return this.editRightsPane;
    }
}
