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
package org.xwiki.filter.confluence.xml.internal.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.confluence.input.ConfluenceInputProperties;
import org.xwiki.filter.confluence.xml.internal.ConfluenceFilter;
import org.xwiki.filter.confluence.xml.internal.ConfluenceXMLPackage;
import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.event.user.GroupFilter;
import org.xwiki.filter.event.user.UserFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(ConfluenceInputFilterStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceInputFilterStream
    extends AbstractBeanInputFilterStream<ConfluenceInputProperties, ConfluenceFilter>
{
    @Inject
    private Logger logger;

    @Inject
    @Named("confluence/1.0")
    private StreamParser confluenceWIKIParser;

    @Inject
    @Named("confluence+xhtml/1.0")
    private StreamParser confluenceXHTMLParser;

    private ConfluenceXMLPackage confluencePackage;

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    @Override
    protected void read(Object filter, ConfluenceFilter proxyFilter) throws FilterException
    {
        // Prepare package
        try {
            this.confluencePackage = new ConfluenceXMLPackage(this.properties.getSource());
        } catch (Exception e) {
            throw new FilterException("Failed to read package", e);
        }

        // Generate users events
        for (int userInt : this.confluencePackage.getUsers()) {
            PropertiesConfiguration userProperties;
            try {
                userProperties = this.confluencePackage.getUserProperties(userInt);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get user properties", e);
            }

            String userId = userProperties.getString(ConfluenceXMLPackage.KEY_USER_NAME, String.valueOf(userInt));
            if (this.properties.isConvertToXWiki() && userId.equals("admin")) {
                userId = "Admin";
            }

            FilterEventParameters userParameters = new FilterEventParameters();

            userParameters.put(UserFilter.PARAMETER_FIRSTNAME,
                userProperties.getString(ConfluenceXMLPackage.KEY_USER_FIRSTNAME));
            userParameters.put(UserFilter.PARAMETER_LASTNAME,
                userProperties.getString(ConfluenceXMLPackage.KEY_USER_LASTNAME));
            userParameters.put(UserFilter.PARAMETER_EMAIL,
                userProperties.getString(ConfluenceXMLPackage.KEY_USER_EMAIL));
            userParameters.put(UserFilter.PARAMETER_ACTIVE,
                userProperties.getBoolean(ConfluenceXMLPackage.KEY_USER_ACTIVE));

            try {
                userParameters.put(UserFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(userProperties, ConfluenceXMLPackage.KEY_USER_REVISION_DATE));
                userParameters.put(UserFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(userProperties, ConfluenceXMLPackage.KEY_USER_CREATION_DATE));
            } catch (ParseException e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse date", e);
                }
            }

            // TODO: no idea how to import/convert the password, probably salted with the Confluence instance id

            // > User
            proxyFilter.beginUser(userId, userParameters);

            // < User
            proxyFilter.endUser(userId, userParameters);
        }

        // Generate users events
        for (int groupInt : this.confluencePackage.getGroups()) {
            PropertiesConfiguration groupProperties;
            try {
                groupProperties = this.confluencePackage.getGroupProperties(groupInt);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get group properties", e);
            }

            String groupName = groupProperties.getString(ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(groupInt));
            if (this.properties.isConvertToXWiki()) {
                if (groupName.equals("confluence-administrators")) {
                    groupName = "XWikiAdminGroup";
                } else if (groupName.equals("confluence-users")) {
                    groupName = "XWikiAllGroup";
                }
            }

            FilterEventParameters groupParameters = new FilterEventParameters();

            try {
                groupParameters.put(GroupFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(groupProperties, ConfluenceXMLPackage.KEY_GROUP_REVISION_DATE));
                groupParameters.put(GroupFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(groupProperties, ConfluenceXMLPackage.KEY_GROUP_CREATION_DATE));
            } catch (ParseException e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse date", e);
                }
            }

            // > Group
            proxyFilter.beginGroupContainer(groupName, groupParameters);

            // Members users
            if (groupProperties.containsKey(ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS)) {
                List<Integer> users =
                    this.confluencePackage.getIntegertList(groupProperties, ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS);
                for (Integer memberInt : users) {
                    FilterEventParameters memberParameters = new FilterEventParameters();

                    try {
                        String memberId = this.confluencePackage.getUserProperties(memberInt)
                            .getString(ConfluenceXMLPackage.KEY_USER_NAME, String.valueOf(memberInt));

                        if (this.properties.isConvertToXWiki() && memberId.equals("admin")) {
                            memberId = "Admin";
                        }

                        proxyFilter.onGroupMemberGroup(memberId, memberParameters);
                    } catch (ConfigurationException e) {
                        this.logger.error("Failed to get user properties", e);
                    }
                }
            }

            // Members groups
            if (groupProperties.containsKey(ConfluenceXMLPackage.KEY_GROUP_MEMBERGROUPS)) {
                List<Integer> groups = this.confluencePackage.getIntegertList(groupProperties,
                    ConfluenceXMLPackage.KEY_GROUP_MEMBERGROUPS);
                for (Integer memberInt : groups) {
                    FilterEventParameters memberParameters = new FilterEventParameters();

                    try {
                        String memberId = this.confluencePackage.getGroupProperties(memberInt)
                            .getString(ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(memberInt));

                        if (this.properties.isConvertToXWiki()) {
                            if (memberId.equals("confluence-administrators")) {
                                memberId = "XWikiAdminGroup";
                            } else if (memberId.equals("confluence-users")) {
                                memberId = "XWikiAllGroup";
                            }
                        }

                        proxyFilter.onGroupMemberGroup(memberId, memberParameters);
                    } catch (ConfigurationException e) {
                        this.logger.error("Failed to get group properties", e);
                    }
                }
            }

            // < Group
            proxyFilter.endGroupContainer(groupName, groupParameters);
        }

        // Generate documents events
        for (Map.Entry<Integer, List<Integer>> entry : this.confluencePackage.getPages().entrySet()) {
            int spaceId = entry.getKey();

            PropertiesConfiguration spaceProperties;
            try {
                spaceProperties = this.confluencePackage.getSpaceProperties(spaceId);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get space properties", e);
            }

            String spaceName = spaceProperties.getString(ConfluenceXMLPackage.KEY_SPACE_NAME);
            FilterEventParameters spaceParameters = new FilterEventParameters();

            // > WikiSpace
            proxyFilter.beginWikiSpace(spaceName, spaceParameters);

            // Main page
            Integer descriptionId = spaceProperties.getInteger(ConfluenceXMLPackage.KEY_SPACE_DESCRIPTION, null);
            if (descriptionId != null) {
                readPage(descriptionId, filter, proxyFilter);
            }

            // Other pages
            for (int pageId : entry.getValue()) {
                readPage(pageId, filter, proxyFilter);
            }

            // < WikiSpace
            proxyFilter.endWikiSpace(spaceName, spaceParameters);
        }

        // Cleanup

        try {
            this.confluencePackage.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close package", e);
        }
    }

    private void readPage(int pageId, Object filter, ConfluenceFilter proxyFilter) throws FilterException
    {
        PropertiesConfiguration pageProperties = getPageProperties(pageId);

        String documentName;
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_HOMEPAGE)) {
            documentName = this.properties.getSpacePageName();
        } else {
            documentName = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE);
        }

        FilterEventParameters documentParameters = new FilterEventParameters();
        if (this.properties.getDefaultLocale() != null) {
            documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, this.properties.getDefaultLocale());
        }

        // > WikiDocument
        proxyFilter.beginWikiDocument(documentName, documentParameters);

        Locale locale = Locale.ROOT;

        FilterEventParameters documentLocaleParameters = new FilterEventParameters();
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR)) {
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR));
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE)) {
            try {
                documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE,
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE));
            } catch (ParseException e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse date", e);
                }
            }
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION)) {
            documentLocaleParameters.put(WikiDocumentFilter.PARAMETER_LASTREVISION,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION));
        }

        // > WikiDocumentLocale
        proxyFilter.beginWikiDocumentLocale(locale, documentLocaleParameters);

        // Revisions
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISIONS)) {
            List<Integer> revisions =
                this.confluencePackage.getIntegertList(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISIONS);
            for (Integer revisionId : revisions) {
                readPageRevision(revisionId, filter, proxyFilter);
            }
        }

        // Current version
        readPageRevision(pageId, filter, proxyFilter);

        // < WikiDocumentLocale
        proxyFilter.endWikiDocumentLocale(locale, documentLocaleParameters);

        // < WikiDocument
        proxyFilter.endWikiDocument(documentName, documentParameters);
    }

    private PropertiesConfiguration getPageProperties(Integer pageId) throws FilterException
    {
        try {
            return this.confluencePackage.getPageProperties(pageId);
        } catch (ConfigurationException e) {
            throw new FilterException("Failed to get page properties", e);
        }
    }

    private void readPageRevision(Integer pageId, Object filter, ConfluenceFilter proxyFilter) throws FilterException
    {
        PropertiesConfiguration pageProperties = getPageProperties(pageId);

        readPageRevision(pageId, pageProperties, filter, proxyFilter);
    }

    private void readPageRevision(int pageId, PropertiesConfiguration pageProperties, Object filter,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        String revision = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION);

        FilterEventParameters documentRevisionParameters = new FilterEventParameters();
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_PARENT)) {
            try {
                documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_PARENT,
                    this.confluencePackage.getReferenceFromId(pageProperties, ConfluenceXMLPackage.KEY_PAGE_PARENT));
            } catch (ConfigurationException e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse parent", e);
                }
            }
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR)) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR));
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE)) {
            try {
                documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE,
                    this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE));
            } catch (ParseException e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse date", e);
                }
            }
        }
        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT)) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT,
                pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT));
        }
        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_TITLE,
            pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE));

        String bodyContent = null;
        Syntax bodySyntax = null;
        int bodyType = -1;

        if (pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_BODY)
            && pageProperties.containsKey(ConfluenceXMLPackage.KEY_PAGE_BODY_TYPE)) {
            bodyContent = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_BODY);
            bodyType = pageProperties.getInt(ConfluenceXMLPackage.KEY_PAGE_BODY_TYPE);

            switch (bodyType) {
                case 0:
                    bodySyntax = Syntax.CONFLUENCE_1_0;
                    break;
                case 2:
                    bodySyntax = Syntax.CONFLUENCEXHTML_1_0;
                    break;
                default:
                    if (this.properties.isVerbose()) {
                        this.logger.error("Unknown body type [{}]", bodyType);
                    }
                    break;
            }
        }

        // If target filter does not support rendering events, pass the content as it is
        if (!(filter instanceof Listener) && bodyContent != null && bodySyntax != null) {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_CONTENT, bodyContent);
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_SYNTAX, bodySyntax);
        }

        // > WikiDocumentRevision
        proxyFilter.beginWikiDocumentRevision(revision, documentRevisionParameters);

        // Content
        if (filter instanceof Listener && bodyContent != null && bodySyntax != null) {
            try {
                switch (bodyType) {
                    case 0:
                        this.confluenceWIKIParser.parse(new StringReader(bodyContent), proxyFilter);
                        break;
                    case 2:
                        this.confluenceXHTMLParser.parse(new StringReader(bodyContent), proxyFilter);
                        break;
                    default:
                        break;
                }
            } catch (org.xwiki.rendering.parser.ParseException e) {
                throw new FilterException(String.format("Failed parser content [%s]", bodyContent), e);
            }
        }

        // Attachments
        Map<String, PropertiesConfiguration> pageAttachments = new LinkedHashMap<>();
        for (int attachmentId : this.confluencePackage.getAttachments(pageId)) {
            PropertiesConfiguration attachmentProperties;
            try {
                attachmentProperties = this.confluencePackage.getAttachmentProperties(pageId, attachmentId);
            } catch (ConfigurationException e) {
                throw new FilterException("Failed to get attachment properties", e);
            }

            String attachmentName = this.confluencePackage.getAttachmentName(attachmentProperties);

            PropertiesConfiguration currentAttachmentProperties = pageAttachments.get(attachmentName);
            if (currentAttachmentProperties != null) {
                int version = this.confluencePackage.getAttachementVersion(attachmentProperties);
                int currentVersion = this.confluencePackage.getAttachementVersion(currentAttachmentProperties);

                if (version > currentVersion) {
                    pageAttachments.put(attachmentName, attachmentProperties);
                }
            } else {
                pageAttachments.put(attachmentName, attachmentProperties);
            }
        }

        for (PropertiesConfiguration attachmentProperties : pageAttachments.values()) {
            readAttachment(pageId, attachmentProperties, filter, proxyFilter);
        }

        // < WikiDocumentRevision
        proxyFilter.endWikiDocumentRevision(revision, documentRevisionParameters);
    }

    private void readAttachment(int pageId, PropertiesConfiguration attachmentProperties, Object filter,
        ConfluenceFilter proxyFilter) throws FilterException
    {
        String contentStatus = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTSTATUS, null);
        if (StringUtils.equals(contentStatus, "deleted")) {
            // The actual deleted attachment is not in the exported package so we can't really do anything with it
            return;
        }

        int attachmentId = attachmentProperties.getInt("id");

        String attachmentName = this.confluencePackage.getAttachmentName(attachmentProperties);

        long attachmentSize;
        String mediaType = null;
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTPROPERTIES)) {
            PropertiesConfiguration attachmentContentProperties =
                getContentProperties(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTPROPERTIES);

            attachmentSize = attachmentContentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_FILESIZE);
            if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE)) {
                mediaType =
                    attachmentContentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_MEDIA_TYPE);
            }
        } else {
            attachmentSize = attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_SIZE);
            if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE)) {
                mediaType = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENTTYPE);
            }
        }

        Integer version = this.confluencePackage.getAttachementVersion(attachmentProperties);

        int originalRevisionId =
            this.confluencePackage.getAttachmentOriginalVersionId(attachmentProperties, attachmentId);
        File contentFile;
        try {
            contentFile = this.confluencePackage.getAttachmentFile(pageId, originalRevisionId, version);
        } catch (FileNotFoundException e) {
            throw new FilterException(
                String.format("Filed to find file corresponding to version [%s] attachment [%s] in page [%s]", version,
                    attachmentName, pageId),
                e);
        }

        FilterEventParameters attachmentParameters = new FilterEventParameters();
        if (mediaType != null) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CONTENT_TYPE, mediaType);
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_AUTHOR,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR));
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE)) {
            try {
                attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_DATE, this.confluencePackage
                    .getDate(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE));
            } catch (ParseException e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse date", e);
                }
            }
        }

        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION, String.valueOf(version));
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR));
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE)) {
            try {
                attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, this.confluencePackage
                    .getDate(attachmentProperties, ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE));
            } catch (ParseException e) {
                if (this.properties.isVerbose()) {
                    this.logger.error("Failed to parse date", e);
                }
            }
        }
        if (attachmentProperties.containsKey(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT)) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT,
                attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT));
        }

        // WikiAttachment

        try {
            FileInputStream fis = new FileInputStream(contentFile);

            try {
                proxyFilter.onWikiAttachment(attachmentName, fis, attachmentSize, attachmentParameters);
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            throw new FilterException("Failed to read attachment", e);
        }
    }

    public PropertiesConfiguration getContentProperties(PropertiesConfiguration properties, String key)
        throws FilterException
    {
        try {
            return this.confluencePackage.getContentProperties(properties, key);
        } catch (Exception e) {
            throw new FilterException("Failed to parse content properties", e);
        }
    }
}
