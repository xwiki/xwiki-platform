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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.web.Utils;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UsedValuesListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class UsedValuesListQueryBuilderTest
{
    @Rule
    public MockitoComponentMockingRule<QueryBuilder<ListClass>> mocker =
        new MockitoComponentMockingRule<QueryBuilder<ListClass>>(UsedValuesListQueryBuilder.class);

    private QueryManager queryManager;

    private ContextualAuthorizationManager authorization;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private ListClass listClass;

    @Before
    public void configure() throws Exception
    {
        this.queryManager = this.mocker.getInstance(QueryManager.class);
        this.authorization = this.mocker.getInstance(ContextualAuthorizationManager.class);
        this.documentReferenceResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");

        DocumentReference oneReference = new DocumentReference("wiki", "Page", "one");
        when(this.documentReferenceResolver.resolve("alice")).thenReturn(oneReference);
        when(this.authorization.hasAccess(Right.VIEW, oneReference)).thenReturn(false);

        DocumentReference twoReference = new DocumentReference("wiki", "Page", "two");
        when(this.documentReferenceResolver.resolve("alice")).thenReturn(twoReference);
        when(this.authorization.hasAccess(Right.VIEW, twoReference)).thenReturn(true);

        BaseClass xclass = new BaseClass();
        xclass.setDocumentReference(new DocumentReference("apps", "Blog", "BlogPostClass"));

        Utils.setComponentManager(this.mocker);
        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(xclass.getDocumentReference())).thenReturn("Blog.BlogPostClass");

        this.listClass = new DBListClass();
        this.listClass.setName("category");
        this.listClass.setObject(xclass);
    }

    @Test
    public void buildForStringProperty() throws Exception
    {
        listClass.setMultiSelect(false);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(
            "select prop.value, count(*) as unfilterable0 " + "from BaseObject as obj, StringProperty as prop "
                + "where obj.className = :className and obj.name <> :templateName"
                + " and prop.id.id = obj.id and prop.id.name = :propertyName " + "group by prop.value "
                + "order by count(*) desc",
            Query.HQL)).thenReturn(query);

        assertSame(query, this.mocker.getComponentUnderTest().build(listClass));

        verify(query).bindValue("className", "Blog.BlogPostClass");
        verify(query).bindValue("propertyName", "category");
        verify(query).bindValue("templateName", "Blog.BlogPostTemplate");
        verify(query).setWiki("apps");
    }

    @Test
    public void buildForDBStringListProperty() throws Exception
    {
        listClass.setMultiSelect(true);
        listClass.setRelationalStorage(true);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("select listItem, count(*) as unfilterable0 "
            + "from BaseObject as obj, DBStringListProperty as prop join prop.list listItem "
            + "where obj.className = :className and obj.name <> :templateName"
            + " and prop.id.id = obj.id and prop.id.name = :propertyName " + "group by listItem "
            + "order by count(*) desc", Query.HQL)).thenReturn(query);

        assertSame(query, this.mocker.getComponentUnderTest().build(listClass));
    }

    @Test
    public void buildForStringListProperty() throws Exception
    {
        listClass.setMultiSelect(true);
        listClass.setRelationalStorage(false);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(
            "select prop.textValue, count(*) as unfilterable0 " + "from BaseObject as obj, StringListProperty as prop "
                + "where obj.className = :className and obj.name <> :templateName"
                + " and prop.id.id = obj.id and prop.id.name = :propertyName " + "group by prop.textValue "
                + "order by count(*) desc",
            Query.HQL)).thenReturn(query);

        assertSame(query, this.mocker.getComponentUnderTest().build(listClass));
    }
}
