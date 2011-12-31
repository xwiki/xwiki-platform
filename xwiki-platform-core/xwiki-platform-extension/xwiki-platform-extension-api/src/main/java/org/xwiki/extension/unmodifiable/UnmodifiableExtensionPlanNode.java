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
package org.xwiki.extension.unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.wrap.AbstractWrappingObject;

/**
 * Provide a readonly access to an extension plan node.
 * 
 * @version $Id$
 */
public class UnmodifiableExtensionPlanNode extends AbstractWrappingObject<ExtensionPlanNode> implements
    ExtensionPlanNode
{
    /**
     * @see #getChildren()
     */
    private Collection<ExtensionPlanNode> wrappedChildren;

    /**
     * @param node the wrapped node
     */
    public UnmodifiableExtensionPlanNode(ExtensionPlanNode node)
    {
        super(node);
    }

    @Override
    public ExtensionPlanAction getAction()
    {
        return new UnmodifiableExtensionPlanAction(getWrapped().getAction());
    }

    @Override
    public Collection<ExtensionPlanNode> getChildren()
    {
        if (this.wrappedChildren == null) {
            Collection<ExtensionPlanNode> nodes = getWrapped().getChildren();
            if (nodes.isEmpty()) {
                this.wrappedChildren = Collections.emptyList();
            } else {
                this.wrappedChildren = new ArrayList<ExtensionPlanNode>(nodes.size());
                for (ExtensionPlanNode node : nodes) {
                    this.wrappedChildren.add(new UnmodifiableExtensionPlanNode(node));
                }
            }
        }

        return this.wrappedChildren;
    }

    @Override
    public VersionConstraint getInitialVersionConstraint()
    {
        return getWrapped().getInitialVersionConstraint();
    }
}
