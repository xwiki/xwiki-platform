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

import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanNode;

/**
 * Provide a readonly access to an extension plan.
 * 
 * @version $Id$
 */
public class UnmodifiableExtensionPlan extends UnmodifiableJobStatus<ExtensionPlan> implements ExtensionPlan
{
    /**
     * @see #getTree()
     */
    private Collection<ExtensionPlanNode> wrappedTree;

    /**
     * @see #getActions()
     */
    private Collection<ExtensionPlanAction> wrappedActions;

    /**
     * @param plan the wrappeed plan
     */
    public UnmodifiableExtensionPlan(ExtensionPlan plan)
    {
        super(plan);
    }

    @Override
    public Collection<ExtensionPlanNode> getTree()
    {
        if (this.wrappedTree == null) {
            Collection<ExtensionPlanNode> nodes = getWrapped().getTree();
            if (nodes.isEmpty()) {
                this.wrappedTree = Collections.emptyList();
            } else {
                this.wrappedTree = new ArrayList<ExtensionPlanNode>(nodes.size());
                for (ExtensionPlanNode node : nodes) {
                    this.wrappedTree.add(new UnmodifiableExtensionPlanNode(node));
                }
            }
        }

        return this.wrappedTree;
    }

    @Override
    public Collection<ExtensionPlanAction> getActions()
    {
        if (this.wrappedActions == null) {
            Collection<ExtensionPlanAction> actions = getWrapped().getActions();
            if (actions.isEmpty()) {
                this.wrappedActions = Collections.emptyList();
            } else {
                this.wrappedActions = new ArrayList<ExtensionPlanAction>(actions.size());
                for (ExtensionPlanAction action : actions) {
                    this.wrappedActions.add(new UnmodifiableExtensionPlanAction(action));
                }
            }
        }

        return this.wrappedActions;
    }
}
