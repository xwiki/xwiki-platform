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
package org.xwiki.model.validation.edit;

import java.util.List;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link XWikiDocumentLockEditConfirmationChecker}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiDocumentLockEditConfirmationCheckerTest
{
    private static final XDOM XDOM = new XDOM(List.of());

    @InjectMockComponents
    private XWikiDocumentLockEditConfirmationChecker checker;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private TemplateManager templateManager;

    private XWikiContext context;

    private XWikiDocument tdoc;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager)
    {
        this.context = mock(XWikiContext.class);
        this.tdoc = mock(XWikiDocument.class);
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.get("tdoc")).thenReturn(this.tdoc);
        when(this.context.getUser()).thenReturn("U1");
        when(this.templateManager.executeNoException(anyString())).thenReturn(XDOM);
        Utils.setComponentManager(componentManager);
    }

    @Test
    void checkNotLocked() throws Exception
    {
        // Not locked.
        when(this.tdoc.getLock(this.context)).thenReturn(null);
        assertEquals(Optional.empty(), this.checker.check());
    }

    @Test
    void checkLocked() throws Exception
    {
        // Locked by user U2, current user is U1
        when(this.tdoc.getLock(this.context)).thenReturn(new XWikiLock(42, "U2"));
        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, false)), this.checker.check());
    }

    @Test
    void checkLockedBySelf() throws Exception
    {
        // Locked by user U1, current user is U1
        when(this.tdoc.getLock(this.context)).thenReturn(new XWikiLock(42, "U1"));
        assertEquals(Optional.empty(), this.checker.check());
    }
}
