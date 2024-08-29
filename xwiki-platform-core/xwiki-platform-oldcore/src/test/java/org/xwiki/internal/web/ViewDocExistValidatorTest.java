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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ViewDocExistValidator}.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@ComponentTest
class ViewDocExistValidatorTest
{
    public static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private ViewDocExistValidator validator;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XWikiDocument revisionDoc;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiRequest request;

    @BeforeEach
    void setUp()
    {
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.doc.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
    }

    @Test
    void docExistNoRev()
    {
        assertFalse(this.validator.docExist(this.doc, this.context));
    }

    @Test
    void docExistIsNewNoRev()
    {
        when(this.doc.isNew()).thenReturn(true);
        assertTrue(this.validator.docExist(this.doc, this.context));
    }

    @ParameterizedTest
    @CsvSource({ "recyclebin,false", "children,false", "siblings,false", "other,true" })
    void docExistIsNewNoRevHasViewer(String viewer, boolean expected)
    {
        when(this.doc.isNew()).thenReturn(true);
        when(this.request.get("viewer")).thenReturn(viewer);
        assertEquals(expected, this.validator.docExist(this.doc, this.context));
    }

    @Test
    void docExistHasRevExists() throws Exception
    {
        when(this.request.get("rev")).thenReturn("deleted:2");
        when(this.documentRevisionProvider.getRevision(this.doc, "deleted:2")).thenReturn(this.revisionDoc);
        assertFalse(this.validator.docExist(this.doc, this.context));
    }

    @Test
    void docExistHasRevDoesNotExist() throws Exception
    {
        when(this.request.get("rev")).thenReturn("deleted:2");
        when(this.documentRevisionProvider.getRevision(this.doc, "deleted:2")).thenReturn(null);
        assertTrue(this.validator.docExist(this.doc, this.context));
    }

    @Test
    void docExistHasRevHasError() throws Exception
    {
        when(this.request.get("rev")).thenReturn("deleted:2");
        when(this.documentRevisionProvider.getRevision(this.doc, "deleted:2")).thenThrow(XWikiException.class);
        assertTrue(this.validator.docExist(this.doc, this.context));
        assertEquals(1, this.logCapture.size());
        assertEquals("Error while accessing document [wiki:space.page] in revision [deleted:2]. "
            + "Cause: [XWikiException: Error number 0 in 0].", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }
}
