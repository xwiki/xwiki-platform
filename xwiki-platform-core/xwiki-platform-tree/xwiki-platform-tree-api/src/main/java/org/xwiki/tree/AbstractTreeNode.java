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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * Base class for representing a node in a tree structure.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
public abstract class AbstractTreeNode implements TreeNode
{
    @Inject
    protected Logger logger;

    @Inject
    @Named("context")
    protected Provider<ComponentManager> contextComponentManagerProvider;

    private final Map<String, Object> properties = new HashMap<>();

    private final String type;

    protected AbstractTreeNode()
    {
        this(null);
    }

    protected AbstractTreeNode(String type)
    {
        this.type = type;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

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

    @SuppressWarnings("unchecked")
    protected Set<String> getExclusions()
    {
        return (Set<String>) getProperties().getOrDefault(PROPERTY_EXCLUSIONS, new HashSet<>());
    }

    protected Set<String> getExclusions(String parentNodeId)
    {
        // Initialize with global exclusions.
        Set<String> exclusions = new HashSet<>(getExclusions());
        // Add exclusions from filters.
        exclusions.addAll(getFilters().stream().flatMap(filter -> filter.getChildExclusions(parentNodeId).stream())
            .collect(Collectors.toSet()));
        return exclusions;
    }

    private List<TreeFilter> getFilters()
    {
        return ((List<?>) getProperties().getOrDefault(PROPERTY_FILTERS, Collections.emptyList())).stream()
            .map(this::getFilter).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private TreeFilter getFilter(Object filter)
    {
        if (filter instanceof TreeFilter) {
            return (TreeFilter) filter;
        } else if (filter instanceof String) {
            try {
                return this.contextComponentManagerProvider.get().getInstance(TreeFilter.class, (String) filter);
            } catch (ComponentLookupException e) {
                this.logger.warn("Skipping tree filter [{}]. Root cause is [{}].", filter,
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Stacktrace:", e);
            }
        }
        return null;
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

    protected TreeNode withSameProperties(TreeNode treeNode)
    {
        treeNode.getProperties().clear();
        treeNode.getProperties().putAll(getProperties());
        return treeNode;
    }
}
