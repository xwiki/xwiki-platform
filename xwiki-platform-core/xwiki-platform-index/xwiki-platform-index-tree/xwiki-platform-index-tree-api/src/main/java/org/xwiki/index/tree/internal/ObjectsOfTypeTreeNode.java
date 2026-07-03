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
package org.xwiki.index.tree.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.tree.AbstractTreeNode;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The "objects of type" tree node.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("objectsOfType")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ObjectsOfTypeTreeNode extends AbstractTreeNode
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        DocumentReference[] parts = resolve(nodeId);
        if (parts != null) {
            try {
                List<String> children = new ArrayList<>();
                for (ObjectReference objectReference : subList(getXObjectReferences(parts[0], parts[1]), offset,
                    limit)) {
                    children.add("object:" + this.defaultEntityReferenceSerializer.serialize(objectReference));
                }
                return children;
            } catch (Exception e) {
                this.logger.warn("Failed to retrieve the children of [{}]. Root cause is [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return Collections.emptyList();
    }

    @Override
    public int getChildCount(String nodeId)
    {
        DocumentReference[] parts = resolve(nodeId);
        if (parts != null) {
            try {
                return getXObjectReferences(parts[0], parts[1]).size();
            } catch (Exception e) {
                this.logger.warn("Failed to count the children of [{}]. Root cause is [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return 0;
    }

    @Override
    public String getParent(String nodeId)
    {
        DocumentReference[] parts = resolve(nodeId);
        if (parts != null) {
            return "objects:" + this.defaultEntityReferenceSerializer.serialize(parts[0]);
        }
        return null;
    }

    private DocumentReference[] resolve(String nodeId)
    {
        String[] parts = StringUtils.split(nodeId, ":", 2);
        if (parts == null || parts.length != 2 || !"objectsOfType".equals(parts[0])) {
            return null;
        }

        int separatorIndex = parts[1].lastIndexOf('/');
        if (separatorIndex < 0) {
            return null;
        }

        String serializedDocRef = parts[1].substring(0, separatorIndex);
        String serializedClassRef = parts[1].substring(separatorIndex + 1);
        return new DocumentReference[] {this.currentDocumentReferenceResolver.resolve(serializedDocRef),
            this.currentDocumentReferenceResolver.resolve(serializedClassRef)};
    }

    private List<ObjectReference> getXObjectReferences(DocumentReference documentReference,
        DocumentReference classReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        List<ObjectReference> objectReferences = new ArrayList<>();
        List<BaseObject> objects = document.getXObjects(classReference);
        if (objects != null) {
            for (BaseObject object : objects) {
                // Yes, the list of objects can contain null values..
                if (object != null) {
                    objectReferences.add(object.getReference());
                }
            }
        }
        return objectReferences;
    }
}
