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
package org.xwiki.internal.web;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test of {@link GetDocExistValidator}.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@ComponentTest
class GetDocExistValidatorTest
{
    public static final String TEST_SHEET = "testSheet";

    public static final DocumentReference SHEET_DOCUMENT_REFERENCE = new DocumentReference("wiki", "space",
        TEST_SHEET);

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private GetDocExistValidator validator;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentmixedReferenceResolver;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XWikiDocument sheetDoc;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWiki wiki;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.doc.isNew()).thenReturn(true);
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.currentmixedReferenceResolver.resolve(TEST_SHEET)).thenReturn(SHEET_DOCUMENT_REFERENCE);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(SHEET_DOCUMENT_REFERENCE, this.context)).thenReturn(this.sheetDoc);
    }

    @Test
    void docExistIsNew()
    {
        assertTrue(this.validator.docExist(this.doc, this.context));
    }

    @Test
    void docIsNotNew()
    {
        // The doc is not new and the request does not have any specific parameter.
        when(this.doc.isNew()).thenReturn(false);
        assertFalse(this.validator.docExist(this.doc, this.context));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void docExistIsSheet(boolean isSheetDocNew)
    {
        when(this.sheetDoc.isNew()).thenReturn(isSheetDocNew);
        when(this.request.get("sheet")).thenReturn(TEST_SHEET);
        // The 'isNew' status of the sheet is returned.
        assertEquals(isSheetDocNew, this.validator.docExist(this.doc, this.context));
    }

    @Test
    void docExistHasError() throws Exception
    {
        when(this.request.get("sheet")).thenReturn(TEST_SHEET);
        when(this.wiki.getDocument(SHEET_DOCUMENT_REFERENCE, this.context)).thenThrow(XWikiException.class);
        assertTrue(this.validator.docExist(this.doc, this.context));
        assertEquals(1, this.logCapture.size());
        assertEquals("Error while trying to load sheet [wiki:space.testSheet] for checking status code on GET "
            + "request for [null]: [XWikiException: Error number 0 in 0]", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void docExistDisableCheckNotExisting()
    {
        when(this.request.get("disableCheckNotExisting")).thenReturn("0");
        assertTrue(this.validator.docExist(this.doc, this.context));
    }
}
