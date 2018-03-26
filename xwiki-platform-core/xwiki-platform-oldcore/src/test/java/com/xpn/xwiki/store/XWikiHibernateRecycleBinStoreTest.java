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
package com.xpn.xwiki.store;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.object.HasToString;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.SimpleExpression;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Unit tests for {@link XWikiHibernateRecycleBinStore}.
 * 
 * @version $Id$
 */
public class XWikiHibernateRecycleBinStoreTest extends AbstractXWikiHibernateStoreTest<XWikiRecycleBinStoreInterface>
{
    /**
     * A special component manager that mocks automatically all dependencies of the component under test.
     */
    @Rule
    public MockitoComponentMockingRule<XWikiRecycleBinStoreInterface> mocker =
        new MockitoComponentMockingRule<XWikiRecycleBinStoreInterface>(XWikiHibernateRecycleBinStore.class);

    @Override
    protected MockitoComponentMockingRule<XWikiRecycleBinStoreInterface> getMocker()
    {
        return mocker;
    }

    @Test
    public void getAllDeletedDocuments() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getFullName()).thenReturn("Space.Page");
        when(document.getLanguage()).thenReturn("ro");

        List<XWikiDeletedDocument> deletedVersions =
            Arrays.asList(mock(XWikiDeletedDocument.class, "v1"), mock(XWikiDeletedDocument.class, "v2"));

        Criteria criteria = mock(Criteria.class);
        when(criteria.list()).thenReturn(deletedVersions);
        when(session.createCriteria(XWikiDeletedDocument.class)).thenReturn(criteria);

        assertArrayEquals(deletedVersions.toArray(new XWikiDeletedDocument[2]), mocker.getComponentUnderTest()
            .getAllDeletedDocuments(document, xcontext, true));

        // Too bad the restrictions don't implement equals..
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("fullName=Space.Page"))));
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("language=ro"))));
        verify(criteria).addOrder(argThat(new HasToString<Order>(equalTo("date desc"))));
    }

    @Test
    public void getAllDeletedDocumentsWhenLanguageIsEmpty() throws Exception
    {
        Criteria criteria = mock(Criteria.class);
        when(session.createCriteria(XWikiDeletedDocument.class)).thenReturn(criteria);

        mocker.getComponentUnderTest().getAllDeletedDocuments(mock(XWikiDocument.class), xcontext, true);

        // Too bad the restrictions don't implement equals..
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("fullName=null"))));
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("language= or language is null"))));
    }
}
