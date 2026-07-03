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

import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.extension.job.plan.ExtensionPlanTree;
import org.xwiki.script.safe.SafeCollection;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to an extension plan tree.
 * 
 * @version $Id$
 * @since 4.1M1
 */
public class SafeExtensionPlanTree extends SafeCollection<ExtensionPlanNode, ExtensionPlanTree> implements
    ExtensionPlanTree
{
    /**
     * @param tree the wrapped tree
     * @param safeProvider the provider of instances safe for public scripts
     * @throws Exception failed to create a new SafeExtensionPlanTree
     */
    public SafeExtensionPlanTree(ExtensionPlanTree tree, ScriptSafeProvider< ? > safeProvider) throws Exception
    {
        super(tree, safeProvider, SafeExtensionPlanNode.class.getConstructor(ExtensionPlanNode.class,
            ScriptSafeProvider.class));
    }
}
