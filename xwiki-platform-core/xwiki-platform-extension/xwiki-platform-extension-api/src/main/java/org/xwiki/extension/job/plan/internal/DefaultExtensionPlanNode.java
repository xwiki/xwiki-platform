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
package org.xwiki.extension.job.plan.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.extension.version.VersionConstraint;

/**
 * A node in the extension plan tree.
 * 
 * @version $Id$
 */
public class DefaultExtensionPlanNode implements ExtensionPlanNode
{
    /**
     * @see #getAction()
     */
    private ExtensionPlanAction action;

    /**
     * @see #getChildren()
     */
    private Collection<ExtensionPlanNode> children;

    /**
     * @see #getVersionConstraint()
     */
    private VersionConstraint initialVersionConstraint;

    /**
     * @param node a node to copy
     */
    public DefaultExtensionPlanNode(ExtensionPlanNode node)
    {
        this(node.getAction(), node.getChildren(), node.getInitialVersionConstraint());
    }

    /**
     * @param action the action to perform for this node
     * @param initialVersionConstraint the initial version constraint before resolving the extension
     */
    public DefaultExtensionPlanNode(ExtensionPlanAction action, VersionConstraint initialVersionConstraint)
    {
        this(action, null, initialVersionConstraint);
    }

    /**
     * @param action the action to perform for this node
     * @param children the children of this node
     * @param initialVersionConstraint the initial version constraint before resolving the extension
     */
    public DefaultExtensionPlanNode(ExtensionPlanAction action, Collection<ExtensionPlanNode> children,
        VersionConstraint initialVersionConstraint)
    {
        this.action = action;
        if (children != null) {
            this.children = new ArrayList<ExtensionPlanNode>(children);
        } else {
            this.children = Collections.emptyList();
        }
        this.initialVersionConstraint = initialVersionConstraint;
    }

    @Override
    public ExtensionPlanAction getAction()
    {
        return this.action;
    }

    @Override
    public Collection<ExtensionPlanNode> getChildren()
    {
        return this.children;
    }

    @Override
    public VersionConstraint getInitialVersionConstraint()
    {
        return this.initialVersionConstraint;
    }
}
