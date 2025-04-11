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
package org.xwiki.edit.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Configuration Source that looks for editor bindings in the preferences page of the current space.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Named("editorBindings/space")
@Singleton
public class SpaceEditorBindingsSource extends AbstractEditorBindingsSource
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    protected String getCacheId()
    {
        return "configuration.editorBindings.space";
    }

    @Override
    protected String getCacheKeyPrefix()
    {
        DocumentReference currentDocumentReference = this.documentAccessBridge.getCurrentDocumentReference();
        if (currentDocumentReference != null) {
            return this.referenceSerializer.serialize(currentDocumentReference.getParent());
        }
        return null;
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        DocumentReference currentDocumentReference = this.documentAccessBridge.getCurrentDocumentReference();
        if (currentDocumentReference != null) {
            return new DocumentReference("WebPreferences", (SpaceReference) currentDocumentReference.getParent());
        }
        return null;
    }
}
