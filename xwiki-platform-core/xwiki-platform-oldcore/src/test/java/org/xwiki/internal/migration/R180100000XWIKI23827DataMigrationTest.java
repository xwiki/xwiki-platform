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
import java.util.stream.IntStream;
import java.util.stream.LongStream;

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
        String passwordXClassQuery = "select doc.fullName"
            + "from XWikiDocument doc"
            + "where doc.xWikiClassXML like "
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
        when(myQuery.execute()).thenReturn(new ArrayList<>(passwordClassReferenceString));

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

        List<Integer> resultsNumbers = List.of(
            108,
            13,
            111,
            3
        );

        String objectIdsQuery = "select obj.id "
            + "from BaseObject as obj "
            + "where obj.className = :className "
            + "order by obj.id";
        Query classObjectIdQuery = mock(Query.class);
        OngoingStubbing<Query> queryOngoingStubbing =
            when(queryManager.createQuery(objectIdsQuery, Query.HQL)).thenReturn(classObjectIdQuery);

        for (int i = 0; i < passwordClassReferenceString.size(); i++) {
            String className = passwordClassReferenceString.get(i);
            DocumentReference documentReference = mock(DocumentReference.class, "docRef_" + i);
            when(this.documentReferenceResolver.resolve(className))
                .thenReturn(documentReference);
            XWikiDocument doc = mock(XWikiDocument.class, "doc_" + i);
            BaseClass xclass = mock(BaseClass.class, "class_" + i);
            when(doc.getXClass()).thenReturn(xclass);
            when(wiki.getDocument(documentReference, context)).thenReturn(doc);
            when(xclass.getFieldList()).thenReturn(propertyLists.get(i));

            Query boundClassObjectIdQuery = mock(Query.class);
            when(classObjectIdQuery.bindValue("className", className)).thenReturn(boundClassObjectIdQuery);
            List<Long> objectIdQueryResult = new ArrayList<>();
            for (int j = 0; j < resultsNumbers.get(i); j++) {
                objectIdQueryResult.add(Long.valueOf(j));
            }
            when(boundClassObjectIdQuery.execute()).thenReturn(new ArrayList<>(objectIdQueryResult));
            classObjectIdQuery = mock(Query.class);
            queryOngoingStubbing = queryOngoingStubbing.thenReturn(classObjectIdQuery);
        }

        XWikiHibernateStore hibernateStore = mock(XWikiHibernateStore.class);
        when(wiki.getHibernateStore()).thenReturn(hibernateStore);

        String selectProperties = "select prop from StringProperty as prop "
            + "where prop.id.name in :propNames and prop.id.id in :objectIds";

        // XWiki.XWikiUser: 108 objects with a single property
        // 2 loops, 1 for 100 objects, another one for 8 objects
        List<String> propertyNamesUser = List.of(
            "password"
        );

        // loop 1
        int beginLoop = 0;
        int endLoop = 100;
        List<Long> objectIdsSession1 = LongStream.range(beginLoop, endLoop).boxed().toList();

        Session session1 = mock(Session.class);
        OngoingStubbing<Session> sessionOngoingStubbing = when(hibernateStore.getSession(context)).thenReturn(session1);
        org.hibernate.query.Query<StringProperty> query1 = mock(org.hibernate.query.Query.class, "query1");
        when(session1.createQuery(selectProperties, StringProperty.class)).thenReturn(query1);

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
        List<Long> objectIdsSession2 = LongStream.range(beginLoop, endLoop).boxed().toList();
        Session session2 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session2);
        org.hibernate.query.Query<StringProperty> query2 = mock(org.hibernate.query.Query.class, "query2");
        when(session2.createQuery(selectProperties, StringProperty.class)).thenReturn(query2);

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
        List<String> propertyNamesReset = List.of("reset");
        // loop 1
        beginLoop = 0;
        endLoop = 13;
        List<Long> objectIdsSession3 = LongStream.range(beginLoop, endLoop).boxed().toList();
        Session session3 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session3);
        org.hibernate.query.Query<StringProperty> query3 = mock(org.hibernate.query.Query.class, "query3");
        when(session3.createQuery(selectProperties, StringProperty.class)).thenReturn(query3);

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
        List<String> propertyNamesCustom = List.of(
            "myCustomClassPassword1",
            "myCustomClassPassword2"
        );

        // 2 loops of 50 and 1 loop of 11

        // loop 1
        beginLoop = 0;
        endLoop = 50;
        List<Long> objectIdsSession4 = LongStream.range(beginLoop, endLoop).boxed().toList();
        Session session4 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session4);
        org.hibernate.query.Query<StringProperty> query4 = mock(org.hibernate.query.Query.class, "query4");
        when(session4.createQuery(selectProperties, StringProperty.class)).thenReturn(query4);

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
        List<Long> objectIdsSession5 = LongStream.range(beginLoop, endLoop).boxed().toList();
        Session session5 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session5);
        org.hibernate.query.Query<StringProperty> query5 = mock(org.hibernate.query.Query.class, "query5");
        when(session5.createQuery(selectProperties, StringProperty.class)).thenReturn(query5);

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
        List<Long> objectIdsSession6 = LongStream.range(beginLoop, endLoop).boxed().toList();
        Session session6 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session6);
        org.hibernate.query.Query<StringProperty> query6 = mock(org.hibernate.query.Query.class, "query6");
        when(session6.createQuery(selectProperties, StringProperty.class)).thenReturn(query6);

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
        List<String> propertyNamesLotsOfFields = IntStream
            .range(0, 113)
            .boxed()
            .map(number -> String.format("lotsOfFieldsPass_%s", number))
            .toList();
        // 3 loops, 1 per object
        // loop1

        beginLoop = 0;
        endLoop = 113;
        Session session7 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session7);
        org.hibernate.query.Query<StringProperty> query7 = mock(org.hibernate.query.Query.class, "query7");
        when(session7.createQuery(selectProperties, StringProperty.class)).thenReturn(query7);

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
        Session session8 = mock(Session.class);
        sessionOngoingStubbing = sessionOngoingStubbing.thenReturn(session8);
        org.hibernate.query.Query<StringProperty> query8 = mock(org.hibernate.query.Query.class, "query8");
        when(session8.createQuery(selectProperties, StringProperty.class)).thenReturn(query8);

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

        Session session9 = mock(Session.class);
        sessionOngoingStubbing.thenReturn(session9);
        org.hibernate.query.Query<StringProperty> query9 = mock(org.hibernate.query.Query.class, "query9");
        when(session9.createQuery(selectProperties, StringProperty.class)).thenReturn(query9);

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
        verify(query1).setParameter("propNames", propertyNamesUser);
        verify(query1).setParameter("objectIds", objectIdsSession1);
        for (StringProperty stringProperty : propertyListSession1) {
            verify(session1).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession1) {
            verify(session1).save(passwordProperty);
        }

        // loop 2
        verify(query2).setParameter("propNames", propertyNamesUser);
        verify(query2).setParameter("objectIds", objectIdsSession2);
        for (StringProperty stringProperty : propertyListSession2) {
            verify(session2).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession2) {
            verify(session2).save(passwordProperty);
        }

        // XWiki.ResetPassword
        // loop 1
        verify(query3).setParameter("propNames", propertyNamesReset);
        verify(query3).setParameter("objectIds", objectIdsSession3);
        for (StringProperty stringProperty : propertyListSession3) {
            verify(session3).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession3) {
            verify(session3).save(passwordProperty);
        }

        // MyApp.CustomClass
        // loop 1
        verify(query4).setParameter("propNames", propertyNamesCustom);
        verify(query4).setParameter("objectIds", objectIdsSession4);
        for (StringProperty stringProperty : propertyListSession4) {
            verify(session4).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession4) {
            verify(session4).save(passwordProperty);
        }

        // loop 2
        verify(query5).setParameter("propNames", propertyNamesCustom);
        verify(query5).setParameter("objectIds", objectIdsSession5);
        for (StringProperty stringProperty : propertyListSession5) {
            verify(session5).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession5) {
            verify(session5).save(passwordProperty);
        }

        // loop 3
        verify(query6).setParameter("propNames", propertyNamesCustom);
        verify(query6).setParameter("objectIds", objectIdsSession6);
        for (StringProperty stringProperty : propertyListSession6) {
            verify(session6).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession6) {
            verify(session6).save(passwordProperty);
        }

        // AnotherApp.LotsOfFields
        verify(query7).setParameter("propNames", propertyNamesLotsOfFields);
        verify(query7).setParameter("objectIds", List.of(0L));
        for (StringProperty stringProperty : propertyListSession7) {
            verify(session7).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession7) {
            verify(session7).save(passwordProperty);
        }

        // loop2
        verify(query8).setParameter("propNames", propertyNamesLotsOfFields);
        verify(query8).setParameter("objectIds", List.of(1L));
        for (StringProperty stringProperty : propertyListSession8) {
            verify(session8).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession8) {
            verify(session8).save(passwordProperty);
        }

        // loop3
        verify(query9).setParameter("propNames", propertyNamesLotsOfFields);
        verify(query9).setParameter("objectIds", List.of(2L));
        for (StringProperty stringProperty : propertyListSession9) {
            verify(session9).delete(stringProperty);
        }
        for (PasswordProperty passwordProperty : passwordPropertyListSession9) {
            verify(session9).save(passwordProperty);
        }
        verify(hibernateStore, times(9)).beginTransaction(context);
        verify(hibernateStore, times(9)).endTransaction(context, true);
        assertEquals(18, logCapture.size());
        assertEquals("[4] different xclass found containing password properties values to migrate found.",
            logCapture.getMessage(0));
        assertEquals("[108] objects to migrate found for xclass [XWiki.XWikiUser].",
            logCapture.getMessage(1));
        assertEquals("Starting migration of [108] objects containing [1] password properties from xclass "
                + "[XWiki.XWikiUser].",
            logCapture.getMessage(2));
        assertEquals("[100] objects migrated on [108] for xclass [XWiki.XWikiUser].",
            logCapture.getMessage(3));
        assertEquals("[108] objects migrated on [108] for xclass [XWiki.XWikiUser].",
            logCapture.getMessage(4));
        assertEquals("[13] objects to migrate found for xclass [XWiki.ResetPassword].",
            logCapture.getMessage(5));
        assertEquals("Starting migration of [13] objects containing [1] password properties from xclass "
                + "[XWiki.ResetPassword].",
            logCapture.getMessage(6));
        assertEquals("[13] objects migrated on [13] for xclass [XWiki.ResetPassword].",
            logCapture.getMessage(7));
        assertEquals("[111] objects to migrate found for xclass [MyApp.CustomClass].",
            logCapture.getMessage(8));
        assertEquals("Starting migration of [111] objects containing [2] password properties from xclass "
                + "[MyApp.CustomClass].",
            logCapture.getMessage(9));
        assertEquals("[50] objects migrated on [111] for xclass [MyApp.CustomClass].",
            logCapture.getMessage(10));
        assertEquals("[100] objects migrated on [111] for xclass [MyApp.CustomClass].",
            logCapture.getMessage(11));
        assertEquals("[111] objects migrated on [111] for xclass [MyApp.CustomClass].",
            logCapture.getMessage(12));
        assertEquals("[3] objects to migrate found for xclass [AnotherApp.LotsOfFields].",
            logCapture.getMessage(13));
        assertEquals("Starting migration of [3] objects containing [113] password properties from xclass "
                + "[AnotherApp.LotsOfFields].",
            logCapture.getMessage(14));
        assertEquals("[1] objects migrated on [3] for xclass [AnotherApp.LotsOfFields].",
            logCapture.getMessage(15));
        assertEquals("[2] objects migrated on [3] for xclass [AnotherApp.LotsOfFields].",
            logCapture.getMessage(16));
        assertEquals("[3] objects migrated on [3] for xclass [AnotherApp.LotsOfFields].",
            logCapture.getMessage(17));
    }
}