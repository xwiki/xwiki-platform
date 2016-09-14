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
package org.xwiki.index.tree.internal.nestedpages.query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.query.QueryFilter;

/**
 * Transforms the results of a nested page query into a list of {@link DocumentReference}s.
 * 
 * @version $Id$
 * @since 8.3RC1, 7.4.5
 */
@Component
@Named("documentReferenceResolver/nestedPages")
@Singleton
public class DocumentReferenceResolverFilter implements QueryFilter
{
    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    @Named("current")
    protected SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Override
    public List<?> filterResults(@SuppressWarnings("rawtypes") List results)
    {
        String defaultDocumentName =
            this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
        List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
        for (Object result : results) {
            String reference = (String) ((Object[]) result)[0];
            boolean terminal = toBoolean(((Object[]) result)[1]);
            if (terminal) {
                documentReferences.add(this.currentDocumentReferenceResolver.resolve(reference));
            } else {
                SpaceReference spaceReference = this.currentSpaceReferenceResolver.resolve(reference);
                documentReferences.add(new DocumentReference(defaultDocumentName, spaceReference));
            }
        }
        return documentReferences;
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        return statement;
    }

    protected boolean toBoolean(Object value)
    {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else {
            return false;
        }
    }
}
