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

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.OrderImpl;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.query.criteria.internal.predicate.BetweenPredicate;
import org.hibernate.query.criteria.internal.predicate.ComparisonPredicate;
import org.hibernate.query.criteria.internal.predicate.NullnessPredicate;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
public class VersioningStoreQueryFactoryTest
{
    @Mock
    private Session session;

    @Mock
    private CriteriaBuilderImpl builder;

    @Mock
    private Root<XWikiRCSNodeInfo> root;

    @Mock
    private CriteriaQuery<XWikiRCSNodeInfo> criteriaQueryNodeInfo;

    @Mock
    private CriteriaQuery<Long> criteriaQueryLong;

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

        when(this.builder.equal(any(), any(Object.class))).thenAnswer(i -> {
            ComparisonPredicate predicate = mock(ComparisonPredicate.class);
            when(predicate.getComparisonOperator()).thenReturn(ComparisonPredicate.ComparisonOperator.EQUAL);
            when(predicate.getLeftHandOperand()).thenReturn(i.getArgument(0));
            when(predicate.getRightHandOperand()).thenReturn(new LiteralExpression(null, i.getArgument(1)));
            return predicate;
        });

        when(this.builder.isNotNull(any())).thenAnswer(i -> {
            NullnessPredicate predicate = mock(NullnessPredicate.class);
            when(predicate.getOperand()).thenReturn(i.getArgument(0));
            when(predicate.isNegated()).thenReturn(true);
            return predicate;
        });

        when(this.builder.between(Mockito.<Path<Date>>any(), Mockito.<Date>any(), any())).thenAnswer(i -> {
            BetweenPredicate<?> predicate = mock(BetweenPredicate.class);
            when(predicate.getExpression()).thenReturn(i.getArgument(0));
            when(predicate.getLowerBound()).thenReturn(new LiteralExpression(null, i.getArgument(1)));
            when(predicate.getUpperBound()).thenReturn(new LiteralExpression(null, i.getArgument(2)));
            return predicate;
        });

        when(this.builder.asc(any())).thenAnswer(i -> new OrderImpl(i.getArgument(0), true));
        when(this.builder.desc(any())).thenAnswer(i -> new OrderImpl(i.getArgument(0), false));

        // With this mock we rely on toString() to check that the relative path is correct.
        when(this.root.get(anyString())).thenAnswer(i -> {
            Path<?> path = mock(Path.class);
            String pString = "mocked " + i.getArgument(0, String.class);
            when(path.toString()).thenReturn(pString);
            when(path.get(anyString())).thenAnswer(i3 -> {
                Path<?> path2 = mock(Path.class);
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

        ComparisonPredicate idPredicate = (ComparisonPredicate) predicates.get(0);
        LiteralExpression<Long> idExpression = (LiteralExpression<Long>) idPredicate.getRightHandOperand();
        assertEquals(ComparisonPredicate.ComparisonOperator.EQUAL, idPredicate.getComparisonOperator());
        assertEquals("mocked id.docId", idPredicate.getLeftHandOperand().toString());
        assertEquals(42L, idExpression.getLiteral());

        NullnessPredicate nonNullDiffPredicate = (NullnessPredicate) predicates.get(1);
        assertTrue(nonNullDiffPredicate.isNegated());
        assertEquals("mocked diff", nonNullDiffPredicate.getOperand().toString());
    }

    @Test
    void testRCSNodeInfoCountQueryWithAuthorCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoCountQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria("TestAuthor", true));
        verify(this.criteriaQueryLong).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(4, predicates.size());

        ComparisonPredicate authorPredicate = (ComparisonPredicate) predicates.get(2);
        LiteralExpression<String> authorExpression = (LiteralExpression<String>) authorPredicate.getRightHandOperand();
        assertEquals(ComparisonPredicate.ComparisonOperator.EQUAL, authorPredicate.getComparisonOperator());
        assertEquals("mocked author", authorPredicate.getLeftHandOperand().toString());
        assertEquals("TestAuthor", authorExpression.getLiteral());

        BetweenPredicate<Date> datePredicate = (BetweenPredicate<Date>) predicates.get(3);
        LiteralExpression<Date> dateLowerExpression = (LiteralExpression<Date>) datePredicate.getLowerBound();
        LiteralExpression<Date> dateUpperExpression = (LiteralExpression<Date>) datePredicate.getUpperBound();
        assertEquals("mocked date", datePredicate.getExpression().toString());
        assertEquals(new Date(0L), dateLowerExpression.getLiteral());
        assertEquals(new Date(Long.MAX_VALUE), dateUpperExpression.getLiteral());
    }

    @Test
    void testRCSNodeInfoCountQueryWithDateCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoCountQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria(new Period(1000L, 2000L), true));
        verify(this.criteriaQueryLong).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(3, predicates.size());

