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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.environment.Environment;
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

import org.xwiki.test.page.PageTest;
import org.xwiki.text.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code documentTags.vm} template.
 *
 * @version $Id$
 */
// TODO: This is the first page test really testing a vm template file. This is an experiment that is meant to be moved
// either in PageTest or better, in a TemplateTest class extending PageTest. The hard part is deciding how to find
// template files in non platform-web modules (probably needs a dep on the platform-web module and have it in the
// classpath).
@ComponentList({
    // Security SS
    SecurityScriptService.class,
    SecurityAuthorizationScriptService.class,
    RightConverter.class,
    // SKin Extensions
    SkinExtensionAsync.class
})
class DocumentTagsTest extends PageTest
{
    @BeforeEach
    void setUp() throws Exception
    {
        // Environment resources
        Environment environment = oldcore.getMocker().getInstance(Environment.class);
        when(environment.getResource(any(String.class))).thenAnswer(
            (Answer) invocation -> {
                String templateName = (String) invocation.getArguments()[0];
                // Try to load the resource from the CP first and if not found load it from src/main/webapp/templates
                // This is to support the skin.properties template resource coming from the PageTest module.
                URL url = getClass().getResource(templateName);
                if (url == null) {
                    String templatePath = getResourcePath(templateName);
                    url = new File(templatePath).toURI().toURL();
                }
                return url;
            });
        when(environment.getResourceAsStream(any(String.class))).thenAnswer(
            (Answer) invocation -> {
                String templateName = (String) invocation.getArguments()[0];
                // Try to load the resource from the CP first and if not found load it from src/main/webapp/templates
                // This is to support the skin.properties template resource coming from the PageTest module.
                InputStream is = getClass().getResourceAsStream(templateName);
                if (is == null) {
                    String templatePath = getResourcePath(templateName);
                    is = new FileInputStream(templatePath);
                }
                return is;
            });

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
        assertThat(result, matchesPattern("\\Q<div class=\"doc-tags\" id=\"xdocTags\"> core.tags.list.label </div>"));
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

    private String getResourcePath(String templateName)
    {
        // Extract the part after the /skins/flamingo/ and look for it in src/main/webapp/templates instead
        String suffix = StringUtils.substringAfter(templateName, "/skins/flamingo/");
        return String.format("src/main/webapp/templates/%s", suffix);
    }
}
