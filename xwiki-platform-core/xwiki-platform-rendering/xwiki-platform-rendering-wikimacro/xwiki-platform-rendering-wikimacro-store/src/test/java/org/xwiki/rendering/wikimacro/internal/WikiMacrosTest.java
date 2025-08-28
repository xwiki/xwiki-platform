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
package org.xwiki.rendering.wikimacro.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Various general tests on wiki macros.
 * 
 * @version $Id$
 */
@AllComponents
@OldcoreTest
class WikiMacrosTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWikiDocument macroDocument;

    private BaseObject macroObject;

    @BeforeEach
    void before() throws Exception
    {
        this.macroDocument = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));
        this.macroDocument.setSyntax(Syntax.XWIKI_2_0);
        this.macroObject = new BaseObject();
        this.macroObject.setXClassReference(new DocumentReference("wiki", "XWiki", "WikiMacroClass"));
        this.macroObject.setStringValue("id", "macroid");
        this.macroObject.setLargeStringValue("code", "code");
        this.macroDocument.addXObject(macroObject);

        this.oldcore.getXWikiContext().setWikiId("wiki");

        // We need component related events
        this.oldcore.notifyComponentDescriptorEvent();
        this.oldcore.notifyDocumentCreatedEvent(true);
        this.oldcore.notifyDocumentUpdatedEvent(true);
    }

    private ComponentManager getWikiComponentManager() throws Exception
    {
        return this.oldcore.getMocker().getInstance(ComponentManager.class, "wiki");
    }

    private ComponentManager getUserComponentManager() throws Exception
    {
        return this.oldcore.getMocker().getInstance(ComponentManager.class, "user");
    }

    @Test
    void saveWikiMacro() throws Exception
    {
        when(this.oldcore.getMockDocumentAuthorizationManager().hasAccess(any(), any(), any(), any())).thenReturn(true);

        this.macroObject.setStringValue("visibility", "Current Wiki");

        // Save wiki macro
        this.oldcore.getSpyXWiki().saveDocument(this.macroDocument, this.oldcore.getXWikiContext());

        Macro testMacro = getWikiComponentManager().getInstance(Macro.class, "macroid");

        assertEquals("macroid", testMacro.getDescriptor().getId().getId());

        // Verify that the macro is not in the global component manager (only in the wiki one).
        Throwable exception = assertThrows(ComponentLookupException.class, () -> {
            this.oldcore.getMocker().getInstance(Macro.class, "macroid");
        });
        assertEquals("Can't find descriptor for the component with type [interface org.xwiki.rendering.macro.Macro] "
            + "and hint [macroid]", exception.getMessage());
    }

    @Test
    void unRegisterWikiMacroWithDifferentVisibilityKeys() throws Exception
    {
        when(this.oldcore.getMockRightService().hasAccessLevel(any(String.class), any(String.class), any(String.class), any(XWikiContext.class))).thenReturn(true);

        this.macroObject.setStringValue("visibility", "Current User");

        DocumentReference user1 = new DocumentReference("wiki", "Wiki", "user1");

        this.macroDocument.setAuthorReference(user1);

        // Save wiki macro
        this.oldcore.getSpyXWiki().saveDocument(this.macroDocument, this.oldcore.getXWikiContext());

        // Try to lookup the macro
        this.oldcore.getXWikiContext().setUserReference(user1);
        Macro testMacro = getUserComponentManager().getInstance(Macro.class, "macroid");

        assertEquals("macroid", testMacro.getDescriptor().getId().getId());

        // register with another user

        DocumentReference user2 = new DocumentReference("wiki", "Wiki", "user2");

        this.macroDocument.setAuthorReference(user2);

        // Save wiki macro
        this.oldcore.getSpyXWiki().saveDocument(this.macroDocument, this.oldcore.getXWikiContext());

        // Try to lookup the macro
        this.oldcore.getXWikiContext().setUserReference(user2);
        testMacro = getUserComponentManager().getInstance(Macro.class, "macroid");

        assertEquals("macroid", testMacro.getDescriptor().getId().getId());

        // validate that the macro as been properly unregistered for former user
        this.oldcore.getXWikiContext().setUserReference(user1);

        // Verify that the macro has been properly unregistered from the user CM
        Throwable exception = assertThrows(ComponentLookupException.class, () -> {
            getUserComponentManager().getInstance(Macro.class, "macroid");
        });
        assertEquals("Can't find descriptor for the component with type [interface org.xwiki.rendering.macro.Macro] "
            + "and hint [macroid]", exception.getMessage());
    }
}
