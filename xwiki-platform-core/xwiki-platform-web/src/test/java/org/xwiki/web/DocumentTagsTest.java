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
package org.xwiki.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.script.SecurityAuthorizationScriptService;
import org.xwiki.security.authorization.script.internal.RightConverter;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.skinx.internal.async.SkinExtensionAsync;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin;
import com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin;
import com.xpn.xwiki.plugin.tag.TagPlugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code documentTags.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    // Security SS so that $service.security.* calls in the vm work and their behavior controlled.
    SecurityScriptService.class,
    SecurityAuthorizationScriptService.class,
    RightConverter.class,
    // SKin Extensions so that $jsx.* and $ssx.* calls in the vm work.
    SkinExtensionAsync.class
})
class DocumentTagsTest extends TemplateTest
{
    @BeforeEach
    void setUp()
    {
        // Enable the Tag plugin
        oldcore.getSpyXWiki().getPluginManager().addPlugin("tag", TagPlugin.class.getName(), oldcore.getXWikiContext());

        // Enable the ssfx/jsfx plugins
        oldcore.getSpyXWiki().getPluginManager().addPlugin("ssfx", CssSkinFileExtensionPlugin.class.getName(),
            oldcore.getXWikiContext());
        oldcore.getSpyXWiki().getPluginManager().addPlugin("jsfx", JsSkinFileExtensionPlugin.class.getName(),
            oldcore.getXWikiContext());
    }

    @Test
    void displayTagsWhenNoEditRightsAndTagPluginAvailableAndNoTags() throws Exception
    {
        TemplateManager templateManager = oldcore.getMocker().getInstance(TemplateManager.class);
        String result = templateManager.render("documentTags.vm").trim().replaceAll("\\s+", " ");

        // Verify that the generated HTML matches the expectation:
        // - The tag label is displayed
        // - No tag is listed after the tag label
        // - No "+" link is displayed since the user doesn't have edit rights
        assertThat(result, matchesPattern("\\Q<div class=\"doc-tags tag-visibility\" id=\"xdocTags\"> core.tags.list.label </div>"));
    }

    @Test
    void displayTagsWhenEditRightsAndTagPluginAvailableAndNoTags() throws Exception
    {
        // Give edit rights ($hasEdit = true)
        when(oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT)).thenReturn(true);

        TemplateManager templateManager = oldcore.getMocker().getInstance(TemplateManager.class);
        String result = templateManager.render("documentTags.vm").trim().replaceAll("\\s+", " ");

        // Verify that the generated HTML matches the expectation:
        // - The tag label is displayed
        // - No tag is listed after the tag label
        // - The "+" link is displayed since the user has edit rights
        assertThat(result, matchesPattern("\\Q<div class=\"doc-tags\" id=\"xdocTags\"> core.tags.list.label "
            + "<div class=\"tag-tool tag-add\"><a href=\"\\E.*\"\\Q title=\"core.tags.add.tooltip\" "
            + "rel=\"nofollow\">[+]</a></div> </div>\\E"));
    }
}
