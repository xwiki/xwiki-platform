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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;

/**
 * This query filter is meant to be used with short HQL statements like this:
 * <dl>
 * <dt>Attachments ordered by date</dt>
 * <dd>order by attachment.date</dd>
 * <dt>Image attachments</dt>
 * <dd>where attachment.mimeType like 'image/%'</dd>
 * <dt>User profile attachments</dt>
 * <dd>, BaseObject as obj where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'</dd>
 * </dl>
 * The filter performs the following operations:
 * <ul>
 * <li>Extends the query statement to select the attachment file name column from the {@code XWikiAttachment} table and
 * to join the {@code XWikiAttachment} table with the {@code XWikiDocument} table.</li>
 * <li>Transforms the result in a list of {@link AttachmentReference}s.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Named("attachment")
@Singleton
public class AttachmentQueryFilter extends AbstractWhereQueryFilter
{
    private static final String FROM_DOC_TABLE = " XWikiDocument doc";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String filterStatement(String statement, String language)
    {
        // This method is called after completing the short statements so we expect to have the select and from clauses.
        if (Query.HQL.equals(language)) {
            int selectEnd = statement.indexOf(" from ");
            if (selectEnd > 0) {
                // Extend the select clause to include the attachment file name.
                StringBuilder filteredStatement = new StringBuilder(statement);
                filteredStatement.insert(selectEnd, ", attachment.filename");
                // Extend the from clause to include the attachments table.
                int fromDocTable = filteredStatement.indexOf(FROM_DOC_TABLE);
                if (fromDocTable > 0) {
                    filteredStatement.insert(fromDocTable + FROM_DOC_TABLE.length(), ", XWikiAttachment attachment");
                    // Extend the where clause to join the documents and attachments tables.
                    return insertWhereClause("doc.id = attachment.docId", filteredStatement.toString(), language);
                }
            }
        }
        return statement;
    }

    @Override
    public List filterResults(List results)
    {
        // We need at least 2 columns in the select: the document full name and the attachment file name.
        if (results.size() > 0 && results.get(0).getClass().isArray()) {
            List<AttachmentReference> attachmentReferences = new ArrayList<>();
            for (Object result : results) {
                Object[] actualResult = (Object[]) result;
                String documentFullName = String.valueOf(actualResult[0]);
                String attachmentFileName = String.valueOf(actualResult[1]);
                DocumentReference documentReference = this.documentReferenceResolver.resolve(documentFullName);
                attachmentReferences.add(new AttachmentReference(attachmentFileName, documentReference));
            }
            return attachmentReferences;
        }

        return results;
    }
}
