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
package org.xwiki.wikistream.instance.internal.input;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.output.DocumentInstanceOutputProperties;
import org.xwiki.wikistream.instance.internal.output.DocumentInstanceOutputWikiStream;

import com.xpn.xwiki.XWikiException;

/**
 * Validate {@link DocumentInstanceOutputWikiStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class DocumentInstanceInputWikiStreamTest extends AbstractInstanceInputWikiStreamTest
{
    // Tests

    @Test
    public void testImportDocumentsPreserveVersion() throws WikiStreamException, XWikiException, ParseException,
        IOException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();
        outputProperties.setPreserveVersion(true);

        XWikiDocumentInputProperties inputProperties = new XWikiDocumentInputProperties();
        inputProperties.setWithWikiAttachmentRevisions(false);
        inputProperties.setWithWikiDocumentRevisions(false);

        assertXML("document1", outputProperties, inputProperties);
    }
}
