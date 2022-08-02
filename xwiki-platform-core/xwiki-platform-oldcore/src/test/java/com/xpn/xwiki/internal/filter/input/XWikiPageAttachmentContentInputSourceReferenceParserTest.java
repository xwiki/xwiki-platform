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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link XWikiPageAttachmentContentInputSourceReferenceParser}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class XWikiPageAttachmentContentInputSourceReferenceParserTest
{
    @InjectMockComponents
    private XWikiPageAttachmentContentInputSourceReferenceParser parser;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private Environment environment;

    @Test
    void parse() throws FilterException, IOException, XWikiException
    {
        DocumentReference documentReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "currentspace", "currentdocument");
        XWikiDocument document = new XWikiDocument(documentReference);
        document.setAttachment("file.txt", new ByteArrayInputStream("12345".getBytes()),
            this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(document);
        this.oldcore.getXWikiContext().getWiki().saveDocument(document, this.oldcore.getXWikiContext());

        XWikiAttachmentContentInputSource source = (XWikiAttachmentContentInputSource) this.parser.parse("file.txt");

        assertEquals("12345", IOUtils.toString(source.getInputStream(), StandardCharsets.UTF_8));
    }
}
