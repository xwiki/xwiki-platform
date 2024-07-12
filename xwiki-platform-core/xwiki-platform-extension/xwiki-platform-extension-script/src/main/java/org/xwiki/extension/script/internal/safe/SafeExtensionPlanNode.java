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

import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.script.safe.AbstractSafeObject;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to an extension plan node.
 * 
 * @version $Id$
 * @since 4.0M2
 */
public class SafeExtensionPlanNode extends AbstractSafeObject<ExtensionPlanNode> implements ExtensionPlanNode
{
    /**
     * @see #getChildren()
     */
    private Collection<ExtensionPlanNode> wrappedChildren;

    /**
     * @param node the wrapped node
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeExtensionPlanNode(ExtensionPlanNode node, ScriptSafeProvider< ? > safeProvider)
    {
        super(node, safeProvider);
    }

    @Override
    public ExtensionPlanAction getAction()
    {
        return new SafeExtensionPlanAction(getWrapped().getAction(), this.safeProvider);
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
                    this.wrappedChildren.add(new SafeExtensionPlanNode(node, this.safeProvider));
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
