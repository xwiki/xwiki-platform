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
package org.xwiki.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.stability.Unstable;

/**
 * Base class for representing a tree structure.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Unstable
public abstract class AbstractTree extends AbstractTreeNode implements Tree
{
    @Override
    public List<String> getPath(String nodeId)
    {
        List<String> path = new ArrayList<String>();
        String rootId = (String) getProperties().get(PROPERTY_ROOT);
        String parentId = nodeId;
        while (!StringUtils.isEmpty(parentId) && !parentId.equals(rootId)) {
            path.add(parentId);
            parentId = getParent(parentId);
        }
        Collections.reverse(path);
        return path;
    }
}
