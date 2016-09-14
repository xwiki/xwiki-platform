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
package org.xwiki.index.tree.internal.nestedspaces.query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.internal.nestedpages.query.DocumentReferenceResolverFilter;
import org.xwiki.model.reference.EntityReference;

/**
 * Transforms the results of a nested space query into a list of {@link EntityReference}s (with space or document
 * references).
 * 
 * @version $Id$
 * @since 8.3RC1, 7.4.5
 */
@Component
@Named("documentOrSpaceReferenceResolver/nestedSpaces")
@Singleton
public class DocumentOrSpaceReferenceResolverFilter extends DocumentReferenceResolverFilter
{
    @Override
    public List<?> filterResults(@SuppressWarnings("rawtypes") List results)
    {
        List<EntityReference> entityReferences = new ArrayList<EntityReference>();
        for (Object result : results) {
            String reference = (String) ((Object[]) result)[0];
            boolean isDocument = toBoolean(((Object[]) result)[1]);
            if (isDocument) {
                entityReferences.add(this.currentDocumentReferenceResolver.resolve(reference));
            } else {
                entityReferences.add(this.currentSpaceReferenceResolver.resolve(reference));
            }
        }

        return entityReferences;
    }
}
