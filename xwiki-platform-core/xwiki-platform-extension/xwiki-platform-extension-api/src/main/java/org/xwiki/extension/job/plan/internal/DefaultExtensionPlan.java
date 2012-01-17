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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.internal.DefaultJobStatus;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * A plan of extension related actions to perform.
 * 
 * @param <R>
 * @version $Id$
 */
public class DefaultExtensionPlan<R extends ExtensionRequest> extends DefaultJobStatus<R> implements ExtensionPlan
{
    /**
     * @see #getTree()
     */
    private List<ExtensionPlanNode> tree = new ArrayList<ExtensionPlanNode>();

    /**
     * @see #getActions()
     */
    private Set<ExtensionPlanAction> actions;

    /**
     * @param request the request provided when started the job
     * @param id the unique id of the job
     * @param observationManager the observation manager component
     * @param loggerManager the logger manager component
     * @param tree the tree representation of the plan, it's not copied but taken as it it to allow filling it from
     *            outside
     */
    public DefaultExtensionPlan(R request, String id, ObservationManager observationManager,
        LoggerManager loggerManager, List<ExtensionPlanNode> tree)
    {
        super(request, id, observationManager, loggerManager);

        this.tree = tree;
    }

    /**
     * @param extensions the list of fill with actions
     * @param nodes of branch of the tree representation of the plan
     */
    private void fillExtensionActions(Set<ExtensionPlanAction> extensions, Collection<ExtensionPlanNode> nodes)
    {
        for (ExtensionPlanNode node : nodes) {
            fillExtensionActions(extensions, node.getChildren());

            extensions.add(node.getAction());
        }
    }

    @Override
    public Collection<ExtensionPlanNode> getTree()
    {
        return Collections.unmodifiableCollection(this.tree);
    }

    @Override
    public Collection<ExtensionPlanAction> getActions()
    {
        if (getState() != State.FINISHED) {
            Set<ExtensionPlanAction> extensions = new LinkedHashSet<ExtensionPlanAction>();
            fillExtensionActions(extensions, this.tree);

            return extensions;
        } else {
            if (this.actions == null) {
                this.actions = new LinkedHashSet<ExtensionPlanAction>();
                fillExtensionActions(this.actions, this.tree);
            }

            return Collections.unmodifiableCollection(this.actions);
        }
    }
}
