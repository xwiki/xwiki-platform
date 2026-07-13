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
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.UsersClass;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUsersQueryBuilder}.
 *
 * @version $Id$
 */
@ComponentTest
@SuppressWarnings("checkstyle:MultipleStringLiterals")
class DefaultUsersQueryBuilderTest
{
    @InjectMockComponents
    private DefaultUsersQueryBuilder builder;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("document")
    private QueryFilter documentFilter;

    @MockComponent
    @Named("viewable")
    private QueryFilter viewableFilter;

    private UsersClass usersClass;

    @BeforeEach
    void configure()
    {
        XWikiDocument ownerDocument = mock(XWikiDocument.class);
        when(ownerDocument.getDocumentReference()).thenReturn(new DocumentReference("tests", "Some", "Page"));
        this.usersClass = new UsersClass();
        this.usersClass.setOwnerDocument(ownerDocument);
    }

    private Query assertQuery(String statement) throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(query);

        assertSame(query, this.builder.build(this.usersClass));

        verify(query).addFilter(this.documentFilter);
        verify(query).addFilter(this.viewableFilter);
        verify(query).setWiki("tests");
        return query;
    }

    @Test
    void buildExcludesInactiveUsersByDefault() throws Exception
    {
        assertQuery("select doc.fullName as userReference,"
            + " firstName.value||' '||lastName.value as userName "
            + "from XWikiDocument doc, BaseObject obj, StringProperty firstName, StringProperty lastName"
            + ", IntegerProperty active"
            + " where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'"
            + " and obj.id = firstName.id.id and firstName.id.name = 'first_name'"
            + " and obj.id = lastName.id.id and lastName.id.name = 'last_name'"
            + " and doc.space = 'XWiki' "
            + "and obj.id = active.id.id and active.id.name = 'active' and active.value = 1 "
            + "order by lower(firstName.value), firstName.value, lower(lastName.value), lastName.value");
    }

    @Test
    void buildIncludesInactiveUsersWhenConfigured() throws Exception
    {
        this.usersClass.setIntValue(UsersClass.META_PROPERTY_INCLUDE_INACTIVE_USERS, 1);

        assertQuery("select doc.fullName as userReference,"
            + " firstName.value||' '||lastName.value as userName "
            + "from XWikiDocument doc, BaseObject obj, StringProperty firstName, StringProperty lastName "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'"
            + " and obj.id = firstName.id.id and firstName.id.name = 'first_name'"
            + " and obj.id = lastName.id.id and lastName.id.name = 'last_name'"
            + " and doc.space = 'XWiki' "
            + "order by lower(firstName.value), firstName.value, lower(lastName.value), lastName.value");
    }
}
