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
package org.xwiki.extension.xar.internal.job.diff;

import org.junit.jupiter.api.Test;
import org.xwiki.diff.display.internal.CharSplitter;
import org.xwiki.diff.display.internal.DefaultInlineDiffDisplayer;
import org.xwiki.diff.display.internal.DefaultUnifiedDiffDisplayer;
import org.xwiki.diff.display.internal.LineSplitter;
import org.xwiki.diff.internal.DefaultDiffManager;
import org.xwiki.extension.xar.job.diff.DocumentUnifiedDiff;
import org.xwiki.extension.xar.job.diff.EntityUnifiedDiff;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DocumentUnifiedDiffBuilder}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({DefaultInlineDiffDisplayer.class, DefaultUnifiedDiffDisplayer.class, DefaultDiffManager.class,
    CharSplitter.class, LineSplitter.class})
class DocumentUnifiedDiffBuilderTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DocumentUnifiedDiffBuilder diffBuilder;

    @MockComponent
    private GeneralMailConfiguration emails;

    @Test
    void diffWithObfustedEmails() throws XWikiException
    {
        LocalDocumentReference classLocalReference = new LocalDocumentReference("Space", "Class");
        DocumentReference classDocumentReference =
            new DocumentReference(classLocalReference, new WikiReference("wiki"));
        XWikiDocument classDocument =
            this.oldcore.getSpyXWiki().getDocument(classDocumentReference, this.oldcore.getXWikiContext());
        BaseClass xclass = classDocument.getXClass();
        xclass.addPasswordField("password", "Password", 30);
        xclass.addEmailField("email", "Email", 30);
        xclass.addTextField("text", "Text", 30);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, this.oldcore.getXWikiContext());

        XWikiDocument document1 = new XWikiDocument(new DocumentReference("wiki", "space", "document"));

        XWikiDocument document2 = document1.clone();
        BaseObject object = document2.newXObject(classLocalReference, this.oldcore.getXWikiContext());
        object.setStringValue("password", "password content");
        object.setStringValue("email", "email content");
        object.setStringValue("text", "text content");

        DocumentUnifiedDiff diff = this.diffBuilder.diff(document1, document2);
        assertEquals(1, diff.getObjectDiffs().size());
        EntityUnifiedDiff<ObjectReference> objectDiff = diff.getObjectDiffs().get(0);
        assertEquals(3, objectDiff.size());
        assertEquals("[@@ -1,0 +1,1 @@\n+text content\n]", objectDiff.get("text").toString());
        assertEquals("[]", objectDiff.get("password").toString());
        assertEquals("[@@ -1,0 +1,1 @@\n+email content\n]", objectDiff.get("email").toString());

        when(this.emails.shouldObfuscate()).thenReturn(true);

        diff = this.diffBuilder.diff(document1, document2);
        assertEquals(1, diff.getObjectDiffs().size());
        objectDiff = diff.getObjectDiffs().get(0);
        assertEquals(3, objectDiff.size());
        assertEquals("[@@ -1,0 +1,1 @@\n+text content\n]", objectDiff.get("text").toString());
        assertEquals("[]", objectDiff.get("password").toString());
        assertEquals("[]", objectDiff.get("email").toString());
    }
}
