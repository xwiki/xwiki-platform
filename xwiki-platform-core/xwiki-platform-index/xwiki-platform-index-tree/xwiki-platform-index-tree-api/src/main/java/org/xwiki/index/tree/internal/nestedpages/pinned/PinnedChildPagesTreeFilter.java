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
package org.xwiki.index.tree.internal.nestedpages.pinned;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.tree.AbstractEntityTreeFilter;

/**
 * Filter the pinned child pages from the list of child pages that are normally displayed in alphabetical order, because
 * they will be displayed before the rest of the child pages.
 * 
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Named("pinnedChildPages")
@Singleton
public class PinnedChildPagesTreeFilter extends AbstractEntityTreeFilter
{
    @Inject
    private PinnedChildPagesManager pinnedChildPagesManager;

    @Override
    public Set<EntityReference> getChildExclusions(EntityReference parentReference)
    {
        return pinnedChildPagesManager.getPinnedChildPages(parentReference).stream().collect(Collectors.toSet());
    }

    @Override
    public Set<EntityReference> getDescendantExclusions(EntityReference parentReference)
    {
        return Collections.emptySet();
    }
}
