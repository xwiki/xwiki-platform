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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.skinx.internal.async.SkinExtensionAsync;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarPackage;
import org.xwiki.xar.script.XarScriptService;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin;
import com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

/**
 * Test of template {@code importinline.vm}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 * @since 14.4.8
 * @since 13.10.11
 */
@ComponentList({
    SkinExtensionAsync.class,
    ModelScriptService.class,
    TemplateScriptService.class
})
class ImportInlinePageTest extends PageTest
{
    private TemplateManager templateManager;

    @Mock
    private XarScriptService xarScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        // Enable the ssfx/jsfx plugins.
        XWikiPluginManager pluginManager = this.oldcore.getSpyXWiki().getPluginManager();
        pluginManager.addPlugin("ssfx", CssSkinFileExtensionPlugin.class.getName(), this.context);
        pluginManager.addPlugin("jsfx", JsSkinFileExtensionPlugin.class.getName(), this.context);
        // Initialize XWiki.XWikiPreferences 
        XWikiDocument xWikiPreferencesDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"), this.context);
        this.xwiki.saveDocument(xWikiPreferencesDocument, this.context);
        // Initialize the current document
        XWikiDocument currentDocument = this.xwiki.getDocument(new DocumentReference("xwiki", "Doc", "Space"
            + "\"'/><script>console.log('docTitle'</script>{{html}}{{noscript/}}"), this.context);
        currentDocument.setAttachment("attachment.xar", new ByteArrayInputStream("xar content".getBytes()),
            this.context);
        currentDocument.setAttachment("\"'/><script>console.log('secondAttachment')</script>{{/html}}{{noscript/}}",
            new ByteArrayInputStream("file content".getBytes()), this.context);
        this.xwiki.saveDocument(currentDocument, this.context);
        this.context.setDoc(currentDocument);
        this.componentManager.registerComponent(ScriptService.class, "xar", this.xarScriptService);
    }

    @Test
    void escape() throws Exception
    {
        String packageAuthor = "\"'><script>console.log('package author');</script>{{/html}}{{noscript/}}";
        String packageName = "\"'><script>console.log('package name');</script>{{/html}}{{noscript/}}";
        String packageLicense = "\"'><script>console.log('package licence');</script>{{/html}}{{noscript/}}";
        String spaceName = "\"'><script>console.log('space name');</script>{{/html}}{{noscript/}}";
        String pageName = "\"'><script>console.log('page name');</script>{{/html}}{{noscript/}}";
        LocalDocumentReference reference = new LocalDocumentReference(spaceName, pageName, Locale.FRANCE);
        XarEntry xarEntry = new XarEntry(reference);

        XarPackage xarPackage = new XarPackage(singletonList(xarEntry));
        xarPackage.setPackageAuthor(packageAuthor);
        xarPackage.setPackageName(packageName);
        xarPackage.setPackageDescription("packageDescription");
        xarPackage.setPackageLicense(packageLicense);
        xarPackage.setPackageExtensionId("packageExtensionId");

        when(this.xarScriptService.getXarPackage(any(InputStream.class), anyBoolean())).thenReturn(xarPackage);
        this.request.put("file", "attachment.xar");
        Document document = Jsoup.parse(this.templateManager.render("importinline.vm"));

        assertEquals("/xwiki/bin/upload/Doc/Space%22%27%2F%3E%3Cscript%3Econsole.log%28%27docTitle"
                + "%27%3C%2Fscript%3E%7B%7Bhtml%7D%7D%7B%7Bnoscript%2F%7D%7D",
            document.selectFirst("form").attr("action"));
        Element packageinfos = document.selectFirst(".packageinfos");
        assertEquals(packageAuthor, packageinfos.selectFirst(".author").text());
        assertEquals(packageName, packageinfos.selectFirst(".name").text());
        assertEquals(packageLicense, packageinfos.selectFirst(".licence").text());
        Element packageDiv = document.getElementById("package");
        assertEquals(spaceName, packageDiv.selectFirst(".spacename").text());
        Element xitemLi = packageDiv.selectFirst(".xitem");
        assertEquals("\"'><script>console\\.log('space name');</script>{{/html}}{{noscript/}}.\"'>"
                + "<script>console\\.log('page name');</script>{{/html}}{{noscript/}}:fr_FR",
            xitemLi.selectFirst("input[type='hidden']").attr("name"));
        assertEquals("\"'><script>console.log('space name');</script>{{/html}}{{noscript/}} \"'>"
            + "<script>console.log('page name');</script>{{/html}}{{noscript/}} - fr_FR", xitemLi.text());
        List<String> attachmentLinksText =
            document.select("#packagelistcontainer .xitemcontainer.package .name>a")
                .stream()
                .map(Element::text)
                .toList();
        assertThat(attachmentLinksText,
            containsInAnyOrder("><script>console.log('secondAttachment')</script>{{/html}}{{noscript/}}",
                "attachment.xar"));
    }
}
