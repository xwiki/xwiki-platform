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

import java.util.Arrays;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;

/**
 * Displays the content of a document.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("content")
@Singleton
public class DocumentContentDisplayer implements DocumentDisplayer
{
    @Inject
    private Provider<DocumentContentAsyncRenderer> rendererProvider;

    @Inject
    private BlockAsyncRendererExecutor executor;

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        // Get a renderer
        DocumentContentAsyncRenderer renderer = this.rendererProvider.get();
        Set<String> contextEntries = renderer.initialize(document, parameters);

        // Configure
        AsyncRendererConfiguration configuration = new AsyncRendererConfiguration();
        configuration.setContextEntries(contextEntries);

        // Execute
        try {
            Block block = this.executor.execute(renderer, configuration);

            return block instanceof XDOM ? (XDOM) block : new XDOM(Arrays.asList(block));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
