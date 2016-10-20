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
package org.xwiki.filter.confluence.xml.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.xml.stax.StAXUtils;

import com.google.common.base.Strings;

public class ConfluenceXMLPackage
{
    public static final String KEY_SPACE_NAME = "name";

    public static final String KEY_SPACE_DESCRIPTION = "description";

    public static final String KEY_PAGE_HOMEPAGE = "homepage";

    public static final String KEY_PAGE_PARENT = "parent";

    public static final String KEY_PAGE_SPACE = "space";

    public static final String KEY_PAGE_TITLE = "title";

    public static final String KEY_PAGE_CONTENTS = "bodyContents";

    public static final String KEY_PAGE_CREATION_AUTHOR = "creatorName";

    public static final String KEY_PAGE_CREATION_DATE = "creationDate";

    public static final String KEY_PAGE_REVISION = "version";

    public static final String KEY_PAGE_REVISION_AUTHOR = "lastModifierName";

    public static final String KEY_PAGE_REVISION_DATE = "lastModificationDate";

    public static final String KEY_PAGE_REVISION_COMMENT = "versionComment";

    public static final String KEY_PAGE_REVISIONS = "historicalVersions";

    public static final String KEY_PAGE_CONTENT_STATUS = "contentStatus";

    public static final String KEY_PAGE_BODY = "body";

    public static final String KEY_PAGE_BODY_TYPE = "bodyType";

    /**
     * Old property to indicate attachment name.
     * 
     * @see #KEY_ATTACHMENT_TITLE
     */
    public static final String KEY_ATTACHMENT_NAME = "fileName";

    public static final String KEY_ATTACHMENT_TITLE = "title";

    /**
     * Old field containing attachment page id.
     * 
     * @see #KEY_ATTACHMENT_CONTAINERCONTENT
     */
    public static final String KEY_ATTACHMENT_CONTENT = "content";

    public static final String KEY_ATTACHMENT_CONTAINERCONTENT = "containerContent";

    /**
     * Old property to indicate attachment size.
     * 
     * @see #KEY_ATTACHMENT_CONTENTPROPERTIES
     * @see #KEY_ATTACHMENT_CONTENT_FILESIZE
     */
    public static final String KEY_ATTACHMENT_CONTENT_SIZE = "fileSize";

    /**
     * Old property to indicate attachment media type.
     * 
     * @see #KEY_ATTACHMENT_CONTENTPROPERTIES
     * @see #KEY_ATTACHMENT_CONTENT_MEDIA_TYPE
     */
    public static final String KEY_ATTACHMENT_CONTENTTYPE = "contentType";

    public static final String KEY_ATTACHMENT_CONTENTPROPERTIES = "contentProperties";

    public static final String KEY_ATTACHMENT_CONTENTSTATUS = "contentStatus";

    public static final String KEY_ATTACHMENT_CONTENT_MINOR_EDIT = "MINOR_EDIT";

    public static final String KEY_ATTACHMENT_CONTENT_FILESIZE = "FILESIZE";

    public static final String KEY_ATTACHMENT_CONTENT_MEDIA_TYPE = "MEDIA_TYPE";

    public static final String KEY_ATTACHMENT_CREATION_AUTHOR = "creatorName";

    public static final String KEY_ATTACHMENT_CREATION_DATE = "creationDate";

    public static final String KEY_ATTACHMENT_REVISION_AUTHOR = "lastModifierName";

    public static final String KEY_ATTACHMENT_REVISION_DATE = "lastModificationDate";

    public static final String KEY_ATTACHMENT_REVISION_COMMENT = "comment";

    /**
     * Old property to indicate attachment revision.
     * 
     * @see #KEY_ATTACHMENT_VERSION
     */
    public static final String KEY_ATTACHMENT_ATTACHMENTVERSION = "attachmentVersion";

    public static final String KEY_ATTACHMENT_VERSION = "version";

