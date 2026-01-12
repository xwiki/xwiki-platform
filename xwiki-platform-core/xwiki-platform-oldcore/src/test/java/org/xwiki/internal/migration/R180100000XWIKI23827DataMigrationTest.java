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
package org.xwiki.internal.migration;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PasswordProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import jakarta.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link R180100000XWIKI23827DataMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class R180100000XWIKI23827DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R180100000XWIKI23827DataMigration dataMigration;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private XWikiContext context;
    private XWiki wiki;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void migrate() throws QueryException, XWikiException, DataMigrationException
    {
        String passwordXClassQuery = "select doc.fullName, obj.id "
            + "from XWikiDocument doc, BaseObject as obj "
            + "where obj.className = doc.fullName and doc.xWikiClassXML like "
            + "'%<classType>com.xpn.xwiki.objects.classes.PasswordClass</classType>%' "
            + "order by doc.fullName";
        XWikiStoreInterface storeInterface = mock(XWikiStoreInterface.class);
        when(wiki.getStore()).thenReturn(storeInterface);
        QueryManager queryManager = mock(QueryManager.class);
        when(storeInterface.getQueryManager()).thenReturn(queryManager);

        List<String> passwordClassReferenceString = List.of(
            "XWiki.XWikiUser",
            "XWiki.ResetPassword",
            "MyApp.CustomClass",
            "AnotherApp.LotsOfFields"
        );

        Query myQuery = mock(Query.class);
        when(queryManager.createQuery(passwordXClassQuery, Query.HQL)).thenReturn(myQuery);

        List<Object> queryResults = new ArrayList<>();
        // 108 objects of XWiki.XWikiUser
        for (int i = 0; i < 108; i++) {
            queryResults.add(new Object[] { passwordClassReferenceString.get(0), Long.valueOf(i) });
        }
        // 13 objects of XWiki.ResetPassword
        for (int i = 0; i < 13; i++) {
            queryResults.add(new Object[] { passwordClassReferenceString.get(1), Long.valueOf(i) });
        }
        // 111 objects of MyApp.CustomClass
        for (int i = 0; i < 111; i++) {
            queryResults.add(new Object[] { passwordClassReferenceString.get(2), Long.valueOf(i) });
        }
        // 3 objects of AnotherApp.LotsOfFields
        for (int i = 0; i < 3; i++) {
            queryResults.add(new Object[] { passwordClassReferenceString.get(3), Long.valueOf(i) });
        }
        when(myQuery.execute()).thenReturn(queryResults);

        PasswordClass userPasswordProperty = mock(PasswordClass.class);
        when(userPasswordProperty.getName()).thenReturn("password");

        PasswordClass resetPasswordProperty = mock(PasswordClass.class);
        when(resetPasswordProperty.getName()).thenReturn("reset");

        PasswordClass myCustomClassPassword1 = mock(PasswordClass.class);
        when(myCustomClassPassword1.getName()).thenReturn("myCustomClassPassword1");

        PasswordClass myCustomClassPassword2 = mock(PasswordClass.class);
        when(myCustomClassPassword2.getName()).thenReturn("myCustomClassPassword2");

        List<Object> lotsOfFieldsPasswords = new ArrayList<>();
        for (int i = 0; i < 113; i++) {
            PasswordClass lotsOfFieldsPassword = mock(PasswordClass.class);
            when(lotsOfFieldsPassword.getName()).thenReturn("lotsOfFieldsPass_" + i);
            lotsOfFieldsPasswords.add(lotsOfFieldsPassword);
        }

        List<List<Object>> propertyLists = List.of(
            List.of(
                mock(StringClass.class),
                userPasswordProperty,
                mock(BaseClass.class)
            ),
            List.of(resetPasswordProperty),
            List.of(
                mock(StringClass.class),
                myCustomClassPassword1,
                mock(NumberClass.class),
                myCustomClassPassword2
            ),
            lotsOfFieldsPasswords
        );

        for (int i = 0; i < passwordClassReferenceString.size(); i++) {
            DocumentReference documentReference = mock(DocumentReference.class, "docRef_" + i);
            when(this.documentReferenceResolver.resolve(passwordClassReferenceString.get(i)))
                .thenReturn(documentReference);
            XWikiDocument doc = mock(XWikiDocument.class, "doc_" + i);
            BaseClass xclass = mock(BaseClass.class, "class_" + i);
            when(doc.getXClass()).thenReturn(xclass);
            when(wiki.getDocument(documentReference, context)).thenReturn(doc);
            when(xclass.getFieldList()).thenReturn(propertyLists.get(i));
        }

        XWikiHibernateStore hibernateStore = mock(XWikiHibernateStore.class);
        when(wiki.getHibernateStore()).thenReturn(hibernateStore);

        String selectProperties = "select prop from StringProperty as prop where ";

        // XWiki.XWikiUser: 108 objects with a single property
        // 2 loops, 1 for 100 objects, another one for 8 objects

        // loop 1
        StringBuilder xwikiUserWhereStatement1 = new StringBuilder();
        int beginLoop = 0;
        int endLoop = 100;
        for (int i = beginLoop; i < endLoop; i++) {
            xwikiUserWhereStatement1.append("(prop.id.id = :objectId_");
            xwikiUserWhereStatement1.append(i);
            xwikiUserWhereStatement1.append(" and prop.id.name = :property_0)");
            if (i < endLoop - 1) {
                xwikiUserWhereStatement1.append(" or ");
            }
        }

        Session session1 = mock(Session.class);
        OngoingStubbing<Session> sessionOngoingStubbing = when(hibernateStore.getSession(context)).thenReturn(session1);
        org.hibernate.query.Query<StringProperty> query1 = mock(org.hibernate.query.Query.class, "query1");
        when(session1.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + xwikiUserWhereStatement1, invocationOnMock.getArgument(0));
            return query1;
        });

        List<StringProperty> propertyListSession1 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession1 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("password");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("password");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession1.add(stringProperty);
            passwordPropertyListSession1.add(passwordProperty);
        }

        when(query1.getResultList()).thenReturn(propertyListSession1);

        // loop 2
        beginLoop = 100;
        endLoop = 108;
        StringBuilder xwikiUserWhereStatement2 = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            xwikiUserWhereStatement2.append("(prop.id.id = :objectId_");
            xwikiUserWhereStatement2.append(i);
            xwikiUserWhereStatement2.append(" and prop.id.name = :property_0)");
            if (i < endLoop - 1) {
                xwikiUserWhereStatement2.append(" or ");
            }
        }
        Session session2 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session2);
        org.hibernate.query.Query<StringProperty> query2 = mock(org.hibernate.query.Query.class, "query2");
        when(session2.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + xwikiUserWhereStatement2, invocationOnMock.getArgument(0));
            return query2;
        });

        List<StringProperty> propertyListSession2 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession2 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("password");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("password");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession2.add(stringProperty);
            passwordPropertyListSession2.add(passwordProperty);
        }

        when(query2.getResultList()).thenReturn(propertyListSession2);

        // XWiki.ResetPassword 13 objects with a single property -> 1 loop
        // loop 1
        beginLoop = 0;
        endLoop = 13;
        StringBuilder resetPasswordWhereStatement = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            resetPasswordWhereStatement.append("(prop.id.id = :objectId_");
            resetPasswordWhereStatement.append(i);
            resetPasswordWhereStatement.append(" and prop.id.name = :property_0)");
            if (i < endLoop - 1) {
                resetPasswordWhereStatement.append(" or ");
            }
        }
        Session session3 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session3);
        org.hibernate.query.Query<StringProperty> query3 = mock(org.hibernate.query.Query.class, "query3");
        when(session3.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + resetPasswordWhereStatement, invocationOnMock.getArgument(0));
            return query3;
        });

        List<StringProperty> propertyListSession3 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession3 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("reset");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("reset");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession3.add(stringProperty);
            passwordPropertyListSession3.add(passwordProperty);
        }

        when(query3.getResultList()).thenReturn(propertyListSession3);

        // MyApp.CustomClass 111 objects with 2 properties
        // 2 loops of 50 and 1 loop of 11

        // loop 1
        beginLoop = 0;
        endLoop = 50;
        StringBuilder customClassWhereStatement1 = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            customClassWhereStatement1.append("(prop.id.id = :objectId_");
            customClassWhereStatement1.append(i);
            customClassWhereStatement1.append(" and prop.id.name = :property_0) or (prop.id.id = :objectId_");
            customClassWhereStatement1.append(i);
            customClassWhereStatement1.append(" and prop.id.name = :property_1)");
            if (i < endLoop - 1) {
                customClassWhereStatement1.append(" or ");
            }
        }
        Session session4 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session4);
        org.hibernate.query.Query<StringProperty> query4 = mock(org.hibernate.query.Query.class, "query4");
        when(session4.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + customClassWhereStatement1, invocationOnMock.getArgument(0));
            return query4;
        });

        List<StringProperty> propertyListSession4 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession4 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("reset");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("reset");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession4.add(stringProperty);
            passwordPropertyListSession4.add(passwordProperty);
        }

        when(query4.getResultList()).thenReturn(propertyListSession4);

        // loop 2
        beginLoop = 50;
        endLoop = 100;
        StringBuilder customClassWhereStatement2 = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            customClassWhereStatement2.append("(prop.id.id = :objectId_");
            customClassWhereStatement2.append(i);
            customClassWhereStatement2.append(" and prop.id.name = :property_0) or (prop.id.id = :objectId_");
            customClassWhereStatement2.append(i);
            customClassWhereStatement2.append(" and prop.id.name = :property_1)");
            if (i < endLoop - 1) {
                customClassWhereStatement2.append(" or ");
            }
        }
        Session session5 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session5);
        org.hibernate.query.Query<StringProperty> query5 = mock(org.hibernate.query.Query.class, "query5");
        when(session5.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + customClassWhereStatement2, invocationOnMock.getArgument(0));
            return query5;
        });

        List<StringProperty> propertyListSession5 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession5 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("reset");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("reset");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession5.add(stringProperty);
            passwordPropertyListSession5.add(passwordProperty);
        }

        when(query5.getResultList()).thenReturn(propertyListSession5);

        // loop 3
        beginLoop = 100;
        endLoop = 111;
        StringBuilder customClassWhereStatement3 = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            customClassWhereStatement3.append("(prop.id.id = :objectId_");
            customClassWhereStatement3.append(i);
            customClassWhereStatement3.append(" and prop.id.name = :property_0) or (prop.id.id = :objectId_");
            customClassWhereStatement3.append(i);
            customClassWhereStatement3.append(" and prop.id.name = :property_1)");
            if (i < endLoop - 1) {
                customClassWhereStatement3.append(" or ");
            }
        }
        Session session6 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session6);
        org.hibernate.query.Query<StringProperty> query6 = mock(org.hibernate.query.Query.class, "query6");
        when(session6.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + customClassWhereStatement3, invocationOnMock.getArgument(0));
            return query6;
        });

        List<StringProperty> propertyListSession6 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession6 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("reset");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("reset");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession6.add(stringProperty);
            passwordPropertyListSession6.add(passwordProperty);
        }

        when(query6.getResultList()).thenReturn(propertyListSession6);

        // AnotherApp.LotsOfFields -> 3 objects of 113 fields
        // 3 loops, 1 per object
        // loop1

        beginLoop = 0;
        endLoop = 113; // the number of properties here

        StringBuilder lotsOfFieldsWhereStatement1 = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            lotsOfFieldsWhereStatement1.append("(prop.id.id = :objectId_0 and prop.id.name = :property_");
            lotsOfFieldsWhereStatement1.append(i);
            lotsOfFieldsWhereStatement1.append(")");
            if (i < endLoop - 1) {
                lotsOfFieldsWhereStatement1.append(" or ");
            }
        }
        Session session7 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session7);
        org.hibernate.query.Query<StringProperty> query7 = mock(org.hibernate.query.Query.class, "query7");
        when(session7.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + lotsOfFieldsWhereStatement1, invocationOnMock.getArgument(0));
            return query7;
        });

        List<StringProperty> propertyListSession7 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession7 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("reset");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("reset");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession7.add(stringProperty);
            passwordPropertyListSession7.add(passwordProperty);
        }

        when(query7.getResultList()).thenReturn(propertyListSession7);

        // loop2

        beginLoop = 0;
        endLoop = 113; // the number of properties here

        StringBuilder lotsOfFieldsWhereStatement2 = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            lotsOfFieldsWhereStatement2.append("(prop.id.id = :objectId_1 and prop.id.name = :property_");
            lotsOfFieldsWhereStatement2.append(i);
            lotsOfFieldsWhereStatement2.append(")");
            if (i < endLoop - 1) {
                lotsOfFieldsWhereStatement2.append(" or ");
            }
        }
        Session session8 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session8);
        org.hibernate.query.Query<StringProperty> query8 = mock(org.hibernate.query.Query.class, "query8");
        when(session8.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + lotsOfFieldsWhereStatement2, invocationOnMock.getArgument(0));
            return query8;
        });

        List<StringProperty> propertyListSession8 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession8 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("reset");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("reset");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession8.add(stringProperty);
            passwordPropertyListSession8.add(passwordProperty);
        }

        when(query8.getResultList()).thenReturn(propertyListSession8);

        // loop3

        beginLoop = 0;
        endLoop = 113; // the number of properties here

        StringBuilder lotsOfFieldsWhereStatement3 = new StringBuilder();
        for (int i = beginLoop; i < endLoop; i++) {
            lotsOfFieldsWhereStatement3.append("(prop.id.id = :objectId_2 and prop.id.name = :property_");
            lotsOfFieldsWhereStatement3.append(i);
            lotsOfFieldsWhereStatement3.append(")");
            if (i < endLoop - 1) {
                lotsOfFieldsWhereStatement3.append(" or ");
            }
        }
        Session session9 = mock(Session.class);
        sessionOngoingStubbing.thenReturn(session9);
        org.hibernate.query.Query<StringProperty> query9 = mock(org.hibernate.query.Query.class, "query9");
        when(session9.createQuery(any(), eq(StringProperty.class))).then(invocationOnMock -> {
            assertEquals(selectProperties + lotsOfFieldsWhereStatement3, invocationOnMock.getArgument(0));
            return query9;
        });

        List<StringProperty> propertyListSession9 = new ArrayList<>();
        List<PasswordProperty> passwordPropertyListSession9 = new ArrayList<>();
        for (int i = beginLoop; i < endLoop; i++) {
            StringProperty stringProperty = mock(StringProperty.class);
            when(stringProperty.getName()).thenReturn("reset");
            when(stringProperty.getId()).thenReturn(Long.valueOf(i));
            when(stringProperty.getValue()).thenReturn("mypassword_" + i);

            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName("reset");
            passwordProperty.setValue("mypassword_" + i);
            passwordProperty.setId(i);
            propertyListSession9.add(stringProperty);
            passwordPropertyListSession9.add(passwordProperty);
        }

        when(query9.getResultList()).thenReturn(propertyListSession9);

        this.dataMigration.migrate();

        // verify
        // XWiki.XWikiUser
        // loop 1
        verify(query1).setParameter("property_0", "password");
        for (int i = 0; i < 100; i++) {
            verify(query1).setParameter("objectId_" + i, Long.valueOf(i));
        }
        for (StringProperty stringProperty : propertyListSession1) {
            verify(session1).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession1) {
            verify(session1).save(passwordProperty);
        }

        // loop 2
        verify(query2).setParameter("property_0", "password");
        for (int i = 100; i < 108; i++) {
            verify(query2).setParameter("objectId_" + i, Long.valueOf(i));
        }
        for (StringProperty stringProperty : propertyListSession2) {
            verify(session2).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession2) {
            verify(session2).save(passwordProperty);
        }

        // XWiki.ResetPassword
        // loop 1
        verify(query3).setParameter("property_0", "reset");
        for (int i = 0; i < 13; i++) {
            verify(query3).setParameter("objectId_" + i, Long.valueOf(i));
        }
        for (StringProperty stringProperty : propertyListSession3) {
            verify(session3).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession3) {
            verify(session3).save(passwordProperty);
        }

        // MyApp.CustomClass
        // loop 1
        verify(query4).setParameter("property_0", "myCustomClassPassword1");
        verify(query4).setParameter("property_1", "myCustomClassPassword2");
        for (int i = 0; i < 50; i++) {
            verify(query4).setParameter("objectId_" + i, Long.valueOf(i));
        }
        for (StringProperty stringProperty : propertyListSession4) {
            verify(session4).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession4) {
            verify(session4).save(passwordProperty);
        }

        // loop 2
        verify(query5).setParameter("property_0", "myCustomClassPassword1");
        verify(query5).setParameter("property_1", "myCustomClassPassword2");
        for (int i = 50; i < 100; i++) {
            verify(query5).setParameter("objectId_" + i, Long.valueOf(i));
        }
        for (StringProperty stringProperty : propertyListSession5) {
            verify(session5).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession5) {
            verify(session5).save(passwordProperty);
        }

        // loop 3
        verify(query6).setParameter("property_0", "myCustomClassPassword1");
        verify(query6).setParameter("property_1", "myCustomClassPassword2");
        for (int i = 100; i < 111; i++) {
            verify(query6).setParameter("objectId_" + i, Long.valueOf(i));
        }
        for (StringProperty stringProperty : propertyListSession6) {
            verify(session6).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession6) {
            verify(session6).save(passwordProperty);
        }

        // AnotherApp.LotsOfFields
        verify(query7).setParameter("objectId_0", 0L);
        for (int i = 0; i < 113; i++) {
            verify(query7).setParameter("property_" + i, "lotsOfFieldsPass_" + i);
        }
        for (StringProperty stringProperty : propertyListSession7) {
            verify(session7).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession7) {
            verify(session7).save(passwordProperty);
        }

        // loop2
        verify(query8).setParameter("objectId_1", 1L);
        for (int i = 0; i < 113; i++) {
            verify(query8).setParameter("property_" + i, "lotsOfFieldsPass_" + i);
        }
        for (StringProperty stringProperty : propertyListSession8) {
            verify(session8).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession8) {
            verify(session8).save(passwordProperty);
        }

        // loop3
        verify(query9).setParameter("objectId_2", 2L);
        for (int i = 0; i < 113; i++) {
            verify(query9).setParameter("property_" + i, "lotsOfFieldsPass_" + i);
        }
        for (StringProperty stringProperty : propertyListSession9) {
            verify(session9).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession9) {
            verify(session9).save(passwordProperty);
        }
        verify(hibernateStore, times(9)).beginTransaction(context);
        verify(hibernateStore, times(9)).endTransaction(context, true);
        assertEquals(14, logCapture.size());
        assertEquals("[235] xobjects containing password properties related to [4] different xclass to migrate found.",
            logCapture.getMessage(0));
        assertEquals("Starting migration of [108] objects containing [1] password properties from xclass "
                + "[XWiki.XWikiUser].",
            logCapture.getMessage(1));
        assertEquals("[100] objects migrated on [108] for xclass [XWiki.XWikiUser].",
            logCapture.getMessage(2));
        assertEquals("[108] objects migrated on [108] for xclass [XWiki.XWikiUser].",
            logCapture.getMessage(3));
        assertEquals("Starting migration of [13] objects containing [1] password properties from xclass "
                + "[XWiki.ResetPassword].",
            logCapture.getMessage(4));
        assertEquals("[13] objects migrated on [13] for xclass [XWiki.ResetPassword].",
            logCapture.getMessage(5));
        assertEquals("Starting migration of [111] objects containing [2] password properties from xclass "
                + "[MyApp.CustomClass].",
            logCapture.getMessage(6));
        assertEquals("[50] objects migrated on [111] for xclass [MyApp.CustomClass].",
            logCapture.getMessage(7));
        assertEquals("[100] objects migrated on [111] for xclass [MyApp.CustomClass].",
            logCapture.getMessage(8));
        assertEquals("[111] objects migrated on [111] for xclass [MyApp.CustomClass].",
            logCapture.getMessage(9));
        assertEquals("Starting migration of [3] objects containing [113] password properties from xclass "
                + "[AnotherApp.LotsOfFields].",
            logCapture.getMessage(10));
        assertEquals("[1] objects migrated on [3] for xclass [AnotherApp.LotsOfFields].",
            logCapture.getMessage(11));
        assertEquals("[2] objects migrated on [3] for xclass [AnotherApp.LotsOfFields].",
            logCapture.getMessage(12));
        assertEquals("[3] objects migrated on [3] for xclass [AnotherApp.LotsOfFields].",
            logCapture.getMessage(13));
    }
}