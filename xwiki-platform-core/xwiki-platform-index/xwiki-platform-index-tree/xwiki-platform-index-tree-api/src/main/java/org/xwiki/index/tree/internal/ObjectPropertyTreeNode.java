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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * The object property tree node.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named(ObjectPropertyTreeNode.HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ObjectPropertyTreeNode extends AbstractEntityTreeNode
{
    /**
     * The component hint and also the tree node type.
     */
    public static final String HINT = "objectProperty";

    /**
     * Default constructor.
     */
    public ObjectPropertyTreeNode()
    {
        super(HINT);
    }

    @Override
    public String getParent(String nodeId)
    {
        EntityReference objectPropertyReference = resolve(nodeId);
        if (objectPropertyReference != null && objectPropertyReference.getType() == EntityType.OBJECT_PROPERTY) {
            return serialize(objectPropertyReference.getParent());
        }
        return null;
    }
}
