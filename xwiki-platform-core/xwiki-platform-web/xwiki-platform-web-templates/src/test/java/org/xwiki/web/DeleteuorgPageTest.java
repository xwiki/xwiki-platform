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
package org.xwiki.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.extension.script.ExtensionManagerScriptService;
import org.xwiki.extension.xar.script.XarExtensionScriptService;
import org.xwiki.extension.xar.security.ProtectionLevel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@code deleteuorg.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    ModelScriptService.class
})
class DeleteuorgPageTest extends PageTest
{
    private static final String DELETEUORG = "deleteuorg.vm";

    private static final DocumentReference GROUP_REFERENCE = new DocumentReference("xwiki", "XWiki", "G1");

    private TemplateManager templateManager;

    private XarExtensionScriptService xarExtensionScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        CSRFTokenScriptService csrfScriptService = mock(CSRFTokenScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "csrf", csrfScriptService);
        when(csrfScriptService.isTokenValid("token42")).thenReturn(true);

        ExtensionManagerScriptService extensionScriptService = mock(ExtensionManagerScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, ExtensionManagerScriptService.ROLEHINT,
            extensionScriptService);
        this.xarExtensionScriptService = mock(XarExtensionScriptService.class);
        doReturn(this.xarExtensionScriptService).when(extensionScriptService).get("xar");

        XWikiDocument group = this.xwiki.getDocument(GROUP_REFERENCE, this.context);
        this.xwiki.saveDocument(group, this.context);

        when(this.oldcore.getMockRightService().hasAccessLevel(eq("delete"), any(), eq("xwiki:XWiki.G1"), any()))
            .thenReturn(true);

        this.request.put("docname", "XWiki.G1");
    }

    @Test
    void deleteNotProtectedPage() throws Exception
    {
        // No protection level is returned, as when the XAR extension handler is not installed.
        this.request.put("form_token", "token42");

        assertEquals("OK", this.templateManager.render(DELETEUORG).trim());
        assertFalse(this.xwiki.exists(GROUP_REFERENCE, this.context));
    }

    @Test
    void deleteProtectedPageWithoutForce() throws Exception
    {
        when(this.xarExtensionScriptService.getDeleteSecurityLevel(any(), eq(GROUP_REFERENCE)))
            .thenReturn(ProtectionLevel.WARNING);
        this.request.put("form_token", "token42");

        assertDeleteRefused();
    }

    @Test
    void deleteProtectedPageWithForce() throws Exception
    {
        when(this.xarExtensionScriptService.getDeleteSecurityLevel(any(), eq(GROUP_REFERENCE)))
            .thenReturn(ProtectionLevel.WARNING);
        this.request.put("form_token", "token42");
        this.request.put("force", "true");

        assertEquals("OK", this.templateManager.render(DELETEUORG).trim());
        assertFalse(this.xwiki.exists(GROUP_REFERENCE, this.context));
    }

    @Test
    void deleteDeniedPageWithForce() throws Exception
    {
        when(this.xarExtensionScriptService.getDeleteSecurityLevel(any(), eq(GROUP_REFERENCE)))
            .thenReturn(ProtectionLevel.DENY);
        this.request.put("form_token", "token42");
        this.request.put("force", "true");

        assertDeleteRefused();
    }

    @Test
    void deleteWithInvalidCSRFToken() throws Exception
    {
        this.request.put("form_token", "invalid");

        assertEquals("FAIL", this.templateManager.render(DELETEUORG).trim());
        assertTrue(this.xwiki.exists(GROUP_REFERENCE, this.context));
    }

    private void assertDeleteRefused() throws Exception
    {
        XWikiResponse response = spy(this.response);
        this.context.setResponse(response);

        assertEquals("FAIL", this.templateManager.render(DELETEUORG).trim());
        verify(response).setStatus(403);
        assertTrue(this.xwiki.exists(GROUP_REFERENCE, this.context));
    }
}
