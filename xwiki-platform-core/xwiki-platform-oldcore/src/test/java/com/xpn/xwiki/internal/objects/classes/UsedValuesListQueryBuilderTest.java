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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UsedValuesListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
class UsedValuesListQueryBuilderTest
{
    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("local")
    EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private HibernateStore hibernateStore;

    private ListClass listClass;

    @InjectMockComponents
    private UsedValuesListQueryBuilder usedValuesListQueryBuilder;

    @BeforeEach
    void before(MockitoComponentManager componentManager) throws Exception
    {
        DocumentReference oneReference = new DocumentReference("wiki", "Page", "one");
        when(this.documentReferenceResolver.resolve("alice")).thenReturn(oneReference);
        when(this.authorization.hasAccess(Right.VIEW, oneReference)).thenReturn(false);

        DocumentReference twoReference = new DocumentReference("wiki", "Page", "two");
        when(this.documentReferenceResolver.resolve("alice")).thenReturn(twoReference);
        when(this.authorization.hasAccess(Right.VIEW, twoReference)).thenReturn(true);

        BaseClass xclass = new BaseClass();
        xclass.setDocumentReference(new DocumentReference("apps", "Blog", "BlogPostClass"));

        Utils.setComponentManager(componentManager);
        when(this.localEntityReferenceSerializer.serialize(xclass.getDocumentReference()))
            .thenReturn("Blog.BlogPostClass");

        this.listClass = new DBListClass();
        this.listClass.setName("category");
        this.listClass.setObject(xclass);
    }

    @Test
    void buildForStringProperty() throws Exception
    {
        listClass.setMultiSelect(false);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);
        assertSame(query, this.usedValuesListQueryBuilder.build(listClass));

        verify(this.queryManager).createQuery("select prop.value, count(*) as unfilterable0 "
            + "from BaseObject as obj, StringProperty as prop "
            + "where obj.className = :className and obj.name <> :templateName and "
            + "prop.id.id = obj.id and prop.id.name = :propertyName "
            + "group by prop.value "
            + "order by unfilterable0 desc", Query.HQL);
        verify(query).bindValue("className", "Blog.BlogPostClass");
        verify(query).bindValue("propertyName", "category");
        verify(query).bindValue("templateName", "Blog.BlogPostTemplate");
        verify(query).setWiki("apps");
    }

    @Test
    void buildForStringPropertyWithOracle() throws Exception
    {
        listClass.setMultiSelect(false);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);
        when(this.hibernateStore.getDatabaseProductName()).thenReturn(DatabaseProduct.ORACLE);

        assertSame(query, this.usedValuesListQueryBuilder.build(listClass));

        verify(this.queryManager).createQuery("select prop.value, str(prop.value) as unfilterable1 "
            + "from BaseObject as obj, StringProperty as prop "
            + "where obj.className = :className and obj.name <> :templateName and "
            + "prop.id.id = obj.id and prop.id.name = :propertyName "
            + "order by unfilterable1 desc", Query.HQL);
        verify(query).bindValue("className", "Blog.BlogPostClass");
        verify(query).bindValue("propertyName", "category");
        verify(query).bindValue("templateName", "Blog.BlogPostTemplate");
        verify(query).setWiki("apps");
    }

    @Test
    void buildForDBStringListProperty() throws Exception
    {
        listClass.setMultiSelect(true);
        listClass.setRelationalStorage(true);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);

        assertSame(query, this.usedValuesListQueryBuilder.build(listClass));
        verify(this.queryManager).createQuery("select listItem, count(*) as unfilterable0 "
            + "from BaseObject as obj, DBStringListProperty as prop join prop.list listItem "
            + "where obj.className = :className and obj.name <> :templateName and "
            + "prop.id.id = obj.id and prop.id.name = :propertyName "
            + "group by listItem "
            + "order by unfilterable0 desc", Query.HQL);
    }

    @Test
    void buildForDBStringListPropertyWithOracle() throws Exception
    {
        listClass.setMultiSelect(true);
        listClass.setRelationalStorage(true);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);
        when(this.hibernateStore.getDatabaseProductName()).thenReturn(DatabaseProduct.ORACLE);

        assertSame(query, this.usedValuesListQueryBuilder.build(listClass));
        verify(this.queryManager).createQuery("select listItem, str(listItem) as unfilterable1 "
            + "from BaseObject as obj, DBStringListProperty as prop join prop.list listItem "
            + "where obj.className = :className and obj.name <> :templateName and "
            + "prop.id.id = obj.id and prop.id.name = :propertyName "
            + "order by unfilterable1 desc", Query.HQL);
    }

    @Test
    void buildForStringListProperty() throws Exception
    {
        listClass.setMultiSelect(true);
        listClass.setRelationalStorage(false);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);

        assertSame(query, this.usedValuesListQueryBuilder.build(listClass));
        verify(this.queryManager).createQuery("select prop.textValue, count(*) as unfilterable0 "
            + "from BaseObject as obj, StringListProperty as prop "
            + "where obj.className = :className and obj.name <> :templateName and "
            + "prop.id.id = obj.id and prop.id.name = :propertyName "
            + "group by prop.textValue "
            + "order by unfilterable0 desc", Query.HQL);
    }

    @Test
    void buildForStringListPropertyWithOracle() throws Exception
    {
        listClass.setMultiSelect(true);
        listClass.setRelationalStorage(false);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);
        when(this.hibernateStore.getDatabaseProductName()).thenReturn(DatabaseProduct.ORACLE);

        assertSame(query, this.usedValuesListQueryBuilder.build(listClass));
        verify(this.queryManager).createQuery("select prop.textValue, str(prop.textValue) as unfilterable1 "
            + "from BaseObject as obj, StringListProperty as prop "
            + "where obj.className = :className and obj.name <> :templateName and "
            + "prop.id.id = obj.id and prop.id.name = :propertyName "
            + "order by unfilterable1 desc", Query.HQL);
    }
}
