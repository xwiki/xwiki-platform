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
package org.xwiki.index.tree;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.internal.nestedpages.pinned.PinnedChildPagesManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service for index tree operations.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.5
 * @since 16.4.7
 */
@Unstable
@Component
@Singleton
@Named("index.tree")
public class IndexTreeScriptService implements ScriptService
{
    @Inject
    private PinnedChildPagesManager pinnedChildPagesManager;

    /**
     * Retrieve the list of pinned child pages of the given parent.
     * @param parent the document for which to find pinned child pages.
     * @return the ordered list of pinned child pages.
     */
    public List<DocumentReference> getPinnedChildPages(DocumentReference parent)
    {
        return this.pinnedChildPagesManager.getPinnedChildPages(parent);
    }
}
