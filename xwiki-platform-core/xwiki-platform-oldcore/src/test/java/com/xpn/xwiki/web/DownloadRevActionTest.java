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
package com.xpn.xwiki.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

/**
 * Unit test for {@link DownloadRevAction}.
 *
 * @version $Id$
 */
@OldcoreTest
class DownloadRevActionTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Mock
    private XWikiRequest request;

    @InjectMockComponents
    private DownloadRevAction action;

    @MockComponent
    private ResourceReferenceManager resourceReferenceManager;

    private DocumentReference documentReference;

    @BeforeEach
    void setUp()
    {
        // Prepare context
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        xcontext.setRequest(this.request);

        // Prepare a doc reference
        this.documentReference = new DocumentReference("wiki", "space", "page");
        XWikiDocument document = new XWikiDocument(this.documentReference);
        xcontext.setDoc(document);
    }

    /**
     * Checks that if the filename is missing, we get an XWikiException about an attachment not found.
     */
    @Test
    void renderWhenMissingFilename()
    {
        // Resource reference is set up so that no ATTACHMENT reference name is found
        ResourceReference rr = new EntityResourceReference(this.documentReference, EntityResourceAction.VIEW);
        when(this.resourceReferenceManager.getResourceReference()).thenReturn(rr);

        XWikiException e = assertThrows(XWikiException.class,
            () -> this.action.render(this.oldcore.getXWikiContext()),
            "Expected an exception for a missing filename."
        );
        assertEquals("Error number 11003 in 11: No attachment found for resource [type = [entity], parameters = [], "
            + "reference = [wiki:space.page], action = [view], locale = [<null>], anchor = [<null>]]", e.getMessage());
    }
}
