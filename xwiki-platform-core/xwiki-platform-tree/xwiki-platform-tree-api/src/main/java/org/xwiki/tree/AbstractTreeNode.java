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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.stability.Unstable;

/**
 * Base class for representing a node in a tree structure.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Unstable
public abstract class AbstractTreeNode implements TreeNode
{
    @Inject
    protected Logger logger;

    private final Map<String, Object> properties = new HashMap<String, Object>();

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        return Collections.emptyList();
    }

    @Override
    public int getChildCount(String nodeId)
    {
        return 0;
    }

    @Override
    public String getParent(String nodeId)
    {
        return null;
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return this.properties;
    }

    protected String getOrderBy()
    {
        return (String) getProperties().get(PROPERTY_ORDER_BY);
    }

    protected <E> List<E> subList(List<E> list, int offset, int limit)
    {
        if (list == null) {
            return Collections.emptyList();
        }

        int start = Math.min(Math.max(offset, 0), list.size());
        int end = Math.max(Math.min(start + limit, list.size()), start);
        return list.subList(start, end);
    }
}
