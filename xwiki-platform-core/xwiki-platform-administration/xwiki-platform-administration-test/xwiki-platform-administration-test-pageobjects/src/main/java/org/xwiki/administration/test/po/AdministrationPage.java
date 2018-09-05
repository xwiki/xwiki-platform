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

import org.xwiki.menu.test.po.MenuHomePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the main Administration Page.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class AdministrationPage extends ViewPage
{
    private AdministrationMenu menu = new AdministrationMenu();

    public static AdministrationPage gotoPage()
    {
        getUtil().gotoPage(getSpace(), getPage(), "admin");
        return new AdministrationPage();
    }

    /**
     * Redirects to the administration page of a specified space.
     *
     * @param spaceReference the space reference
     * @return the administration page of the specified space
     * @since 7.2M3
     */
    public static AdministrationPage gotoSpaceAdministrationPage(SpaceReference spaceReference)
    {
        DocumentReference documentReference = new DocumentReference("WebPreferences", spaceReference);
        getUtil().gotoPage(documentReference, "admin");

        return new AdministrationPage();
    }

    /**
     * Redirects to the administration page of a specified space.
     *
     * @param spaceReferenceString the string serialized space reference
     * @return the administration page of the specified space
     * @since 7.2M3
     */
    public static AdministrationPage gotoSpaceAdministrationPage(String spaceReferenceString)
    {
        SpaceReference spaceReference = new SpaceReference(getUtil().resolveSpaceReference(spaceReferenceString));
        return gotoSpaceAdministrationPage(spaceReference);
    }

    public static String getURL()
    {
        return getUtil().getURL(getSpace(), getPage());
    }

    public static String getSpace()
    {
        return "XWiki";
    }

    public static String getPage()
    {
        return "XWikiPreferences";
    }

    public LocalizationAdministrationSectionPage clickLocalizationSection()
    {
        this.menu.expandCategoryWithId("content").getSectionById("Localization").click();
        return new LocalizationAdministrationSectionPage();
    }

    public ImportAdministrationSectionPage clickImportSection()
    {
        this.menu.expandCategoryWithId("content").getSectionById("Import").click();
        return new ImportAdministrationSectionPage();
    }

    public AdministrationSectionPage clickRegistrationSection()
    {
        this.menu.expandCategoryWithId("usersgroups").getSectionById("Registration").click();
        return new AdministrationSectionPage("register");
    }

    public UsersAdministrationSectionPage clickUsersSection()
    {
        this.menu.expandCategoryWithId("usersgroups").getSectionById("Users").click();
        return new UsersAdministrationSectionPage();
    }

    public GlobalRightsAdministrationSectionPage clickGlobalRightsSection()
    {
        this.menu.expandCategoryWithId("usersgroups").getSectionById("Rights").click();
        return new GlobalRightsAdministrationSectionPage();
    }

    public AnnotationsPage clickAnnotationsSection()
    {
        this.menu.expandCategoryWithId("content").getSectionById("Annotations").click();
        return new AnnotationsPage();
    }

    public WYSIWYGEditorAdministrationSectionPage clickWYSIWYGEditorSection()
    {
        this.menu.expandCategoryWithId("edit").getSectionById("WYSIWYG").click();
        return new WYSIWYGEditorAdministrationSectionPage();
    }

    public MenuHomePage clickMenuSection()
    {
        this.menu.expandCategoryWithId("lf").getSectionById("menu.name").click();
        return new MenuHomePage();
    }

    /**
     * @since 9.2RC1
     */
    public ThemesAdministrationSectionPage clickThemesSection()
    {
        this.menu.expandCategoryWithId("lf").getSectionById("Themes").click();
        return new ThemesAdministrationSectionPage();
    }

    /**
     * @since 6.4M2
     */
    public ViewPage clickSection(String categoryName, String sectionName)
    {
        this.menu.expandCategoryWithName(categoryName).getSectionByName(categoryName, sectionName).click();
        return new ViewPage();
    }

    /**
     * @since 6.4M2
     */
    public boolean hasSection(String categoryName, String sectionName)
    {
        return this.menu.hasSectionWithName(categoryName, sectionName);
    }

    /**
     * @since 6.4M2
     */
    public boolean hasNotSection(String categoryName, String sectionName)
    {
        return this.menu.hasNotSectionWithName(categoryName, sectionName);
    }

    public boolean hasSection(String sectionId)
    {
        return this.menu.hasSectionWithId(sectionId);
    }

    public boolean hasNotSection(String sectionId)
    {
        return this.menu.hasNotSectionWithId(sectionId);
    }
}
