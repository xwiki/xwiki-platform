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
package org.xwiki.component.wiki.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Default implementation of {@link WikiComponentManagerContext}.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultWikiComponentManagerContext implements WikiComponentManagerContext
{
    /**
     * The {@link DocumentAccessBridge} component.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The {@link org.xwiki.model.ModelContext} component.
     */
    @Inject
    private ModelContext modelContext;

    /**
     * Used to serialize references of documents.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * The {@link org.xwiki.context.Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    @Override
    public DocumentReference getCurrentUserReference()
    {
        return this.documentAccessBridge.getCurrentUserReference();
    }

    @Override
    public EntityReference getCurrentEntityReference()
    {
        return this.modelContext.getCurrentEntityReference();
    }

    @Override
    public void setCurrentUserReference(DocumentReference reference)
    {
        this.documentAccessBridge.setCurrentUser(this.serializer.serialize(reference));
    }

    @Override
    public void setCurrentEntityReference(EntityReference reference)
    {
        this.modelContext.setCurrentEntityReference(reference);
    }
}
