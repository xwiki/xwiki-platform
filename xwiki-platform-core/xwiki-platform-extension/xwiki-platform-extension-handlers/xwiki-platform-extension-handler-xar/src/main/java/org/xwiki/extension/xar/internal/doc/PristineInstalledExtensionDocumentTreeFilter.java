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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Excludes from the document tree the documents that are part of an installed extension and that have no customizations
 * (they have not been touched by the user). The result is the tree of:
 * <ul>
 * <li>content documents (documents that were created directly by the user or indirectly, by XWiki extensions on behalf
 * of the user)</li>
 * <li>and customized extension documents.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 11.10RC1
 */
@Component
@Singleton
@Named("pristineInstalledExtensionDocument")
public class PristineInstalledExtensionDocumentTreeFilter extends InstalledExtensionDocumentTreeFilter
{
    @Override
    protected Set<EntityReference> getExclusions(EntityReference parentReference)
    {
        // Exclude from the generic page tree the extension pages have not been customized and that don't have any
        // nested content pages or nested extension pages with customizations.
        return this.tree.getChildren(parentReference).stream().filter(this::hasNoCustomizationsAndContentPages)
            .collect(Collectors.toSet());
    }

    private boolean hasNoCustomizationsAndContentPages(DocumentReference documentReference)
    {
        return !this.tree.isCustomizedExtensionPage(documentReference)
            && this.tree.getNestedCustomizedExtensionPages(documentReference).isEmpty()
            && hasNoContentPages(documentReference);
    }
}
