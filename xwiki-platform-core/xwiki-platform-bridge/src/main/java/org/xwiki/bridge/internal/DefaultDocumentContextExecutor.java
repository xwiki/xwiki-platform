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
package org.xwiki.bridge.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;

/**
 * Default implementation of {@link DocumentContextExecutor}.
 *
 * @version $Id$
 * @since 14.10.6
 * @since 15.2RC1
 */
@Component
@Singleton
public class DefaultDocumentContextExecutor implements DocumentContextExecutor
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ModelContext modelContext;

    @Override
    public <V> V call(Callable<V> callable, DocumentModelBridge document) throws Exception
    {
        Map<String, Object> backupObjects = new HashMap<>();
        EntityReference currentWikiReference = this.modelContext.getCurrentEntityReference();
        boolean canPop = false;

        try {
            this.documentAccessBridge.pushDocumentInContext(backupObjects, document);
            canPop = true;
            // Make sure to synchronize the context wiki with the context document's wiki.
            this.modelContext.setCurrentEntityReference(document.getDocumentReference().getWikiReference());

            return callable.call();
        } finally {
            if (canPop) {
                this.documentAccessBridge.popDocumentFromContext(backupObjects);
                // Also restore the context wiki.
                this.modelContext.setCurrentEntityReference(currentWikiReference);
            }
        }
    }
}
