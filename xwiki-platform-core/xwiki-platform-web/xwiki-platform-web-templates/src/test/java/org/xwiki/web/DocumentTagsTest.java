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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.skinx.internal.async.SkinExtensionAsync;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin;
import com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin;
import com.xpn.xwiki.plugin.tag.TagPlugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code documentTags.vm} template.
 *
 * @version $Id$
 */
@HTML50ComponentList
@SecurityScriptServiceComponentList
@ComponentList({
    // SKin Extensions so that $jsx.* and $ssx.* calls in the vm work.
    SkinExtensionAsync.class,
    TemplateScriptService.class
})
class DocumentTagsTest extends PageTest
{
    @BeforeEach
    void setUp() throws Exception
    {
        // Enable the Tag plugin
        this.oldcore.getSpyXWiki().getPluginManager().addPlugin("tag", TagPlugin.class.getName(),
            this.oldcore.getXWikiContext());

        // Enable the ssfx/jsfx plugins
        this.oldcore.getSpyXWiki().getPluginManager().addPlugin("ssfx", CssSkinFileExtensionPlugin.class.getName(),
            this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().getPluginManager().addPlugin("jsfx", JsSkinFileExtensionPlugin.class.getName(),
            this.oldcore.getXWikiContext());

        // Set up the current doc in the context so that $doc is bound in scripts
        DocumentReference documentReference = new DocumentReference("xwiki", "space", "page");
        XWikiDocument document = new XWikiDocument(documentReference);
        document.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(document, this.context);
        this.context.setDoc(document);
    }

    @Test
    void displayTagsWhenNoEditRightsAndTagPluginAvailableAndNoTags() throws Exception
    {
        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        // Remove extra spaces to make it easy to assert the result below.
        String result = templateManager.render("documentTags.vm").trim().replaceAll("\\s+", " ");

        // Verify that the generated HTML matches the expectations:
        // - The tag label is not displayed since there is/are no tag(s)
        // - No tag is listed after the tag label
        // - No "+" link is displayed since the user doesn't have edit rights
        assertEquals("<div class=\"doc-tags\" id=\"xdocTags\"> </div>", result);
    }

    @Test
    void displayTagsWhenEditRightsAndTagPluginAvailableAndNoTags() throws Exception
    {
        // Give edit rights ($hasEdit = true)
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT)).thenReturn(true);

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        // Remove extra spaces to make it easy to assert the result below.
        String result = templateManager.render("documentTags.vm").trim().replaceAll("\\s+", " ");

        // Verify that the generated HTML matches the expectations:
        // - The tag label is displayed
        // - No tag is listed after the tag label
        // - The "+" link is displayed since the user has edit rights
        assertThat(result, matchesPattern("\\Q<div class=\"doc-tags\" id=\"xdocTags\"> core.tags.list.label "
            + "<div class=\"tag-tool tag-add\"> <a href=\"/xwiki/bin/view/space/page?showTagAddForm=true#xdocTags\" title=\"core.tags.add.tooltip\" "
            + "rel=\"nofollow\">[+]</a> </div> </div>"));
    }

    @Test
    void displayTagsWhenNoEditRightsAndTagPluginAvailableAndTags() throws Exception
    {
        // Add tags to the current document
        XWikiDocument currentDocument = this.context.getDoc();
        BaseObject baseObject = new BaseObject();
        baseObject.setXClassReference(new DocumentReference("xwiki", "XWiki", "TagClass"));
        baseObject.setStringListValue("tags", Arrays.asList("tag1", "tag2"));
        currentDocument.addXObject(baseObject);
        this.xwiki.saveDocument(currentDocument, this.context);

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        // Remove extra spaces to make it easy to assert the result below.
        String result = templateManager.render("documentTags.vm").trim().replaceAll("\\s+", " ");

        // Verify that the generated HTML matches the expectations:
        // - The tag label is displayed
        // - The tags after the tag label
        // - No "+" link is displayed since the user doesn't have edit rights
        assertThat(result, matchesPattern("\\Q<div class=\"doc-tags\" id=\"xdocTags\"> core.tags.list.label "
            + "<span class=\"tag-wrapper\"> "
            + "<span class=\"tag\">"
                + "<a href=\"/xwiki/bin/view/Main/Tags?do=viewTag&amp;tag=tag1\">tag1</a></span> "
            + "</span> "
            + "<span class=\"tag-wrapper\"> "
            + "<span class=\"tag\">"
                + "<a href=\"/xwiki/bin/view/Main/Tags?do=viewTag&amp;tag=tag2\">tag2</a>"
            + "</span> "
            + "</span> "
            + "</div>"));
    }

    @Test
    void displayTagsWhenEditRightsAndTagPluginAvailableAndTags() throws Exception
    {
        // Give edit rights ($hasEdit = true)
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT)).thenReturn(true);

        // Add tags to the current document
        XWikiDocument currentDocument = this.context.getDoc();
        BaseObject baseObject = new BaseObject();
        baseObject.setXClassReference(new DocumentReference("xwiki", "XWiki", "TagClass"));
        baseObject.setStringListValue("tags", Arrays.asList("tag1", "tag2"));
        currentDocument.addXObject(baseObject);
        this.xwiki.saveDocument(currentDocument, this.context);

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        // Remove extra spaces to make it easy to assert the result below.
        String result = templateManager.render("documentTags.vm").trim().replaceAll("\\s+", " ");

        // Verify that the generated HTML matches the expectations:
        // - The tag label is displayed
        // - The tags after the tag label
        // - The "+" link is displayed since the user has edit rights
        assertThat(result, matchesPattern("\\Q<div class=\"doc-tags\" id=\"xdocTags\"> core.tags.list.label "
        + "<span class=\"tag-wrapper\"> <span class=\"tag\">"
        + "<a href=\"/xwiki/bin/view/Main/Tags?do=viewTag&amp;tag=tag1\">tag1</a></span> "
        + "<span class=\"separator\">[</span>"
        + "<a href=\"/xwiki/bin/view/space/page?xpage=documentTags&amp;xaction=delete&amp;tag=tag1&amp;form_token=&amp;xredirect=%2Fxwiki%2Fbin%2Fview%2Fspace%2Fpage%23xdocTags\" "
        + "class=\"tag-tool tag-delete\" title=\"core.tags.remove.tooltip\">X</a><span class=\"separator\">]</span></span> "
        + "<span class=\"tag-wrapper\"> <span class=\"tag\">"
        + "<a href=\"/xwiki/bin/view/Main/Tags?do=viewTag&amp;tag=tag2\">tag2</a></span> "
        + "<span class=\"separator\">[</span>"
        + "<a href=\"/xwiki/bin/view/space/page?xpage=documentTags&amp;xaction=delete&amp;tag=tag2&amp;form_token=&amp;xredirect=%2Fxwiki%2Fbin%2Fview%2Fspace%2Fpage%23xdocTags\" "
        + "class=\"tag-tool tag-delete\" title=\"core.tags.remove.tooltip\">X</a><span class=\"separator\">]</span></span> "
        + "<div class=\"tag-tool tag-add\"> <a href=\"/xwiki/bin/view/space/page?showTagAddForm=true#xdocTags\" "
        + "title=\"core.tags.add.tooltip\" rel=\"nofollow\">[+]</a> </div> </div>"));
    }
}
