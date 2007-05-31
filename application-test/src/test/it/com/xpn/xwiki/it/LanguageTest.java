/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.it;

/**
 * Verify the ability to change the wiki language.
 *
 * @version $Id: $
 * @todo refactor after creating the APIs for each skin so that we don't have to use getSelenium()
 *       at all
 */
public class LanguageTest extends AbstractAuthenticatedAdminTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();

        // Ensure the default language is English and that the wiki is in monolingual mode
        clickLinkWithLocator("headeradmin");

        // Note: We cannot use "label=No" as some tests below might have changed the language...
        getSelenium().select("XWiki.XWikiPreferences_0_multilingual", "value=0");
        
        getSelenium().type("XWiki.XWikiPreferences_0_default_language", "en");
        saveAndViewEdition();

        assertEquals("Log-out", getSelenium().getText("headerlogout"));
    }

    public void testChangeLanguageInMonolingualModeUsingTheAdministrationPreference()
    {
        clickLinkWithLocator("headeradmin");
        getSelenium().type("XWiki.XWikiPreferences_0_default_language", "fr");
        saveAndViewEdition();

        assertEquals("Quitter la session", getSelenium().getText("headerlogout"));
    }

    public void testVerifyPassingLanguageInRequestHasNotEffectInMonoligualMode()
    {
        open("/xwiki/bin/view/Main/?language=fr");

        assertEquals("Log-out", getSelenium().getText("headerlogout"));
    }

    public void testChangeLanguageInMultilingualModeUsingTheLanguageRequestParameter()
    {
        clickLinkWithLocator("headeradmin");
        getSelenium().select("XWiki.XWikiPreferences_0_multilingual", "value=1");
        saveAndViewEdition();
        open("/xwiki/bin/view/Main/?language=fr");

        assertEquals("Quitter la session", getSelenium().getText("headerlogout"));
    }
}
