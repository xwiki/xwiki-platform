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
package com.xpn.xwiki.internal.filter.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.output.DocumentInstanceOutputFilterStream;

/**
 * Validate {@link DocumentInstanceOutputFilterStream}.
 * 
 * @version $Id$
 */
class DocumentInstanceInputFilterStreamTest extends AbstractInstanceInputFilterStreamTest
{
    // Tests

    @Test
    void importDocumentsPreserveVersion() throws FilterException, IOException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();
        outputProperties.setVerbose(false);

        DocumentInstanceInputProperties inputProperties = new DocumentInstanceInputProperties();
        inputProperties.setWithRevisions(false);
        inputProperties.setWithJRCSRevisions(false);
        inputProperties.setVerbose(false);

        assertXML("document1", outputProperties, inputProperties);
    }

    @Test
    void withAttachmentContent() throws IOException, XWikiException, FilterException
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "document");
        XWikiDocument document = new XWikiDocument(reference);
        document.setDate(new Date(0));
        document.setContentUpdateDate(document.getDate());
        document.setCreationDate(document.getDate());
        document.setAttachment("attachment1", new ByteArrayInputStream("content1".getBytes()),
            this.oldcore.getXWikiContext());
        document.getAttachment("attachment1").setDate(document.getDate());
        document.setAttachment("attachment2", new ByteArrayInputStream("content2".getBytes()),
            this.oldcore.getXWikiContext());
        document.getAttachment("attachment2").setDate(document.getDate());
        document.setMetaDataDirty(false);
        document.setContentDirty(false);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        DocumentInstanceInputProperties inputProperties = new DocumentInstanceInputProperties();
        inputProperties.setAttachmentsContent(Collections.singleton("attachment1"));
        inputProperties.setWithRevisions(false);
        inputProperties.setWithJRCSRevisions(false);
        inputProperties.setVerbose(false);

        assertXML("documentwithattachment1content", inputProperties);

        inputProperties.setAttachmentsContent(Collections.singleton("attachment2"));

        assertXML("documentwithattachment2content", inputProperties);
    }
}
