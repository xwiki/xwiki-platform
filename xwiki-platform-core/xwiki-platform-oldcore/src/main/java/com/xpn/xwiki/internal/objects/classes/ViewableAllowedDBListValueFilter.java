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
package com.xpn.xwiki.internal.objects.classes;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryFilter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Filters the viewable values of a Database List property.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("viewableAllowedDBListPropertyValue")
@Singleton
public class ViewableAllowedDBListValueFilter implements QueryFilter
{
    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String filterStatement(String statement, String language)
    {
        // We only filter the results.
        return statement;
    }

    @Override
    public List filterResults(List results)
    {
        List<Object> filteredResults = new LinkedList<>();
        for (Object result : results) {
            if (result instanceof String) {
                DocumentReference documentReference = this.documentReferenceResolver.resolve((String) result);
                if (this.authorization.hasAccess(Right.VIEW, documentReference)) {
                    filteredResults.add(result);
                }
            } else if (result instanceof Object[]) {
                Object[] row = (Object[]) result;
                if (row.length > 0 && row[0] instanceof String) {
                    DocumentReference documentReference = this.documentReferenceResolver.resolve((String) row[0]);
                    if (this.authorization.hasAccess(Right.VIEW, documentReference)) {
                        // The document full name column was added just to be able to check view right. We can discard
                        // it now and return only the relevant columns.
                        filteredResults.add(row.length > 1 ? Arrays.copyOfRange(row, 1, row.length) : row);
                    }
                }
            }
        }
        return filteredResults;
    }
}
