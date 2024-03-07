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
package org.xwiki.extension.script.internal.safe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanTree;
import org.xwiki.job.internal.script.safe.SafeJobStatus;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to an extension plan.
 * 
 * @param <J> the type of the job status
 * @version $Id$
 * @since 4.0M2
 */
public class SafeExtensionPlan<J extends ExtensionPlan> extends SafeJobStatus<J> implements ExtensionPlan
{
    /**
     * @see #getActions()
     */
    private Collection<ExtensionPlanAction> wrappedActions;

    /**
     * @param plan the wrapped plan
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeExtensionPlan(J plan, ScriptSafeProvider<?> safeProvider)
    {
        super(plan, safeProvider);
    }

    @Override
    public ExtensionPlanTree getTree()
    {
        try {
            return new SafeExtensionPlanTree(getWrapped().getTree(), this.safeProvider);
        } catch (Exception e) {
            // should never happen
            return null;
        }
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
                    this.wrappedActions.add(new SafeExtensionPlanAction(action, this.safeProvider));
                }
            }
        }

        return this.wrappedActions;
    }
}
