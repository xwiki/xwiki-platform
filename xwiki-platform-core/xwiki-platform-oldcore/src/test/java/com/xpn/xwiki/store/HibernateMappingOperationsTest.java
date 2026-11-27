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

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.doc.rcs.XWikiPatch;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Run the Hibernate operations used by the store on a real (in-memory HSQLDB) database with the XWiki mapping, to
 * validate behaviors that can't be checked with a mocked session.
 *
 * @version $Id$
 */
class HibernateMappingOperationsTest
{
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void beforeAll()
    {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbc.JDBCDriver");
        configuration.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:mappingoperations;shutdown=true");
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.connection.password", "");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.addResource("xwiki.hbm.xml");

        sessionFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void afterAll()
    {
        sessionFactory.close();
    }

    private static void write(Consumer<Session> operation)
    {
        sessionFactory.inTransaction(operation);
    }

    private static <T> T read(Function<Session, T> operation)
    {
        return sessionFactory.fromSession(operation);
    }

    private static long persistDeletedAttachment()
    {
        DeletedAttachment deletedAttachment =
            new DeletedAttachment(1, "Space.Page", "file.txt", "hibernate", "XWiki.Alice", new Date(), null);
        write(session -> session.persist(deletedAttachment));

        return deletedAttachment.getId();
    }

    private static StringProperty newStringProperty(long id, String name, String value)
    {
        StringProperty property = new StringProperty();
        property.setId(id);
        property.setName(name);
        property.setValue(value);

        return property;
    }

    private static <T extends BaseProperty> T load(T property)
    {
        return read(session -> {
            // The property is its own identifier
            session.load(property, (Object) property);

            return property;
        });
    }

    private static StringProperty loadStringProperty(long id, String name)
    {
        return load(newStringProperty(id, name, ""));
    }

    private static String getStoredClassType(long id)
    {
        return read(session -> session
            .createNativeQuery("select XWP_CLASSTYPE from xwikiproperties where XWP_ID = :id", String.class)
            .setParameter("id", id).uniqueResult());
    }

    @Test
    void updateDetachedPropertyIdentifiedByItself()
    {
        write(session -> session.persist(newStringProperty(1, "prop", "value1")));

        // A new instance with the same identifier, like the store does when saving a modified document
        write(session -> session.update(newStringProperty(1, "prop", "value2")));

        assertEquals("value2", loadStringProperty(1, "prop").getValue());
    }

    @Test
    void persistNewProperty()
    {
        write(session -> session.persist(newStringProperty(2, "prop", "value")));

        assertEquals("value", loadStringProperty(2, "prop").getValue());
        assertEquals(StringProperty.class.getName(), getStoredClassType(2));
    }

    @Test
    void updateDetachedAttachment()
    {
        // The identifier of an attachment is computed from its document, which an instance created by Hibernate
        // doesn't have
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getId()).thenReturn(42L);
        when(document.getFullName()).thenReturn("Space.Page");

        write(session -> session.persist(newAttachment(document, "version 1")));

        // What the store does when saving an existing attachment
        write(session -> session.update(newAttachment(document, "version 2")));

        assertEquals(List.of("version 2"), read(session -> session
            .createNativeQuery("select XWA_COMMENT from xwikiattachment where XWA_DOC_ID = 42", String.class)
            .list()));
    }

    private static XWikiAttachment newAttachment(XWikiDocument document, String comment)
    {
        XWikiAttachment attachment = new XWikiAttachment(document, "file.txt");
        attachment.setComment(comment);
        attachment.setAuthor("XWiki.Alice");
        attachment.setVersion("1.1");

        return attachment;
    }

    @Test
    void saveNewDocumentVersion()
    {
        // The information and the content of a version are two entities mapped on the same row
        XWikiRCSNodeId id = new XWikiRCSNodeId(43L, new Version(1, 1));
        XWikiRCSNodeInfo nodeInfo = new XWikiRCSNodeInfo(id);
        nodeInfo.setDate(new Date());
        nodeInfo.setAuthor("XWiki.Alice");
        nodeInfo.setComment("comment");
        XWikiRCSNodeContent nodeContent = new XWikiRCSNodeContent(id);
        nodeContent.setPatch(new XWikiPatch("content", false));

        // What the versioning store does when saving a new version
        write(session -> {
            session.saveOrUpdate(nodeInfo);
            session.update(nodeContent);
        });

        assertEquals("XWiki.Alice", read(session -> session.find(XWikiRCSNodeInfo.class, id)).getAuthor());
        assertEquals("content", read(session -> session.find(XWikiRCSNodeContent.class, id)).getPatch().getContent());
    }

    @Test
    void removeDetachedPropertyAndPersistAnotherType()
    {
        write(session -> session.persist(newStringProperty(3, "prop", "short")));

        // What the store does when the type of a property changed
        write(session -> {
            session.remove(newStringProperty(3, "prop", "short"));

            LargeStringProperty newProperty = new LargeStringProperty();
            newProperty.setId(3);
            newProperty.setName("prop");
            newProperty.setValue("large");
            session.persist(newProperty);
        });

        assertEquals(LargeStringProperty.class.getName(), getStoredClassType(3));
    }

