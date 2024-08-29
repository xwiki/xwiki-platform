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
package org.xwiki.export.pdf.internal.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link SafePDFExportConfigurationProvider}.
 * 
 * @version $Id$
 */
@ComponentTest
class SafePDFExportConfigurationProviderTest
{
    @InjectMockComponents
    private SafePDFExportConfigurationProvider provider;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @Mock
    private PDFExportConfiguration config;

    @Test
    void get() throws Exception
    {
        PDFExportConfiguration safeConfig = this.provider.get(this.config);

        when(this.config.isServerSide()).thenReturn(true);
        assertTrue(safeConfig.isServerSide());

        when(this.config.isServerSide()).thenReturn(false);
        assertFalse(safeConfig.isServerSide());

        when(this.config.getXWikiURI()).thenReturn(new URI("//host.xwiki.internal"));
        assertNull(safeConfig.getXWikiURI());

        DocumentReference firstTemplateRef = new DocumentReference("test", "First", "Template");
        DocumentReference secondTemplateRef = new DocumentReference("test", "Second", "Template");
        when(this.authorization.hasAccess(Right.VIEW, secondTemplateRef)).thenReturn(true);
        when(this.config.getTemplates()).thenReturn(Arrays.asList(firstTemplateRef, secondTemplateRef));
        assertEquals(Collections.singletonList(secondTemplateRef), safeConfig.getTemplates());

        when(this.config.getMaxContentSize()).thenReturn(34);
        assertEquals(34, safeConfig.getMaxContentSize());

        when(this.config.getThreadPoolSize()).thenReturn(7);
        assertEquals(7, safeConfig.getThreadPoolSize());

        when(this.config.isReplacingFOP()).thenReturn(false);
        assertEquals(false, safeConfig.isReplacingFOP());
    }
}