        BetweenPredicate<Date> datePredicate = (BetweenPredicate<Date>) predicates.get(2);
        LiteralExpression<Date> dateLowerExpression = (LiteralExpression<Date>) datePredicate.getLowerBound();
        LiteralExpression<Date> dateUpperExpression = (LiteralExpression<Date>) datePredicate.getUpperBound();
        assertEquals("mocked date", datePredicate.getExpression().toString());
        assertEquals(new Date(1000L), dateLowerExpression.getLiteral());
        assertEquals(new Date(2000L), dateUpperExpression.getLiteral());
    }

    @Test
    void testRCSNodeInfoQueryWithoutCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L, null);
        verify(this.criteriaQueryNodeInfo).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(2, predicates.size());

        ComparisonPredicate idPredicate = (ComparisonPredicate) predicates.get(0);
        LiteralExpression<Long> idExpression = (LiteralExpression<Long>) idPredicate.getRightHandOperand();
        assertEquals(ComparisonPredicate.ComparisonOperator.EQUAL, idPredicate.getComparisonOperator());
        assertEquals("mocked id.docId", idPredicate.getLeftHandOperand().toString());
        assertEquals(42L, idExpression.getLiteral());

        NullnessPredicate nonNullDiffPredicate = (NullnessPredicate) predicates.get(1);
        assertTrue(nonNullDiffPredicate.isNegated());
        assertEquals("mocked diff", nonNullDiffPredicate.getOperand().toString());
    }

    @Test
    void testRCSNodeInfoQueryWithAuthorCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria("TestAuthor", true));
        verify(this.criteriaQueryNodeInfo).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(4, predicates.size());

        ComparisonPredicate authorPredicate = (ComparisonPredicate) predicates.get(2);
        LiteralExpression<String> authorExpression = (LiteralExpression<String>) authorPredicate.getRightHandOperand();
        assertEquals(ComparisonPredicate.ComparisonOperator.EQUAL, authorPredicate.getComparisonOperator());
        assertEquals("mocked author", authorPredicate.getLeftHandOperand().toString());
        assertEquals("TestAuthor", authorExpression.getLiteral());

        BetweenPredicate<Date> datePredicate = (BetweenPredicate<Date>) predicates.get(3);
        LiteralExpression<Date> dateLowerExpression = (LiteralExpression<Date>) datePredicate.getLowerBound();
        LiteralExpression<Date> dateUpperExpression = (LiteralExpression<Date>) datePredicate.getUpperBound();
        assertEquals("mocked date", datePredicate.getExpression().toString());
        assertEquals(new Date(0L), dateLowerExpression.getLiteral());
        assertEquals(new Date(Long.MAX_VALUE), dateUpperExpression.getLiteral());
    }

    @Test
    void testRCSNodeInfoQueryWithDateCriteria()
    {
        VersioningStoreQueryFactory.getRCSNodeInfoQuery(this.session, 42L,
            new RevisionCriteriaFactory().createRevisionCriteria(new Period(1000L, 2000L), true));
        verify(this.criteriaQueryNodeInfo).where(this.predicatesCaptor.capture());

        List<Predicate> predicates = Arrays.asList(this.predicatesCaptor.getValue());
        assertEquals(3, predicates.size());

        BetweenPredicate<Date> datePredicate = (BetweenPredicate<Date>) predicates.get(2);
        LiteralExpression<Date> dateLowerExpression = (LiteralExpression<Date>) datePredicate.getLowerBound();
        LiteralExpression<Date> dateUpperExpression = (LiteralExpression<Date>) datePredicate.getUpperBound();
        assertEquals("mocked date", datePredicate.getExpression().toString());
        assertEquals(new Date(1000L), dateLowerExpression.getLiteral());
        assertEquals(new Date(2000L), dateUpperExpression.getLiteral());
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
