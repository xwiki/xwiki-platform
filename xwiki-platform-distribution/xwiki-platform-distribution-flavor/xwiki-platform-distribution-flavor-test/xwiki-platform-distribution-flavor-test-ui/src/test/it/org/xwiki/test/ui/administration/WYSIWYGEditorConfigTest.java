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
package org.xwiki.test.ui.administration;

import org.junit.Assert;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.WYSIWYGEditorAdministrationSectionPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;

/**
 * Test the WYSIWYG Editor administration section.
 * 
 * @version $Id$
 * @since 3.3M2
 */
public class WYSIWYGEditorConfigTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    /**
     * The WYSIWYG Editor administration section.
     */
    private WYSIWYGEditorAdministrationSectionPage wysiwygSection;

    @Before
    public void setUp() throws Exception
    {
        wysiwygSection = AdministrationPage.gotoPage().clickWYSIWYGEditorSection();
    }

    /**
     * Try to enable a dummy WYSIWYG editor plugin from the administration.
     * 
     * @since 3.3M2
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    public void testEnablePlugin()
    {
        String pluginName = RandomStringUtils.randomAlphabetic(5);
        Assert.assertFalse(wysiwygSection.getEnabledPlugins().contains(pluginName));
        wysiwygSection.enablePlugin(pluginName);
        wysiwygSection.clickSave();
        // Reload the administration section.
        getDriver().navigate().refresh();
        wysiwygSection = new WYSIWYGEditorAdministrationSectionPage();
        Assert.assertTrue(wysiwygSection.getEnabledPlugins().contains(pluginName));
    }
}
