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
package org.xwiki.extension.xar.internal.doc;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.tree.AbstractEntityTreeFilter;

/**
 * Excludes from the document tree the documents that are part of an installed extension. The result is the tree of
 * content documents (documents that were created directly by the user or indirectly, by XWiki extensions on behalf of
 * the user).
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Singleton
@Named("installedExtensionDocument")
public class InstalledExtensionDocumentTreeFilter extends AbstractEntityTreeFilter
{
    @Inject
    protected InstalledExtensionDocumentTree tree;

    @Inject
    private NestedPageCounter counter;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Override
    public Set<EntityReference> getChildExclusions(EntityReference parentReference)
    {
        // Exclude from the generic page tree the pages that are part of an extension and that don't contain any nested
        // content pages.
        return this.tree.getChildren(getNodeReference(parentReference)).stream().filter(this::hasNoContentPages)
            .collect(Collectors.toSet());
    }

    protected boolean hasNoContentPages(DocumentReference documentReference)
    {
        // This doesn't cover the case when the user deletes some extension pages and creates some content pages in the
        // same space. We think it's acceptable because deleting extension pages without uninstalling the corresponding
        // extension is not recommended.
        return this.counter.countNestedPages(documentReference) <= this.tree.getNestedExtensionPages(documentReference)
            .size();
    }

    @Override
    public Set<EntityReference> getDescendantExclusions(EntityReference parentReference)
    {
        return this.tree.getNestedExtensionPages(getNodeReference(parentReference)).stream()
            .collect(Collectors.toSet());
    }

    protected EntityReference getNodeReference(EntityReference entityReference)
    {
        if (entityReference.getType() == EntityType.SPACE) {
            // Use the space home page instead because that's what we use in the extension document tree.
            String defaultDocumentName =
                defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
            return new DocumentReference(defaultDocumentName, new SpaceReference(entityReference));
        }
        return entityReference;
    }
}
