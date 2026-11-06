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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaRoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.RangeFactory;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.criteria.impl.RevisionCriteriaFactory;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VersioningStoreQueryFactory}.
 *
 * @version $Id$
 */
@ComponentTest
class VersioningStoreQueryFactoryTest
{
    @Mock
    private Session session;

    @Mock
    private HibernateCriteriaBuilder builder;

    @Mock
    private JpaRoot<XWikiRCSNodeInfo> root;

    @Mock
    private JpaCriteriaQuery<XWikiRCSNodeInfo> criteriaQueryNodeInfo;

    @Mock
    private JpaCriteriaQuery<Long> criteriaQueryLong;

    @Mock
    private Query<XWikiRCSNodeInfo> queryNodeInfo;

    @Mock
    private Query<Long> queryLong;

    @Mock
    private Query queryAny;

    @Captor
    private ArgumentCaptor<Predicate[]> predicatesCaptor;

    @Captor
    private ArgumentCaptor<Order[]> orderCaptor;

    @BeforeEach
    void setUp()
    {
        when(this.session.getCriteriaBuilder()).thenReturn(this.builder);

        when(this.builder.createQuery(XWikiRCSNodeInfo.class)).thenReturn(this.criteriaQueryNodeInfo);
        when(this.builder.createQuery(Long.class)).thenReturn(this.criteriaQueryLong);

        // The predicates and orders are described by their toString() to check what was created
        when(this.builder.equal(any(), any(Object.class)))
            .thenAnswer(i -> predicate(i.getArgument(0) + " = " + i.getArgument(1)));
        when(this.builder.isNotNull(any())).thenAnswer(i -> predicate(i.getArgument(0) + " is not null"));
        when(this.builder.between(Mockito.<Path<Date>>any(), Mockito.<Date>any(), any()))
            .thenAnswer(i -> predicate(i.getArgument(0) + " between " + i.getArgument(1, Date.class).getTime()
                + " and " + i.getArgument(2, Date.class).getTime()));
        when(this.builder.asc(Mockito.<Expression<?>>any())).thenAnswer(i -> order(i.getArgument(0), true));
        when(this.builder.desc(Mockito.<Expression<?>>any())).thenAnswer(i -> order(i.getArgument(0), false));

        // With this mock we rely on toString() to check that the relative path is correct.
        when(this.root.get(anyString())).thenAnswer(i -> {
            JpaPath<?> path = mock(JpaPath.class);
            String pString = "mocked " + i.getArgument(0, String.class);
            when(path.toString()).thenReturn(pString);
            when(path.get(anyString())).thenAnswer(i3 -> {
                JpaPath<?> path2 = mock(JpaPath.class);
                when(path2.toString()).thenReturn(pString + "." + i3.getArgument(0, String.class));
                return path2;
            });
            return path;
        });

        when(this.criteriaQueryNodeInfo.from(XWikiRCSNodeInfo.class)).thenReturn(this.root);
        when(this.criteriaQueryLong.from(XWikiRCSNodeInfo.class)).thenReturn(this.root);
        when(this.session.createQuery(this.criteriaQueryNodeInfo)).thenReturn(this.queryNodeInfo);
        when(this.session.createQuery(this.criteriaQueryLong)).thenReturn(this.queryLong);
        when(this.session.createQuery(anyString())).thenReturn(this.queryAny);
        when(this.queryAny.setParameter(anyString(), any())).thenReturn(this.queryAny);
    }

    private static JpaPredicate predicate(String description)
    {
        JpaPredicate predicate = mock(JpaPredicate.class);
        when(predicate.toString()).thenReturn(description);
        return predicate;
    }

    private static JpaOrder order(Expression<?> path, boolean ascending)
    {
        JpaOrder order = mock(JpaOrder.class);
        when(order.getExpression()).thenAnswer(i -> path);
        when(order.isAscending()).thenReturn(ascending);
        return order;
    }

    @Test
    void testDeleteArchiveQuery()
    {
        VersioningStoreQueryFactory.getDeleteArchiveQuery(this.session, 42L);
        verify(this.session).createQuery("delete from " + XWikiRCSNodeInfo.class.getName()
            + " where id.docId=:docId");
        verify(this.queryAny).setParameter("docId", 42L);
    }

    @Test
    void testRCSNodeInfoCountQueryWithoutCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoCountQuery(this.session, 42L, null);
        verify(this.criteriaQueryLong).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(2, predicates.size());

