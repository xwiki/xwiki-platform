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
import java.net.URLDecoder;
import java.util.Date;

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
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.filesystem.internal.GenericFileUtils;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for XWIKI-9065. Make sure to generate database entry for attachment deleted on filesystem.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Named("R910010XWIKI9065")
@Singleton
public class R910010XWIKI9065DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource configuration;

    @Inject
    private FilesystemStoreTools fstools;

    @Inject
    private Logger logger;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Make sure all existing deleted attachments have a store id and move back metadata to the database.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(910010);
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        // Move back metadata of deleted attachments located in the filesystem store
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Void>()
        {
            @Override
            public Void doInHibernate(Session session)
            {
                try {
                    migrateMetadatas(session);
                } catch (Exception e) {
                    throw new HibernateException("Failed to move deleted attachments metadata to the database", e);
                }

                return null;
            }
        });
    }

    private void migrateMetadatas(Session session)
        throws IOException, XMLStreamException, FactoryConfigurationError, ParserConfigurationException, SAXException
    {
        this.logger.info("Migrating filesystem attachment metadatas storded in [{}]",
            this.fstools.getStorageLocationFile());

        File pathByIdStore = this.fstools.getGlobalFile("DELETED_ATTACHMENT_ID_MAPPINGS.xml");
        if (pathByIdStore.exists()) {
            try (FileInputStream stream = new FileInputStream(pathByIdStore)) {
                XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(stream);

                // <deletedattachmentids>
                xmlReader.nextTag();

                for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                    // <entry>
                    xmlReader.nextTag();

                    String value1 = xmlReader.getElementText();
                    xmlReader.nextTag();
                    String value2 = xmlReader.getElementText();

                    long id;
                    String path;
                    if (xmlReader.getLocalName().equals("path")) {
                        id = Long.valueOf(value1);
                        path = value2;
                    } else {
                        id = Long.valueOf(value2);
                        path = value1;
                    }

                    File directory = new File(path);
                    if (!directory.exists()) {
                        this.logger.warn("[{}] does not exist", directory);

                        continue;
                    }

                    if (!directory.isDirectory()) {
                        this.logger.warn("[{}] is not a directory", directory);

                        continue;
                    }

                    storeDeletedAttachment(directory, id, session);
                }
            }
        }
    }

    private void storeDeletedAttachment(File directory, long id, Session session)
        throws ParserConfigurationException, SAXException, IOException
    {
        this.logger.info("Storing attachment metadata [{}] in the database", directory);

        // Find attachment reference
        DocumentReference documentReference = getDocumentReference(directory);

        // Parse ~DELETED_ATTACH_METADATA.xml
        DeletedAttachment dbAttachment = parseDeletedAttachMedatata(documentReference, id, directory);

        // Save deleted attachment in the DB
        session.save(dbAttachment);

        // Refactor file storage to be based on id instead of date
        File newDirectory = new File(directory.getParentFile(),
            GenericFileUtils.getURLEncoded(dbAttachment.getFilename() + "-id" + dbAttachment.getId()));
        FileUtils.moveDirectory(directory, newDirectory);

    }

    private String decode(String name) throws UnsupportedEncodingException
    {
        return URLDecoder.decode(name, "UTF-8");
    }

    private DocumentReference getDocumentReference(File directory) throws UnsupportedEncodingException
    {
        String name = decode(directory.getName());

        return new DocumentReference(name, (SpaceReference) getEntityReference(directory.getParentFile()));
    }

    private EntityReference getEntityReference(File directory) throws UnsupportedEncodingException
    {
        String name = decode(directory.getName());

        File root = this.fstools.getStorageLocationFile();

        File parent = directory.getParentFile();

        if (parent.equals(root)) {
            return new WikiReference(name);
        } else {
            return new SpaceReference(name, getEntityReference(parent));
        }
    }

    private DeletedAttachment parseDeletedAttachMedatata(DocumentReference documentReference, long id, File directory)
        throws ParserConfigurationException, SAXException, IOException
    {
        File file = new File(directory, "~DELETED_ATTACH_METADATA.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);

        String filename = getElementText(doc, "filename", null);
        String deleter = getElementText(doc, "deleter", null);
        Date deleteDate = new Date(Long.valueOf(getElementText(doc, "datedeleted", null)));

        long docId = new XWikiDocument(documentReference).getId();

        return new DeletedAttachment(docId, this.serializer.serialize(documentReference), filename,
            FileSystemStoreUtils.HINT, deleter, deleteDate, null, id);
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
