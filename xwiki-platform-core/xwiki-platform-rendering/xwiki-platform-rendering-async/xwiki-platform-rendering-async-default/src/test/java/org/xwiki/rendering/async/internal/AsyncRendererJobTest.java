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
package org.xwiki.rendering.async.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.rendering.RenderingException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.mockito.Mockito.verify;

/**
 * Validate {@link AsyncRendererJob}.
 * 
 * @version $Id$
 */
@ComponentTest
public class AsyncRendererJobTest
{
    @InjectMockComponents
    private AsyncRendererJob job;

    @Mock
    private AsyncRenderer renderer;

    private AsyncRendererJobRequest request = new AsyncRendererJobRequest();

    @BeforeEach
    void beforeEach()
    {
        this.request.setRenderer(this.renderer);
    }

    @Test
    void runWithNullContext() throws RenderingException
    {
        this.job.initialize(this.request);

        this.job.run();

        verify(this.renderer).render(true, false);
    }
}