    /**
     * Old property to indicate attachment original revision.
     * 
     * @see #KEY_ATTACHMENT_ORIGINALVERSIONID
     */
    public static final String KEY_ATTACHMENT_ORIGINALVERSION = "originalVersion";

    public static final String KEY_ATTACHMENT_ORIGINALVERSIONID = "originalVersionId";

    public static final String KEY_ATTACHMENT_DTO = "imageDetailsDTO";

    public static final String KEY_GROUP_NAME = "name";

    public static final String KEY_GROUP_ACTIVE = "active";

    public static final String KEY_GROUP_LOCAL = "local";

    public static final String KEY_GROUP_CREATION_DATE = "createdDate";

    public static final String KEY_GROUP_REVISION_DATE = "updatedDate";

    public static final String KEY_GROUP_DESCRIPTION = "description";

    public static final String KEY_GROUP_MEMBERUSERS = "memberusers";

    public static final String KEY_GROUP_MEMBERGROUPS = "membergroups";

    public static final String KEY_USER_NAME = "name";

    public static final String KEY_USER_ACTIVE = "active";

    public static final String KEY_USER_CREATION_DATE = "createdDate";

    public static final String KEY_USER_REVISION_DATE = "updatedDate";

    public static final String KEY_USER_FIRSTNAME = "firstName";

    public static final String KEY_USER_LASTNAME = "lastName";

    public static final String KEY_USER_DISPLAYNAME = "displayName";

    public static final String KEY_USER_EMAIL = "emailAddress";

    public static final String KEY_USER_PASSWORD = "credential";

