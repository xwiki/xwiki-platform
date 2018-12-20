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
package org.xwiki.query.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryFilter;

/**
 * Resolve the string reference from the first column of the query results into a {@link DocumentReference} relative to
 * the current document.
 * 
 * @version $Id$
 * @since 9.8
 */
@Component
@Named("document")
@Singleton
public class DocumentQueryFilter implements QueryFilter
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String filterStatement(String statement, String language)
    {
        // We don't filter the statement.
        return statement;
    }

    @Override
    public List filterResults(List results)
    {
        for (int i = 0; i < results.size(); i++) {
            Object result = results.get(i);
            if (result instanceof String) {
                results.set(i, this.documentReferenceResolver.resolve((String) result));
            } else if (result instanceof Object[]) {
                Object[] row = (Object[]) result;
                if (row.length > 0 && row[0] instanceof String) {
                    row[0] = this.documentReferenceResolver.resolve((String) row[0]);
                }
            }
        }
        return results;
    }
}
