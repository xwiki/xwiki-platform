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
package org.xwiki.export.pdf.internal.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link DocumentMetadataScriptContextInitializer}.
 * 
 * @version $Id$
 */
@ComponentTest
class DocumentMetadataScriptContextInitializerTest
{
    @InjectMockComponents
    private DocumentMetadataScriptContextInitializer initializer;

    @MockComponent
    private Execution execution;

    @Mock
    private ScriptContext scriptContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Object sourceDocument;

    @Mock
    private Object metadata;

    @Test
    void initialize()
    {
        this.initializer.initialize(this.scriptContext);
        verify(this.scriptContext, never()).setAttribute(anyString(), any(), anyInt());

        when(this.execution.getContext()).thenReturn(this.executionContext);
        this.initializer.initialize(this.scriptContext);
        verify(this.scriptContext, never()).setAttribute(anyString(), any(), anyInt());

        when(this.executionContext.hasProperty(DocumentMetadataExtractor.EXECUTION_CONTEXT_PROPERTY_METADATA))
            .thenReturn(true);
        when(this.executionContext.getProperty(DocumentMetadataExtractor.EXECUTION_CONTEXT_PROPERTY_METADATA))
            .thenReturn(this.metadata);
        this.initializer.initialize(this.scriptContext);
        verify(this.scriptContext).setAttribute("metadata", this.metadata, ScriptContext.ENGINE_SCOPE);
    }
}
