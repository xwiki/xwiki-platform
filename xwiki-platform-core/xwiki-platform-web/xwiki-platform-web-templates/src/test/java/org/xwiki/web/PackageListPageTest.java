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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@code packagelist.vm}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 * @since 14.4.8
 * @since 13.10.11
 */
@ComponentList({
    XWikiUsersDocumentInitializer.class
})
class PackageListPageTest extends PageTest
{
    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
    }

    @Test
    void noAttachments() throws Exception
    {
        XWikiDocument doc = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Doc"), this.context);
        this.context.setDoc(doc);
        Document document = Jsoup.parse(this.templateManager.render("packagelist.vm"));
        assertEquals("core.importer.noPackageAvailable", document.selectFirst(".noitems").text());
    }

    @Test
    void attachmentsList() throws Exception
    {
        DocumentReference xwikiUserClass = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        DocumentReference attachmentAuthorReference = new DocumentReference("xwiki", "XWiki", "U1");

        this.xwiki.initializeMandatoryDocuments(this.context);

        // Initialize the current document.
        XWikiDocument doc = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Doc"), this.context);
        XWikiAttachment xWikiAttachment =
            doc.setAttachment("attachment1", toInputStream("abcd", defaultCharset()), this.context);
        xWikiAttachment.setAuthorReference(attachmentAuthorReference);
        this.xwiki.saveDocument(doc, this.context);
        this.context.setDoc(doc);

        // Initialize the attachment author.
        XWikiDocument attachmentAuthorDocument = this.xwiki.getDocument(attachmentAuthorReference, this.context);
        BaseObject xWikiUsersXObject =
            attachmentAuthorDocument.newXObject(xwikiUserClass, this.context);
        xWikiUsersXObject.set("first_name", "<strong>NAME</strong/>", this.context);
        this.xwiki.saveDocument(attachmentAuthorDocument, this.context);

        Document document = Jsoup.parse(this.templateManager.render("packagelist.vm"));

        Element infos = document.selectFirst(".infos");
        String formattedDate = this.xwiki.formatDate(attachmentAuthorDocument.getDate(), null, this.context);
        assertEquals(String.format("core.importer.packageInformationExtract [<strong>NAME</strong/>, %s] - (4 bytes)",
            formattedDate), infos.text());
    }
}
