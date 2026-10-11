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
import java.util.Locale;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.doc.lock.UnlockRule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.internal.edit.EditModeResolver;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private XWikiDocumentLockEditConfirmationChecker checker;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private EditModeResolver editModeResolver;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private XWikiContext context;

    private XWikiDocument tdoc;

    private ComponentManager contextComponentManager;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        this.context = mock(XWikiContext.class);
        this.tdoc = mock(XWikiDocument.class);
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.get("tdoc")).thenReturn(this.tdoc);
        when(this.context.getUser()).thenReturn("U1");
        when(this.templateManager.executeNoException(anyString())).thenReturn(XDOM);
        this.contextComponentManager = mock(ComponentManager.class);
        when(this.componentManagerProvider.get()).thenReturn(this.contextComponentManager);
        when(this.contextComponentManager.getInstanceList(UnlockRule.class)).thenReturn(List.of());
        Utils.setComponentManager(componentManager);
    }

    private void lockedByAnotherUser() throws Exception
    {
        // Locked by user U2, while the current user is U1.
        when(this.tdoc.getLock(this.context)).thenReturn(new XWikiLock(42, "U2"));
    }

    @Test
    void checkNotLocked() throws Exception
    {
        // Not locked.
        when(this.tdoc.getLock(this.context)).thenReturn(null);
        assertEquals(Optional.empty(), this.checker.check());
        // The unlock rules are evaluated only when the document is actually locked.
        verifyNoInteractions(this.componentManagerProvider);
    }

    @Test
    void checkLocked() throws Exception
    {
        lockedByAnotherUser();
        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, false)), this.checker.check());
    }

    @Test
    void checkLockedBySelf() throws Exception
    {
        // Locked by user U1, current user is U1
        when(this.tdoc.getLock(this.context)).thenReturn(new XWikiLock(42, "U1"));
        assertEquals(Optional.empty(), this.checker.check());
    }

    @Test
    void checkLockedWithUnlockRuleThatApplies() throws Exception
    {
        lockedByAnotherUser();
        UnlockRule rejectingRule = mock(UnlockRule.class, "rejecting");
        UnlockRule acceptingRule = mock(UnlockRule.class, "accepting");
        when(acceptingRule.canUnlock(any())).thenReturn(true);
        when(this.contextComponentManager.getInstanceList(UnlockRule.class))
            .thenReturn(List.of(rejectingRule, acceptingRule));
        when(this.tdoc.getDocumentReferenceWithLocale())
            .thenReturn(new DocumentReference("wiki", "space", "page", Locale.FRENCH));
        when(this.tdoc.getRealLocale()).thenReturn(null);

        assertEquals(Optional.empty(), this.checker.check());

        // All the rules receive the same lock context, referencing the translated document.
        ArgumentCaptor<LockContext> lockContextCaptor = ArgumentCaptor.forClass(LockContext.class);
        verify(rejectingRule).canUnlock(lockContextCaptor.capture());
        verify(acceptingRule).canUnlock(lockContextCaptor.capture());
        LockContext lockContext = lockContextCaptor.getAllValues().get(0);
        assertSame(this.tdoc.getDocumentReferenceWithLocale(),
            lockContextCaptor.getAllValues().get(0).getDocumentReferenceWithLocale());
        assertSame(lockContext, lockContextCaptor.getAllValues().get(1));
    }

    @Test
    void checkLockedWithUnlockRuleThatDoesntApply() throws Exception
    {
        lockedByAnotherUser();
        UnlockRule rejectingRule = mock(UnlockRule.class);
        when(this.contextComponentManager.getInstanceList(UnlockRule.class)).thenReturn(List.of(rejectingRule));

        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, false)), this.checker.check());
    }

    @Test
    void checkLockedWhenUnlockRuleLookupFails() throws Exception
    {
        lockedByAnotherUser();
        when(this.contextComponentManager.getInstanceList(UnlockRule.class))
            .thenThrow(new ComponentLookupException("Failed to look up the unlock rules."));

        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, false)), this.checker.check());
        assertEquals("Failed to look up the unlock rules. Root cause is [ComponentLookupException: Failed to look up "
            + "the unlock rules.].", this.logCapture.getMessage(0));
    }
}