        assertEquals("mocked id.docId = 42", predicates.get(0).toString());
        assertEquals("mocked diff is not null", predicates.get(1).toString());
    }

    @Test
    void testRCSNodeInfoCountQueryWithAuthorCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoCountQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria("TestAuthor", true));
        verify(this.criteriaQueryLong).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(4, predicates.size());

        assertEquals("mocked author = TestAuthor", predicates.get(2).toString());
        assertEquals("mocked date between 0 and " + (Integer.MAX_VALUE * 1000L), predicates.get(3).toString());
    }

    @Test
    void testRCSNodeInfoCountQueryWithDateCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoCountQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria(new Period(1000L, 2000L), true));
        verify(this.criteriaQueryLong).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(3, predicates.size());

        assertEquals("mocked date between 1000 and 2000", predicates.get(2).toString());
    }

    @Test
    void testRCSNodeInfoQueryWithoutCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L, null);
        verify(this.criteriaQueryNodeInfo).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(2, predicates.size());

        assertEquals("mocked id.docId = 42", predicates.get(0).toString());
        assertEquals("mocked diff is not null", predicates.get(1).toString());
    }

    @Test
    void testRCSNodeInfoQueryWithAuthorCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria("TestAuthor", true));
        verify(this.criteriaQueryNodeInfo).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(4, predicates.size());

        assertEquals("mocked author = TestAuthor", predicates.get(2).toString());
        assertEquals("mocked date between 0 and " + (Integer.MAX_VALUE * 1000L), predicates.get(3).toString());
    }

    @Test
    void testRCSNodeInfoQueryWithDateCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria(new Period(1000L, 2000L), true));
        verify(this.criteriaQueryNodeInfo).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(3, predicates.size());

        assertEquals("mocked date between 1000 and 2000", predicates.get(2).toString());
    }

    static Stream<Arguments> rangesProvider()
    {
        return Stream.of(
            // Range 0 / 10 : will return the first 10 elements
            Arguments.of(0, 10, 0, 10, true),
            // Range -2 / 10 : will return the last 2 elements (not enough elements for 10)
            Arguments.of(-2, 10, 0, 2, false),
            // Range -2 / -10 : will return the last 10 elements after skipping the last 2
            Arguments.of(-2, -10, 2, 10, false),
            // Range 2 / 10 : will return the first 10 elements after skipping the first 2
            Arguments.of(2, 10, 2, 10, true),
            // Range 2 / -10 : will return the first 2 elements (not enough elements for 10)
            Arguments.of(2, -10, 0, 2, true),
            // Range 0 / -10 : will return the last 10 elements
            Arguments.of(0, -10, 0, 10, false));
    }

    @ParameterizedTest
    @MethodSource("rangesProvider")
    void testRCSNodeInfoQueryWithRange(int startRange, int sizeRange, int start, int size, boolean ascending)
    {
        RevisionCriteria criteria = new RevisionCriteriaFactory().createRevisionCriteria(true);
        criteria.setRange(RangeFactory.createRange(startRange, sizeRange));
        VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L, criteria);
        verify(this.criteriaQueryNodeInfo).orderBy(this.orderCaptor.capture());

        List<Order> orders = Arrays.asList(this.orderCaptor.getValue());
        assertEquals(2, orders.size());

        verify(this.criteriaQueryNodeInfo).orderBy(this.orderCaptor.capture());
        assertEquals("mocked id.version1", orders.get(0).getExpression().toString());
        assertEquals(ascending, orders.get(0).isAscending());
        assertEquals("mocked id.version2", orders.get(1).getExpression().toString());
        assertEquals(ascending, orders.get(1).isAscending());
        verify(this.queryNodeInfo).setFirstResult(start);
        verify(this.queryNodeInfo).setMaxResults(size);
    }

    @Test
    void testRCSNodeInfoQueryWithAllRange()
    {
        // When the size of the range is 0 (ALL), there should be no filtering done.
        RevisionCriteria criteria = new RevisionCriteriaFactory().createRevisionCriteria(true);
        for (int start : new int[] {-10, 0, 10}) {
            criteria.setRange(RangeFactory.createRange(start, 0));
            VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L, criteria);
        }
        verify(this.criteriaQueryNodeInfo, never()).orderBy(Mockito.<Order[]>any());
        verify(this.queryNodeInfo, never()).setFirstResult(anyInt());
        verify(this.queryNodeInfo, never()).setMaxResults(anyInt());
    }
}
