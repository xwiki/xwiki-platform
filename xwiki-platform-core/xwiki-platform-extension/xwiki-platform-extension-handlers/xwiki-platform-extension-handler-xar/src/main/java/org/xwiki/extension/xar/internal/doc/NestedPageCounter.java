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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

/**
 * Counts nested pages.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component(roles = NestedPageCounter.class)
@Singleton
public class NestedPageCounter
{
    @Inject
    private Logger logger;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * @param rootReference the root page reference
     * @return the number of pages (excluding translations) nested under the specified root page
     */
    public long countNestedPages(DocumentReference rootReference)
    {
        String defaultDocumentName =
            this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
        if (!rootReference.getName().equals(defaultDocumentName)) {
            // Terminal page.
            return 0;
        }

        String isDefaultTranslation = "doc.translation = 0";
        String isDirectChild = "(doc.space = :space and doc.name <> :defaultDocumentName)";
        String isIndirectChild = "doc.space like :spacePrefix";
        try {
            Query query = this.queryManager.createQuery(
                "where " + isDefaultTranslation + " and (" + isDirectChild + " or " + isIndirectChild + ")", Query.HQL);
            query.bindValue("defaultDocumentName", defaultDocumentName);
            query.bindValue("space", this.localEntityReferenceSerializer.serialize(rootReference.getParent()));
            String spacePrefix = this.localEntityReferenceSerializer
                .serialize(new DocumentReference(String.valueOf('X'), rootReference.getLastSpaceReference()));
            spacePrefix = StringUtils.removeEnd(spacePrefix, String.valueOf('X'));
            query.bindValue("spacePrefix").literal(spacePrefix).anyChars();
            query.addFilter(this.countFilter);
            return query.<Long>execute().get(0);
        } catch (QueryException e) {
            this.logger.warn("Failed to count the nested pages inside [{}]. Root cause is: [{}]", rootReference,
                ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Stacktrace:", e);
            return 0;
        }
    }
}