    @Test
    void propertiesSharingATableAreDistinguishedByClassType()
    {
        LargeStringProperty largeString = new LargeStringProperty();
        largeString.setId(4);
        largeString.setName("large");
        largeString.setValue("text");
        StringListProperty stringList = new StringListProperty();
        stringList.setId(4);
        stringList.setName("list");
        stringList.setValue(List.of("a", "b"));
        write(session -> {
            session.persist(largeString);
            session.persist(stringList);
        });

        assertEquals(LargeStringProperty.class.getName(), getStoredClassType(4, "large"));
        assertEquals(StringListProperty.class.getName(), getStoredClassType(4, "list"));

        assertEquals(List.of("large"), read(session -> session
            .createQuery("select prop.name from LargeStringProperty as prop where prop.value = 'text'", String.class)
            .list()));
        assertEquals(List.of("list"), read(session -> session
            .createQuery("select prop.name from StringListProperty as prop where prop.textValue like 'a%'",
                String.class)
            .list()));

        LargeStringProperty loadedLargeString = new LargeStringProperty();
        loadedLargeString.setId(4);
        loadedLargeString.setName("large");
        assertEquals("text", load(loadedLargeString).getValue());
    }

    private static String getStoredClassType(long id, String name)
    {
        return read(session -> session
            .createNativeQuery("select XWP_CLASSTYPE from xwikiproperties where XWP_ID = :id and XWP_NAME = :name",
                String.class)
            .setParameter("id", id).setParameter("name", name).uniqueResult());
    }

    @Test
    void removeDetachedEntity()
    {
        write(session -> session.persist(new XWikiLock(5, "XWiki.Alice")));

        write(session -> session.remove(new XWikiLock(5, "XWiki.Alice")));

        assertNull(read(session -> session.find(XWikiLock.class, 5L)));
    }

    @Test
    void selectWithObjectResultType()
    {
        write(session -> session.persist(newStringProperty(6, "prop", "selected")));

        // One selected item is returned as is
        List<Object> names = read(session -> session
            .createQuery("select prop.name from StringProperty as prop where prop.value = 'selected'", Object.class)
            .list());
        assertEquals(List.of("prop"), names);

        // Several selected items are returned as an array
        List<Object> rows = read(session -> session.createQuery(
            "select prop.name, prop.classType from StringProperty as prop where prop.value = 'selected'", Object.class)
            .list());
        assertEquals(1, rows.size());
        assertArrayEquals(new Object[] {"prop", StringProperty.class.getName()}, (Object[]) rows.get(0));

        // Same for native queries
        List<Object> nativeRows = read(session -> session
            .createNativeQuery("select XWP_NAME, XWP_CLASSTYPE from xwikiproperties where XWP_ID = 6", Object.class)
            .list());
        assertEquals(1, nativeRows.size());
        assertArrayEquals(new Object[] {"prop", StringProperty.class.getName()}, (Object[]) nativeRows.get(0));
    }

    @Test
    void removeImmutableEntity()
    {
        long id = persistDeletedAttachment();

        // The documents and attachments in the recycle bin are immutable, but can still be removed
        write(session -> session.remove(session.find(DeletedAttachment.class, id)));

        assertNull(read(session -> session.find(DeletedAttachment.class, id)));
    }

    @Test
    void deleteQueryOnImmutableEntity()
    {
        long id = persistDeletedAttachment();

        // Only the update queries on an immutable entity are rejected by default, not the delete ones
        write(session -> session.createMutationQuery("delete from DeletedAttachment where id = :id")
            .setParameter("id", id).executeUpdate());

        assertNull(read(session -> session.find(DeletedAttachment.class, id)));
    }

    @Test
    void findPropertyFromItsStoredClassType()
    {
        write(session -> session.persist(newStringProperty(9, "prop", "found")));

        StringProperty property = read(session -> {
            // The entity is resolved from the class name stored in XWP_CLASSTYPE
            Class<?> propertyClass = session.getMetamodel().entity(getStoredClassType(9)).getJavaType();

            // A property is its own identifier
            return (StringProperty) session.find(propertyClass, newStringProperty(9, "prop", ""));
        });

        assertEquals("found", property.getValue());
    }

    @Test
    void persistAnotherTypeAfterRemovingTheOldPropertyInTheSameSession()
    {
        write(session -> session.persist(newStringProperty(20, "prop", "old")));

        // What the store does when the type of a property changed: the old property is first removed as a field to
        // remove of the object, and the type stored in the database is still the old one since XWiki sessions are
        // only flushed at commit time
        write(session -> {
            session.setHibernateFlushMode(FlushMode.COMMIT);

            session.remove(newStringProperty(20, "prop", "old"));

            assertEquals(StringProperty.class.getName(), session
                .createQuery("select prop.classType from BaseProperty as prop where prop.id = :id and prop.name= :name",
                    String.class)
                .setParameter("id", 20L).setParameter("name", "prop").uniqueResult());
            // The old property can't be found anymore since it's already removed
            assertNull(session.find(StringProperty.class, newStringProperty(20, "prop", "")));

            LargeStringProperty newProperty = new LargeStringProperty();
            newProperty.setId(20);
            newProperty.setName("prop");
            newProperty.setValue("large");
            session.persist(newProperty);
        });

        assertEquals(LargeStringProperty.class.getName(), getStoredClassType(20));
    }

}
