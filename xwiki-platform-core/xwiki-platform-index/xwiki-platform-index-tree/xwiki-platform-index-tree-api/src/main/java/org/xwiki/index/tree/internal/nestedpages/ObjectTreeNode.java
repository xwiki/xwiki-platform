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
package org.xwiki.index.tree.internal.nestedpages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractEntityTreeNode;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * The object tree node.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component
@Named("object")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ObjectTreeNode extends AbstractEntityTreeNode
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        EntityReference objectReference = resolve(nodeId);
        if (objectReference != null && objectReference.getType() == EntityType.OBJECT) {
            try {
                return serialize(getChildren(new BaseObjectReference(objectReference), offset, limit));
            } catch (Exception e) {
                this.logger.warn("Failed to retrieve the children of [{}]. Root cause is [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return Collections.emptyList();
    }

    private List<ObjectPropertyReference> getChildren(ObjectReference objectReference, int offset, int limit)
        throws Exception
    {
        List<ObjectPropertyReference> children = new ArrayList<ObjectPropertyReference>();
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(objectReference.getParent(), xcontext);
        BaseObject object = document.getXObject(objectReference);
        if (object != null) {
            List<String> properties = new ArrayList<String>(object.getPropertyList());
            Collections.sort(properties);
            for (String property : subList(properties, offset, limit)) {
                children.add(new ObjectPropertyReference(property, objectReference));
            }
        }
        return children;
    }

    @Override
    public int getChildCount(String nodeId)
    {
        EntityReference objectReference = resolve(nodeId);
        if (objectReference != null && objectReference.getType() == EntityType.OBJECT) {
            try {
                return getChildCount(new BaseObjectReference(objectReference));
            } catch (Exception e) {
                this.logger.warn("Failed to count the children of [{}]. Root cause is [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return 0;
    }

    private int getChildCount(ObjectReference objectReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(objectReference.getParent(), xcontext);
        BaseObject object = document.getXObject(objectReference);
        return object == null ? 0 : object.getPropertyList().size();
    }

    @Override
    public String getParent(String nodeId)
    {
        EntityReference objectReference = resolve(nodeId);
        if (objectReference != null && objectReference.getType() == EntityType.OBJECT) {
            DocumentReference classReference = new BaseObjectReference(objectReference).getXClassReference();
            return "objectsOfType:" + this.defaultEntityReferenceSerializer.serialize(objectReference.getParent())
                + this.defaultEntityReferenceSerializer.serialize(classReference);
        }
        return null;
    }
}
