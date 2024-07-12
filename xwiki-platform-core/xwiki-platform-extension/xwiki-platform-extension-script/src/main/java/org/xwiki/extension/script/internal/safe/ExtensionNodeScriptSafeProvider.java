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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.internal.tree.DefaultExtensionNode;
import org.xwiki.extension.tree.ExtensionNode;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide safe ExtensionNode.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Singleton
public class ExtensionNodeScriptSafeProvider implements ScriptSafeProvider<ExtensionNode>
{
    /**
     * The provider of instances safe for public scripts.
     */
    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider defaultSafeProvider;

    @Override
    public ExtensionNode get(ExtensionNode unsafe)
    {
        List<ExtensionNode> safeChildren;
        List<ExtensionNode> children = unsafe.getChildren();
        if (children.isEmpty()) {
            safeChildren = children;
        } else {
            safeChildren = new ArrayList<>(children.size());

            children.forEach(c -> safeChildren.add(get(c)));
        }

        Extension safeExtension = (Extension) this.defaultSafeProvider.get(unsafe.getExtension());

        return new DefaultExtensionNode(unsafe.getNamespace(), safeExtension, safeChildren);
    }
}
