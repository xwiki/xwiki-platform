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
package org.xwiki.flamingo.test.docker;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests validating various store APIs.
 *
 * @version $Id$
 */
@UITest
class HibernateStoreAPIIT
{
    private static final String ATTACHMENT_NAME = "attachment.txt";

    private static final String ATTACHMENT_CONTENT = "small attachment content";

    @BeforeAll
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
    }

    @Order(1)
    @Test
    void searchWithWhereParameters(TestUtils setup, TestReference testReference) throws Exception
    {
        // Make sure the page is not already there
        setup.rest().delete(testReference);

        String content = "{{groovy}}"
            + "print xwiki.getXWiki().search('select distinct doc.fullName from XWikiDocument as doc', [['doc.fullName', '"
            + setup.serializeReference(testReference.getLocalDocumentReference())
            + "']] as Object[][], xcontext.context).size()" + "{{/groovy}}";

        assertEquals("<p>0</p>", setup.executeWiki(content, Syntax.XWIKI_2_1));

        setup.rest().savePage(testReference);

        assertEquals("<p>1</p>", setup.executeWiki(content, Syntax.XWIKI_2_1));
    }

    /**
     * Verify that the document saved through the script API is the one returned by the API afterward, both for the
     * current document and for another one.
     */
    @Order(2)
    @Test
    void saveDocument(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.rest().delete(testReference);

        assertEquals("current content", setup.executeWikiPlain("{{groovy}}"
            + "doc.setContent('current content')\n"
            + "doc.saveAsAuthor()\n"
            + "print(doc.getContent())"
            + "{{/groovy}}", Syntax.XWIKI_2_1));

        assertEquals("other content", setup.executeWikiPlain("{{groovy}}"
            + "otherDoc = xwiki.getDocument('" + setup.serializeLocalReference(testReference) + "')\n"
            + "otherDoc.setContent('other content')\n"
            + "otherDoc.saveAsAuthor()\n"
            + "print(otherDoc.getContent())"
            + "{{/groovy}}", Syntax.XWIKI_2_1));
        Page page = setup.rest().get(testReference);
        assertEquals("other content", page.getContent());
    }

    /**
     * Verify that an attachment added through the script API is saved with the document, and that its version only
     * changes when its content changes.
     */
    @Order(3)
    @Test
    void addAttachment(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.rest().delete(testReference);
        setup.rest().savePage(testReference);

        String documentGetter = "xwiki.getDocument('" + setup.serializeLocalReference(testReference) + "')";
        String addAttachment = "{{groovy}}"
            + "def document = " + documentGetter + "\n"
            + "document.addAttachment('" + ATTACHMENT_NAME + "', '%s'.getBytes('UTF-8'))\n"
            + "print(document.getDocument().getAttachment('" + ATTACHMENT_NAME + "').isContentDirty())\n"
            + "document.saveAsAuthor()\n"
            + "print(' ' + " + documentGetter + ".getDocument().getAttachment('" + ATTACHMENT_NAME
            + "').isContentDirty())"
            + "{{/groovy}}";
        String attachmentGetter = documentGetter + ".getAttachment('" + ATTACHMENT_NAME + "')";

        // The attachment content is dirty until the document is saved.
        assertEquals("true false",
            setup.executeWikiPlain(addAttachment.formatted(ATTACHMENT_CONTENT), Syntax.XWIKI_2_1));

        // The attachment is stored with a single version.
        AttachmentReference attachmentReference = new AttachmentReference(ATTACHMENT_NAME, testReference);
        assertEquals(ATTACHMENT_CONTENT,
            new String(setup.rest().getAttachmentAsByteArray(attachmentReference), StandardCharsets.UTF_8));
        assertEquals("1 " + ATTACHMENT_CONTENT, setup.executeWikiPlain("{{groovy}}"
            + "print(" + attachmentGetter + ".getVersions().size() + ' ')\n"
            + "print(" + attachmentGetter + ".getAttachmentRevision('1.1').getContentAsString())"
            + "{{/groovy}}", Syntax.XWIKI_2_1));

        // Saving the document again doesn't create a new version of the attachment.
        setup.rest().savePage(testReference, "new content", "");
        String printVersion = "{{groovy}}print(" + attachmentGetter + ".getVersion()){{/groovy}}";
        assertEquals("1.1", setup.executeWikiPlain(printVersion, Syntax.XWIKI_2_1));

        // Adding the attachment again does.
        setup.executeWikiPlain(addAttachment.formatted("new attachment content"), Syntax.XWIKI_2_1);
        assertEquals("1.2", setup.executeWikiPlain(printVersion, Syntax.XWIKI_2_1));
    }
}
