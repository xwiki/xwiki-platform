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
package com.xpn.xwiki.store.hibernate.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Helper class to build Hibernate queries for a VersioningStore.
 *
 * @param <T> the type of element returned by the query
 * @version $Id$
 * @since 15.10.8
 * @since 16.2.0RC1
 */
@Unstable
public final class VersioningStoreQueryFactory<T>
{
    private static final String FIELD_ID = "id";

    private static final String FIELD_AUTHOR = "author";

    private static final String FIELD_DATE = "date";

    private static final String FIELD_DIFF = "diff";

    private static final String FIELD_DOCID = "docId";

    private static final String FIELD_VERSION1 = "version1";

    private static final String FIELD_VERSION2 = "version2";

    private final Root<XWikiRCSNodeInfo> root;

    private final CriteriaBuilder builder;

    private final CriteriaQuery<T> criteriaQuery;

    private final Session session;

    private Query<T> query;

    private VersioningStoreQueryFactory(Class<T> clazz, Session session)
    {
        this.session = session;
        this.builder = session.getCriteriaBuilder();
        this.criteriaQuery = this.builder.createQuery(clazz);
        this.root = this.criteriaQuery.from(XWikiRCSNodeInfo.class);
    }

    private void applyCriteria(final long id, RevisionCriteria criteria)
    {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(this.builder.equal(this.root.get(FIELD_ID).get(FIELD_DOCID), id));
        predicates.add(this.builder.isNotNull(this.root.get(FIELD_DIFF)));

        if (criteria != null) {
            if (!criteria.getAuthor().isEmpty()) {
                predicates.add(this.builder.equal(this.root.get(FIELD_AUTHOR), criteria.getAuthor()));
            }

            Date minDate = criteria.getMinDate();
            Date maxDate = criteria.getMaxDate();
            // Hibernate requires positive timestamps.
            if (minDate.getTime() < 0) {
                minDate = new Date(0);
            }
            // Most databases store timestamps as seconds, using integers.
            if (maxDate.getTime() > Integer.MAX_VALUE * 1000L) {
                maxDate = new Date(Integer.MAX_VALUE * 1000L);
            }
            predicates.add(this.builder.between(this.root.get(FIELD_DATE), minDate, maxDate));

            if (!criteria.getIncludeMinorVersions()) {
                // In this case, we keep only the highest minor version for each major version.
                Subquery<Integer> subQuery = this.criteriaQuery.subquery(Integer.class);
                Root<XWikiRCSNodeInfo> subRoot = subQuery.from(XWikiRCSNodeInfo.class);
                subQuery.select(this.builder.max(subRoot.get(FIELD_ID).get(FIELD_VERSION2)));
                subQuery.where(
                    this.builder.equal(subRoot.get(FIELD_ID).get(FIELD_VERSION1),
                        this.root.get(FIELD_ID).get(FIELD_VERSION1)),
                    this.builder.equal(subRoot.get(FIELD_ID).get(FIELD_DOCID),
                        this.root.get(FIELD_ID).get(FIELD_DOCID)));
                predicates.add(this.builder.equal(this.root.get(FIELD_ID).get(FIELD_VERSION2), subQuery));
            }
        }

        this.criteriaQuery.where(predicates.toArray(new Predicate[0]));
    }

    private void applyRange(Range range)
    {
        int start = range.getStart();
        int size = range.getSize();

        if (start > 0 && size != 0 || start == 0 && size > 0) {
            this.criteriaQuery.orderBy(this.builder.asc(this.root.get(FIELD_ID).get(FIELD_VERSION1)),
                this.builder.asc(this.root.get(FIELD_ID).get(FIELD_VERSION2)));
        } else if (size != 0) {
            this.criteriaQuery.orderBy(this.builder.desc(this.root.get(FIELD_ID).get(FIELD_VERSION1)),
                this.builder.desc(this.root.get(FIELD_ID).get(FIELD_VERSION2)));
            start = -start;
            size = -size;
        }

        this.query = this.session.createQuery(this.criteriaQuery);

        if (size > 0) {
            this.query.setFirstResult(start);
            this.query.setMaxResults(size);
        } else if (size < 0) {
            int newStart = Math.max(0, start + size);
            this.query.setFirstResult(newStart);
            this.query.setMaxResults(start - newStart);
        }
    }

    /**
     * Returns a query to completely delete the archive for a given document.
     *
     * @param session the hibernate session
     * @param id the id of the document
     * @return the created query
     */
    public static Query<?> getDeleteArchiveQuery(Session session, final long id)
    {
        return
            session
                .createQuery("delete from " + XWikiRCSNodeInfo.class.getName() + " where id." + FIELD_DOCID + '=' + ':'
                    + FIELD_DOCID)
                .setParameter(FIELD_DOCID, id);
    }

    /**
     * Returns a query to count the number of RCS nodes present in the archive of a given document.
     *
     * @param session the hibernate session
     * @param id the id of the document
     * @param criteria filtering criteria for the counted nodes (can be null)
     * @return the created query
     */
    public static Query<Long> getRCSNodeInfoCountQuery(Session session, final long id, RevisionCriteria criteria)
    {
        VersioningStoreQueryFactory<Long> queryBuilder = new VersioningStoreQueryFactory<>(Long.class, session);

        queryBuilder.criteriaQuery.select(queryBuilder.builder.count(queryBuilder.root));

        queryBuilder.applyCriteria(id, criteria);

        return session.createQuery(queryBuilder.criteriaQuery);
    }

    /**
     * Returns a query to fetch the RCS nodes present in the archive of a given document.
     *
     * @param session the hibernate session
     * @param id the id of the document
     * @param criteria filtering criteria for the counted nodes (can be null)
     * @return the created query
     */
    public static Query<XWikiRCSNodeInfo> getRCSNodeInfoQuery(Session session, final long id, RevisionCriteria criteria)
    {
        VersioningStoreQueryFactory<XWikiRCSNodeInfo> queryBuilder =
            new VersioningStoreQueryFactory<>(XWikiRCSNodeInfo.class, session);

        queryBuilder.criteriaQuery.select(queryBuilder.root);

        queryBuilder.applyCriteria(id, criteria);
        if (criteria != null && criteria.getRange() != null) {
            queryBuilder.applyRange(criteria.getRange());
            return queryBuilder.query;
        }

        return session.createQuery(queryBuilder.criteriaQuery);
    }

}