    /**
     * 2012-03-07 17:16:48.158
     */
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * pattern to find the end of "intentionally damaged" CDATA end sections. Confluence does this to nest CDATA
     * sections inside CDATA sections. Interestingly it does not care if there is a &gt; after the ]].
     */
    private static final Pattern FIND_BROKEN_CDATA_PATTERN = Pattern.compile("]] ");

    /**
     * replacement to repair the CDATA
     */
    private static final String REPAIRED_CDATA_END = "]]";

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private File directory;

    private File entities;

    private File descriptor;

    private File tree;

    private Map<Integer, List<Integer>> pages = new LinkedHashMap<>();

    public ConfluenceXMLPackage(InputSource source)
        throws IOException, FilterException, XMLStreamException, FactoryConfigurationError, ConfigurationException
    {
        InputStream stream;

        if (source instanceof InputStreamInputSource) {
            stream = ((InputStreamInputSource) source).getInputStream();
        } else {
            throw new FilterException(
                String.format("Unsupported input source of type [%s]", source.getClass().getName()));
        }

        try {
            // Get temporary folder
            this.directory = File.createTempFile("confluencexml", "");
            this.directory.delete();
            this.directory.mkdir();
            this.directory.deleteOnExit();

            // Extract the zip
            ZipArchiveInputStream zais = new ZipArchiveInputStream(stream);
            for (ZipArchiveEntry zipEntry = zais.getNextZipEntry(); zipEntry != null; zipEntry =
                zais.getNextZipEntry()) {
                if (!zipEntry.isDirectory()) {
                    String path = zipEntry.getName();
                    File file = new File(this.directory, path);

                    if (path.equals("entities.xml")) {
                        this.entities = file;
                    } else if (path.equals("exportDescriptor.properties")) {
                        this.descriptor = file;
                    }

                    FileUtils.copyInputStreamToFile(new CloseShieldInputStream(zais), file);
                }
            }
        } finally {
            source.close();
        }

        // Initialize

        createTree();
    }

    private PropertiesConfiguration newProperties()
    {
        PropertiesConfiguration properties = new PropertiesConfiguration();

        properties.setDelimiterParsingDisabled(true);

        return properties;
    }

    public Date getDate(PropertiesConfiguration properties, String key) throws ParseException
    {
        String str = properties.getString(key);

        return str != null ? DATE_FORMAT.parse(str) : null;
    }

    public List<Integer> getIntegertList(PropertiesConfiguration properties, String key)
    {
        return getIntegertList(properties, key, null);
    }

    public List<Integer> getIntegertList(PropertiesConfiguration properties, String key, List<Integer> def)
    {
        List<Object> list = properties.getList(key, null);

        if (list == null) {
            return def;
        }

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        if (list.get(0) instanceof Integer) {
            return (List) list;
        }

        List<Integer> integerList = new ArrayList<>(list.size());
        for (Object element : list) {
            integerList.add(Integer.valueOf(element.toString()));
        }

        return integerList;
    }

    public PropertiesConfiguration getContentProperties(PropertiesConfiguration properties, String key)
        throws ConfigurationException
    {
        List<Integer> elements = getIntegertList(properties, key);

        if (elements == null) {
            return null;
        }

        PropertiesConfiguration contentProperties = new PropertiesConfiguration();
        for (Integer element : elements) {
            PropertiesConfiguration contentProperty = getObjectProperties(element);
            if (contentProperty != null) {
                String name = contentProperty.getString("name");

                Object value = contentProperty.getString("longValue", null);
                if (Strings.isNullOrEmpty((String) value)) {
                    value = contentProperty.getString("dateValue", null);
                    if (Strings.isNullOrEmpty((String) value)) {
                        value = contentProperty.getString("stringValue", null);
                    } else {
                        // TODO: dateValue
                    }
                } else {
                    value = contentProperty.getLong("longValue", null);
                }

                contentProperties.setProperty(name, value);
            }
        }

        return contentProperties;

    }

    public EntityReference getReferenceFromId(PropertiesConfiguration currentProperties, String key)
        throws ConfigurationException
    {
        Integer pageId = currentProperties.getInteger(key, null);
        if (pageId != null) {
            PropertiesConfiguration pageProperties = getPageProperties(pageId);

            int spaceId = pageProperties.getInt(KEY_PAGE_SPACE);
            int currentSpaceId = currentProperties.getInt(KEY_PAGE_SPACE);

            EntityReference spaceReference;
            if (spaceId != currentSpaceId) {
                spaceReference = new EntityReference(getSpaceName(currentSpaceId), EntityType.SPACE);
            } else {
                spaceReference = null;
            }

            return new EntityReference(pageProperties.getString(KEY_PAGE_TITLE), EntityType.DOCUMENT, spaceReference);
        }

        return null;
    }

    public String getSpaceName(int spaceId) throws ConfigurationException
    {
        PropertiesConfiguration spaceProperties = getSpaceProperties(spaceId);

        return spaceProperties.getString(KEY_SPACE_NAME);
    }

    public Map<Integer, List<Integer>> getPages()
    {
        return this.pages;
    }

    private void createTree()
        throws XMLStreamException, FactoryConfigurationError, IOException, ConfigurationException, FilterException
    {
        this.tree = new File(this.directory, "tree");
        this.tree.mkdir();

        try (InputStream stream = new FileInputStream(getEntities())) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(stream);

            xmlReader.nextTag();

            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                String elementName = xmlReader.getLocalName();

                if (elementName.equals("object")) {
                    readObject(xmlReader);
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            }
        }
    }

    private void readObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        String type = xmlReader.getAttributeValue(null, "class");

        if (type != null) {
            if (type.equals("Page")) {
                readPageObject(xmlReader);
            } else if (type.equals("Space")) {
                readSpaceObject(xmlReader);
            } else if (type.equals("InternalUser")) {
                readUserObject(xmlReader);
            } else if (type.equals("InternalGroup")) {
                readGroupObject(xmlReader);
            } else if (type.equals("HibernateMembership")) {
                readMembershipObject(xmlReader);
            } else if (type.equals("BodyContent")) {
                readBodyContentObject(xmlReader);
            } else if (type.equals("SpaceDescription")) {
                readSpaceDescriptionObject(xmlReader);
            } else if (type.equals("SpacePermission")) {
                readSpacePermissionObject(xmlReader);
            } else if (type.equals("Attachment")) {
                readAttachmentObject(xmlReader);
            } else {
                PropertiesConfiguration properties = newProperties();

                int id = readObjectProperties(xmlReader, properties);

                // Save page
                saveObjectProperties(properties, id);
            }
        }
    }

    private int readObjectProperties(XMLStreamReader xmlReader, PropertiesConfiguration properties)
        throws XMLStreamException, FilterException
    {
        int id = -1;

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals("id")) {
                String idName = xmlReader.getAttributeValue(null, "name");

                if (idName != null && idName.equals("id")) {
                    id = Integer.valueOf(xmlReader.getElementText());

                    properties.setProperty("id", id);
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            } else if (elementName.equals("property")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readProperty(xmlReader));
            } else if (elementName.equals("collection")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readProperty(xmlReader));
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }

        return id;
    }

    private void readAttachmentObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        PropertiesConfiguration properties = newProperties();

        int attachmentId = readObjectProperties(xmlReader, properties);

        Integer pageId = getAttachmentPageId(properties);

        if (pageId != null) {
            // Save attachment
            saveAttachmentProperties(properties, pageId, attachmentId);
        }
    }

    private Integer getAttachmentPageId(PropertiesConfiguration properties)
    {
        Integer pageId = getInteger(properties, KEY_ATTACHMENT_CONTAINERCONTENT, null);

        if (pageId == null) {
            pageId = properties.getInteger(KEY_ATTACHMENT_CONTENT, null);
        }

        return pageId; 
    }

    private void readSpaceObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        PropertiesConfiguration properties = newProperties();

        int spaceId = readObjectProperties(xmlReader, properties);

        // Save page
        saveSpaceProperties(properties, spaceId);

        // Register space
        List<Integer> spacePages = this.pages.get(spaceId);
        if (spacePages == null) {
            spacePages = new LinkedList<>();
            this.pages.put(spaceId, spacePages);
        }
    }

    private void readSpaceDescriptionObject(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException, ConfigurationException
    {
        PropertiesConfiguration properties = newProperties();

        int descriptionId = readObjectProperties(xmlReader, properties);

        properties.setProperty(KEY_PAGE_HOMEPAGE, true);

        // Save page
        savePageProperties(properties, descriptionId);
    }

    private void readSpacePermissionObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        int permissionId = readObjectProperties(xmlReader, properties);

        Integer spaceId = properties.getInteger("space", null);
        if (spaceId != null) {
            // Save attachment
            saveSpacePermissionsProperties(properties, spaceId, permissionId);
        }
    }

    private void readBodyContentObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();
        properties.setDelimiterParsingDisabled(true);

        readObjectProperties(xmlReader, properties);

        Integer pageId = properties.getInteger("content", null);
        if (pageId != null) {
            savePageProperties(properties, pageId);
        }
    }

    private void readPageObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        int pageId = readObjectProperties(xmlReader, properties);

        // Save page
        savePageProperties(properties, pageId);

        // Register only current pages (they will take care of handling there history)
        Integer originalVersion = (Integer) properties.getProperty("originalVersion");
        if (originalVersion == null) {
            Integer spaceId = (Integer) properties.getInteger("space", null);
            List<Integer> spacePages = this.pages.get(spaceId);
            if (spacePages == null) {
                spacePages = new LinkedList<>();
                this.pages.put(spaceId, spacePages);
            }
            spacePages.add(pageId);
        }
    }

    private void readUserObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        int pageId = readObjectProperties(xmlReader, properties);

        // Save page
        saveObjectProperties("users", properties, pageId);
    }

    private void readGroupObject(XMLStreamReader xmlReader)
        throws XMLStreamException, ConfigurationException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        int pageId = readObjectProperties(xmlReader, properties);

        // Save page
        saveObjectProperties("groups", properties, pageId);
    }

    private void readMembershipObject(XMLStreamReader xmlReader)
        throws ConfigurationException, XMLStreamException, FilterException
    {
        PropertiesConfiguration properties = newProperties();

        readObjectProperties(xmlReader, properties);

        Integer parentGroup = properties.getInteger("parentGroup", null);

        if (parentGroup != null) {
            PropertiesConfiguration groupProperties = getGroupProperties(parentGroup);

            Integer userMember = properties.getInteger("userMember", null);

            if (userMember != null) {
                List<Integer> users = new ArrayList<>(
                    getIntegertList(groupProperties, KEY_GROUP_MEMBERUSERS, Collections.<Integer>emptyList()));
                users.add(userMember);
                groupProperties.setProperty(KEY_GROUP_MEMBERUSERS, users);
            }

            Integer groupMember = properties.getInteger("groupMember", null);

            if (groupMember != null) {
                List<Integer> groups = new ArrayList<>(
                    getIntegertList(groupProperties, KEY_GROUP_MEMBERGROUPS, Collections.<Integer>emptyList()));
                groups.add(groupMember);
                groupProperties.setProperty(KEY_GROUP_MEMBERGROUPS, groups);
            }

            saveObjectProperties("groups", groupProperties, parentGroup);
        }
    }

    private Object readProperty(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        String propertyClass = xmlReader.getAttributeValue(null, "class");

        if (propertyClass == null) {
            return fixCData(xmlReader.getElementText());
        } else if (propertyClass.equals("java.util.List") || propertyClass.equals("java.util.Collection")) {
            return readListProperty(xmlReader);
        } else if (propertyClass.equals("java.util.Set")) {
            return readSetProperty(xmlReader);
        } else if (propertyClass.equals("Page") || propertyClass.equals("Space") || propertyClass.equals("BodyContent")
            || propertyClass.equals("Attachment") || propertyClass.equals("SpaceDescription")
            || propertyClass.equals("Labelling") || propertyClass.equals("SpacePermission")
            || propertyClass.equals("InternalGroup") || propertyClass.equals("InternalUser")
            || propertyClass.equals("Comment") || propertyClass.equals("ContentProperty")) {
            return readObjectReference(xmlReader);
        } else {
            StAXUtils.skipElement(xmlReader);
        }

        return null;
    }

    /**
     * to protect content with cdata section inside of cdata elements confluence adds a single space after two
     * consecutive curly braces. we need to undo this patch as otherwise the content parser will complain about invalid
     * content. strictly speaking this needs only to be done for string valued properties
     */
    private String fixCData(String elementText)
    {
        if (elementText == null) {
            return elementText;
        }
        return FIND_BROKEN_CDATA_PATTERN.matcher(elementText).replaceAll(REPAIRED_CDATA_END);
    }

    private Integer readObjectReference(XMLStreamReader xmlReader) throws FilterException, XMLStreamException
    {
        xmlReader.nextTag();

        if (!xmlReader.getLocalName().equals("id")) {
            throw new FilterException(
                String.format("Was expecting id element but found [%s]", xmlReader.getLocalName()));
        }

        Integer id = Integer.valueOf(xmlReader.getElementText());

        xmlReader.nextTag();

        return id;
    }

    private List<Object> readListProperty(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        List<Object> list = new ArrayList<>();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            list.add(readProperty(xmlReader));
        }

        return list;
    }

    private Set<Object> readSetProperty(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        Set<Object> set = new LinkedHashSet<>();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            set.add(readProperty(xmlReader));
        }

        return set;
    }

    private File getSpacesFolder()
    {
        return new File(this.tree, "spaces");
    }

    private File getSpaceFolder(int spaceId)
    {
        return new File(getSpacesFolder(), String.valueOf(spaceId));
    }

    private File getPagesFolder()
    {
        return new File(this.tree, "pages");
    }

    private File getObjectsFolder(String folderName)
    {
        return new File(this.tree, folderName);
    }

    private File getUsersFolder()
    {
        return getObjectsFolder("users");
    }

    private File getGroupsFolder()
    {
        return getObjectsFolder("groups");
    }

    private File getPageFolder(int pageId)
    {
        return new File(getPagesFolder(), String.valueOf(pageId));
    }

    private File getObjectFolder(String folderName, int objectId)
    {
        return new File(getObjectsFolder(folderName), String.valueOf(objectId));
    }

    private File getPagePropertiesFile(int pageId)
    {
        File folder = getPageFolder(pageId);

        return new File(folder, "properties.properties");
    }

    private File getObjectPropertiesFile(String folderName, int propertyId)
    {
        File folder = getObjectFolder(folderName, propertyId);

        return new File(folder, "properties.properties");
    }

    public Collection<Integer> getAttachments(int pageId)
    {
        File folder = getAttachmentsFolder(pageId);

        Collection<Integer> attachments;
        if (folder.exists()) {
            String[] attachmentFolders = folder.list();

            attachments = new TreeSet<>();
            for (String attachmentIdString : attachmentFolders) {
                if (NumberUtils.isNumber(attachmentIdString)) {
                    attachments.add(Integer.valueOf(attachmentIdString));
                }
            }
        } else {
            attachments = Collections.emptyList();
        }

        return attachments;
    }

    private File getAttachmentsFolder(int pageId)
    {
        return new File(getPageFolder(pageId), "attachments");
    }

    private File getSpacePermissionsFolder(int spaceId)
    {
        return new File(getSpaceFolder(spaceId), "permissions");
    }

    private File getAttachmentFolder(int pageId, int attachmentId)
    {
        return new File(getAttachmentsFolder(pageId), String.valueOf(attachmentId));
    }

    private File getSpacePermissionFolder(int spaceId, int permissionId)
    {
        return new File(getSpacePermissionsFolder(spaceId), String.valueOf(permissionId));
    }

    private File getAttachmentPropertiesFile(int pageId, int attachmentId)
    {
        File folder = getAttachmentFolder(pageId, attachmentId);

        return new File(folder, "properties.properties");
    }

    private File getSpacePermissionPropertiesFile(int spaceId, int permissionId)
    {
        File folder = getSpacePermissionFolder(spaceId, permissionId);

        return new File(folder, "properties.properties");
    }

    private File getSpacePropertiesFile(int spaceId)
    {
        File folder = getSpaceFolder(spaceId);

        return new File(folder, "properties.properties");
    }

    public PropertiesConfiguration getPageProperties(int pageId) throws ConfigurationException
    {
        File file = getPagePropertiesFile(pageId);

        return new PropertiesConfiguration(file);
    }

    public PropertiesConfiguration getObjectProperties(Integer objectId) throws ConfigurationException
    {
        return getObjectProperties("objects", objectId);
    }

    public PropertiesConfiguration getObjectProperties(String folder, Integer objectId) throws ConfigurationException
    {
        int id;
        if (objectId != null) {
            id = (Integer) objectId;
        } else {
            return null;
        }

        File file = getObjectPropertiesFile(folder, id);

        return new PropertiesConfiguration(file);
    }

    public PropertiesConfiguration getUserProperties(Integer userId) throws ConfigurationException
    {
        return getObjectProperties("users", userId);
    }

    public Collection<Integer> getUsers()
    {
        File folder = getUsersFolder();

        Collection<Integer> users;
        if (folder.exists()) {
            String[] userFolders = folder.list();

            users = new TreeSet<>();
            for (String userIdString : userFolders) {
                if (NumberUtils.isNumber(userIdString)) {
                    users.add(Integer.valueOf(userIdString));
                }
            }
        } else {
            users = Collections.emptyList();
        }

        return users;
    }

    public Collection<Integer> getGroups()
    {
        File folder = getGroupsFolder();

        Collection<Integer> groups;
        if (folder.exists()) {
            String[] groupFolders = folder.list();

            groups = new TreeSet<>();
            for (String groupIdString : groupFolders) {
                if (NumberUtils.isNumber(groupIdString)) {
                    groups.add(Integer.valueOf(groupIdString));
                }
            }
        } else {
            groups = Collections.emptyList();
        }

        return groups;
    }

    public PropertiesConfiguration getGroupProperties(Integer groupId) throws ConfigurationException
    {
        return getObjectProperties("groups", groupId);
    }

    public PropertiesConfiguration getAttachmentProperties(int pageId, int attachmentId) throws ConfigurationException
    {
        File file = getAttachmentPropertiesFile(pageId, attachmentId);

        return new PropertiesConfiguration(file);
    }

    public PropertiesConfiguration getSpacePermissionProperties(int spaceId, int permissionId)
        throws ConfigurationException
    {
        File file = getSpacePermissionPropertiesFile(spaceId, permissionId);

        return new PropertiesConfiguration(file);
    }

    public PropertiesConfiguration getSpaceProperties(int spaceId) throws ConfigurationException
    {
        File file = getSpacePropertiesFile(spaceId);

        return new PropertiesConfiguration(file);
    }

    private void savePageProperties(PropertiesConfiguration properties, int pageId) throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getPageProperties(pageId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveObjectProperties(PropertiesConfiguration properties, int objectId) throws ConfigurationException
    {
        saveObjectProperties("objects", properties, objectId);
    }

    private void saveObjectProperties(String folder, PropertiesConfiguration properties, int objectId)
        throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getObjectProperties(folder, objectId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveAttachmentProperties(PropertiesConfiguration properties, int pageId, int attachmentId)
        throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getAttachmentProperties(pageId, attachmentId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveSpacePermissionsProperties(PropertiesConfiguration properties, int spaceId, int permissionId)
        throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getSpacePermissionProperties(spaceId, permissionId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    private void saveSpaceProperties(PropertiesConfiguration properties, int spaceId) throws ConfigurationException
    {
        PropertiesConfiguration fileProperties = getSpaceProperties(spaceId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    public File getEntities()
    {
        return this.entities;
    }

    public File getDescriptor()
    {
        return this.descriptor;
    }

    public File getAttachmentFile(int pageId, int attachmentId, int version) throws FileNotFoundException
    {
        File attachmentsFolder = new File(this.directory, "attachments");
        File attachmentsPageFolder = new File(attachmentsFolder, String.valueOf(pageId));
        File attachmentFolder = new File(attachmentsPageFolder, String.valueOf(attachmentId));

        // In old version the file name is the version
        File file = new File(attachmentFolder, String.valueOf(version));

        if (file.exists()) {
            return file;
        }

        // In recent version the name is always 1
        file = new File(attachmentFolder, "1");

        if (file.exists()) {
            return file;
        }

        throw new FileNotFoundException(file.getAbsolutePath());
    }

    public void close() throws IOException
    {
        if (this.tree != null) {
            FileUtils.deleteDirectory(this.tree);
        }
    }

    public String getAttachmentName(PropertiesConfiguration attachmentProperties)
    {
        String attachmentName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_TITLE, null);
        if (attachmentName == null) {
            attachmentName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_NAME);
        }

        return attachmentName;
    }

    public Integer getAttachementVersion(PropertiesConfiguration attachmentProperties)
    {
        Integer version = getInteger(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_VERSION, null);
        if (version == null) {
            version = getInteger(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ATTACHMENTVERSION, null);
        }

        return version;
    }

    public int getAttachmentOriginalVersionId(PropertiesConfiguration attachmentProperties, int def)
    {
        Integer originalRevisionId =
            getInteger(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSIONID, null);
        return originalRevisionId != null ? originalRevisionId
            : getInteger(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_ORIGINALVERSION, def);
    }

    public Integer getInteger(PropertiesConfiguration properties, String key, Integer def)
    {
        try {
            return properties.getInteger(key, def);
        } catch (Exception e) {
            // Usually mean the field does not have the expected format

            return def;
        }
    }
}
