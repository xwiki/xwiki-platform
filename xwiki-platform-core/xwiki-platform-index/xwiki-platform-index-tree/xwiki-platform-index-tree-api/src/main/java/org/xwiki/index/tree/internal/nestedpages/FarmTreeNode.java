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
package org.xwiki.index.tree.internal.nestedpages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.tree.AbstractTreeNode;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * The farm tree node.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("farm")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class FarmTreeNode extends AbstractTreeNode
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        List<String> wikiIds = new ArrayList<String>(getWikiIds());
        List<String> children = new ArrayList<String>();
        for (String wikiId : subList(wikiIds, offset, limit)) {
            children.add("wiki:" + wikiId);
        }
        return children;
    }

    @Override
    public int getChildCount(String nodeId)
    {
        return getWikiIds().size();
    }

    private Collection<String> getWikiIds()
    {
        try {
            return this.wikiDescriptorManager.getAllIds();
        } catch (WikiManagerException e) {
            this.logger.warn("Failed to retrieve the list of wikis. Root cause [{}].",
                ExceptionUtils.getRootCauseMessage(e));
            return Collections.emptyList();
        }
    }
}
