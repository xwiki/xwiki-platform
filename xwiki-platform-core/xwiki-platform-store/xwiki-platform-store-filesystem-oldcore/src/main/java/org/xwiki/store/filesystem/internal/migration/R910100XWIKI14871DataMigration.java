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

package org.xwiki.store.filesystem.internal.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-9065. Make sure to generate database entry for attachment deleted on filesystem.
 *
 * @version $Id$
 * @since 9.10.1
 * @since 9.11RC1
 */
@Component
@Named("R910100XWIKI14871")
@Singleton
public class R910100XWIKI14871DataMigration extends AbstractFileStoreDataMigration
{
    private static final Pattern STORAGE = Pattern.compile("/storage/");

    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource configuration;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    private final Set<String> migratedDeletedAttachment = new HashSet<>();

    @Override
    public String getDescription()
    {
        return "Make sure all existing deleted attachments have a store id and move back metadata to the database.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(910100);
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        // Move back metadata of deleted attachments located in the filesystem store
        getStore().executeWrite(getXWikiContext(), session -> {
            try {
                migrateMetadatas(session);
            } catch (Exception e) {
                throw new HibernateException("Failed to move deleted attachments metadata to the database", e);
            }

            return null;
        });
    }

    private void migrateMetadatas(Session session) throws IOException, XMLStreamException, FactoryConfigurationError,
        ParserConfigurationException, SAXException, DataMigrationException
    {
        File storageLocationFile = getPre11StoreRootDirectory();

        this.logger.info("Migrating filesystem attachment metadatas stored in [{}]", storageLocationFile);

        File pathByIdStore = new File(storageLocationFile, "~GLOBAL_DELETED_ATTACHMENT_ID_MAPPINGS.xml");
        if (pathByIdStore.exists()) {
            try (FileInputStream stream = new FileInputStream(pathByIdStore)) {
                // No need to protect against XXE attacks since the XML file is controlled.
                // Note that if a user were able to change that XML file content, and modify the <path> element then
                // it would be possible to access any local file content since the path is displayed in the logs below.
                // That would still need server access though.
                XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(stream);

                // <deletedattachmentids>
                xmlReader.nextTag();

                for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                    // <entry>
                    xmlReader.nextTag();

                    String value1 = xmlReader.getElementText();
                    xmlReader.nextTag();
                    String value2 = xmlReader.getElementText();

                    String path;
                    if (xmlReader.getLocalName().equals("path")) {
                        path = value2;
                    } else {
                        path = value1;
                    }

                    // </entry>
                    xmlReader.nextTag();

                    if (!this.migratedDeletedAttachment.contains(path)) {
                        File directory = new File(path);
                        if (!directory.getCanonicalPath().startsWith(getStoreRootDirectory().getCanonicalPath())) {
                            this.logger.warn("[{}] is the wrong path, trying to find the new location", directory);

                            directory = findNewPath(path);

                            if (directory == null) {
                                this.logger.warn("Could not find the deleted attachment in any other location");

                                // Remember that this attachment could not be migrated
                                this.migratedDeletedAttachment.add(path);

                                continue;
                            } else {
                                this.logger.info("Found deleted attachment on [{}]", directory);
                            }
                        }

                        if (!directory.isDirectory()) {
                            this.logger.warn("[{}] is not a directory", directory);

                            continue;
                        }

                        // Find document reference
                        File documentDirectory = directory.getParentFile().getParentFile().getParentFile();
                        DocumentReference documentReference = getPre11DocumentReference(documentDirectory);

                        if (getXWikiContext().getWikiReference().equals(documentReference.getWikiReference())) {
                            storeDeletedAttachment(directory, documentReference, session);

                            // Remember which path we already migrated
                            this.migratedDeletedAttachment.add(path);
                        }
                    }
                }
            }
        }
    }

    private File findNewPath(String path)
    {
        Matcher matcher = STORAGE.matcher(path);

        if (matcher.find()) {
            String relative = path.substring(matcher.end());

            File newPath = new File(getStoreRootDirectory(), relative);
            if (newPath.exists()) {
                return newPath;
            }
        }

        return null;
    }

    private void storeDeletedAttachment(File directory, DocumentReference documentReference, Session session)
        throws ParserConfigurationException, SAXException, IOException, DataMigrationException
    {
        this.logger.info("Storing attachment metadata [{}] in the database", directory);

        // Parse ~DELETED_ATTACH_METADATA.xml
        File file = new File(directory, "~DELETED_ATTACH_METADATA.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);

        String filename = getElementText(doc, "filename", null);
        String deleter = getElementText(doc, "deleter", null);
        Date deleteDate = new Date(Long.valueOf(getElementText(doc, "datedeleted", null)));

        long docId = new XWikiDocument(documentReference).getId();

        // We need to make sure the deleted attachment is not already in the database with a different id (left
        // there by the attachment porter script for example)
        org.hibernate.query.Query<Long> selectQuery = session
            .createQuery("SELECT id FROM DeletedAttachment WHERE docId=:docId AND filename=:filename AND date=:date");
        selectQuery.setParameter("docId", docId);
        selectQuery.setParameter("filename", filename);
        selectQuery.setParameter("date", new java.sql.Timestamp(deleteDate.getTime()));
        Long databaseId = selectQuery.uniqueResult();

        if (databaseId == null) {
            // Try without the milliseconds since most versions of MySQL don't support them
            selectQuery.setParameter("date", new java.sql.Timestamp(deleteDate.toInstant().getEpochSecond() * 1000));
            databaseId = selectQuery.uniqueResult();
        }

        DeletedAttachment dbAttachment;
        if (databaseId != null) {
            // Update the database metadata (probably left there by the attachment porter script)
            dbAttachment = new DeletedAttachment(docId, this.serializer.serialize(documentReference), filename,
                FileSystemStoreUtils.HINT, deleter, deleteDate, null, databaseId);
            session.update(dbAttachment);
        } else {
            // Insert new deleted attachment metadata in the DB
            dbAttachment = new DeletedAttachment(docId, this.serializer.serialize(documentReference), filename,
                FileSystemStoreUtils.HINT, deleter, deleteDate, null);
            databaseId = (Long) session.save(dbAttachment);
        }

        // Refactor file storage to be based on database id instead of date
        File newDirectory =
            new File(directory.getParentFile(), encode(dbAttachment.getFilename() + "-id" + databaseId));
        FileUtils.moveDirectory(directory, newDirectory);
    }

    private String encode(String name) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(name, "UTF-8");
    }

    private String getElementText(Document doc, String elementName, String def)
    {
        NodeList elements = doc.getElementsByTagName(elementName);

        if (elements.getLength() > 0) {
            return elements.item(0).getTextContent();
        }

        return def;
    }
}
