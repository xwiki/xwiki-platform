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
package org.xwiki.officeimporter.server.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.officeimporter.server.OfficeServerException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class OfficeServerScriptServiceTest
{
    @InjectMockComponents
    private OfficeServerScriptService officeServerScriptService;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private OfficeServer officeServer;

    @MockComponent
    private Execution execution;

    @Mock
    private ExecutionContext executionContext;

    @BeforeEach
    private void setUp()
    {
        when (this.execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void startServer() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(true);
        assertTrue(this.officeServerScriptService.startServer());
        verify(this.officeServer).start();
    }

    @Test
    public void startServerForbidden() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("subwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(true);
        assertFalse(this.officeServerScriptService.startServer());
        verify(this.officeServer, never()).start();
        verify(this.executionContext).setProperty(OfficeServerScriptService.OFFICE_MANAGER_ERROR,
            OfficeServerScriptService.ERROR_FORBIDDEN);
    }

    @Test
    public void startServerPrivileges() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(false);
        assertFalse(this.officeServerScriptService.startServer());
        verify(this.officeServer, never()).start();
        verify(this.executionContext).setProperty(OfficeServerScriptService.OFFICE_MANAGER_ERROR,
            OfficeServerScriptService.ERROR_PRIVILEGES);
    }

    @Test
    public void startServerError() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(true);
        doThrow(new OfficeServerException("Error while starting")).when(this.officeServer).start();
        assertFalse(this.officeServerScriptService.startServer());
        verify(this.officeServer).start();
        verify(this.executionContext).setProperty(OfficeServerScriptService.OFFICE_MANAGER_ERROR,
            "Error while starting");
    }

    @Test
    public void stopServer() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(true);
        assertTrue(this.officeServerScriptService.stopServer());
        verify(this.officeServer).stop();
    }

    @Test
    public void stopServerForbidden() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("subwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(true);
        assertFalse(this.officeServerScriptService.stopServer());
        verify(this.officeServer, never()).stop();
        verify(this.executionContext).setProperty(OfficeServerScriptService.OFFICE_MANAGER_ERROR,
            OfficeServerScriptService.ERROR_FORBIDDEN);
    }

    @Test
    public void stopServerPrivileges() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(false);
        assertFalse(this.officeServerScriptService.stopServer());
        verify(this.officeServer, never()).stop();
        verify(this.executionContext).setProperty(OfficeServerScriptService.OFFICE_MANAGER_ERROR,
            OfficeServerScriptService.ERROR_PRIVILEGES);
    }

    @Test
    public void stopServerError() throws OfficeServerException
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.documentAccessBridge.hasProgrammingRights()).thenReturn(true);
        doThrow(new OfficeServerException("Error while stopping")).when(this.officeServer).stop();
        assertFalse(this.officeServerScriptService.stopServer());
        verify(this.officeServer).stop();
        verify(this.executionContext).setProperty(OfficeServerScriptService.OFFICE_MANAGER_ERROR,
            "Error while stopping");
    }
}
