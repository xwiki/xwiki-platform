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
package org.xwiki.extension.xar.script;

import java.util.List;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.extension.xar.internal.security.XarSecurityTool;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.xwiki.extension.xar.security.ProtectionLevel.DENY;
import static org.xwiki.extension.xar.security.ProtectionLevel.NONE;
import static org.xwiki.extension.xar.security.ProtectionLevel.WARNING;
import static org.xwiki.security.authorization.Right.EDIT;

/**
 * Test of {@link SecurityLevelEditConfirmationChecker}.
 *
 * @version $Id$
 */
@ComponentTest
class SecurityLevelEditConfirmationCheckerTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("wiki", "Space", "User");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "Space", "Doc");

    private static final XDOM XDOM = new XDOM(List.of());

    @InjectMockComponents
    private SecurityLevelEditConfirmationChecker checker;

    @MockComponent
    private XarSecurityTool securityTool;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private TemplateManager templateManager;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument tdoc;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getUserReference()).thenReturn(USER_REFERENCE);
        when(this.context.get("tdoc")).thenReturn(this.tdoc);
        when(this.tdoc.getDocumentReferenceWithLocale()).thenReturn(DOCUMENT_REFERENCE);
        when(this.templateManager.executeNoException(anyString())).thenReturn(XDOM);
    }

    @Test
    void checkDeny()
    {
        when(this.securityTool.getProtectionLevel(EDIT, USER_REFERENCE, DOCUMENT_REFERENCE)).thenReturn(DENY);
        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, true)), this.checker.check());
    }

    @Test
    void checkNone()
    {
        when(this.securityTool.getProtectionLevel(EDIT, USER_REFERENCE, DOCUMENT_REFERENCE)).thenReturn(NONE);
        assertEquals(Optional.empty(), this.checker.check());
    }

    @Test
    void checkWarning()
    {
        when(this.securityTool.getProtectionLevel(EDIT, USER_REFERENCE, DOCUMENT_REFERENCE)).thenReturn(WARNING);
        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, false, WARNING)), this.checker.check());
    }
}
