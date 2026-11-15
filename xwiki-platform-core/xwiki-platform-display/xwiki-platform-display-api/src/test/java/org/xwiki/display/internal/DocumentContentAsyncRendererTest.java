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
package org.xwiki.display.internal;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.rendering.async.internal.AsyncProperties;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentContentAsyncRenderer}.
 *
 * @version $Id$
 */
@ComponentTest
class DocumentContentAsyncRendererTest
{
    @InjectMockComponents
    private DocumentContentAsyncRenderer renderer;

    @MockComponent
    private DocumentContentAsyncParser asyncParser;

    @MockComponent
    private DocumentContentAsyncExecutor executor;

    @Mock
    private DocumentModelBridge document;

    @BeforeEach
    void beforeEach()
    {
        when(this.asyncParser.getAsyncProperties(this.document))
            .thenReturn(new AsyncProperties(true, true, Set.of("wiki")));
    }

    @Test
    void isAsyncAndCacheAllowedWithTransformedContent()
    {
        DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();
        this.renderer.initialize(this.document, parameters);

        assertTrue(this.renderer.isAsyncAllowed());
        assertTrue(this.renderer.isCacheAllowed());
        assertNotNull(this.renderer.getId());
        verify(this.executor).initialize(any(), same(this.document), same(parameters), eq(true));
    }

    @Test
    void isAsyncAndCacheAllowedWithoutTransformedContent()
    {
        DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();
        parameters.setContentTransformed(false);
        this.renderer.initialize(this.document, parameters);

        assertFalse(this.renderer.isAsyncAllowed());
        assertFalse(this.renderer.isCacheAllowed());
        assertNull(this.renderer.getId());
        verify(this.executor).initialize(any(), same(this.document), same(parameters), eq(false));
    }
}
