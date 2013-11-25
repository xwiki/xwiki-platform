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
package org.xwiki.wikistream.confluence.xml.internal.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.confluence.input.ConfluenceInputProperties;
import org.xwiki.wikistream.confluence.xml.internal.ConfluenceFilter;
import org.xwiki.wikistream.confluence.xml.internal.ConfluenceXMLPackage;
import org.xwiki.wikistream.filter.user.GroupFilter;
import org.xwiki.wikistream.filter.user.UserFilter;
import org.xwiki.wikistream.internal.input.AbstractBeanInputWikiStream;
import org.xwiki.wikistream.model.filter.WikiAttachmentFilter;
import org.xwiki.wikistream.model.filter.WikiDocumentFilter;

/**
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Named(ConfluenceInputWikiStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConfluenceInputWikiStream extends AbstractBeanInputWikiStream<ConfluenceInputProperties, ConfluenceFilter>
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
    protected void read(Object filter, ConfluenceFilter proxyFilter) throws WikiStreamException
    {
        // Prepare package
        try {
            this.confluencePackage = new ConfluenceXMLPackage(this.properties.getSource());
        } catch (Exception e) {
            throw new WikiStreamException("Failed to read package", e);
        }

        // Generate users events
        for (int userInt : this.confluencePackage.getUsers()) {
            PropertiesConfiguration userProperties;
            try {
                userProperties = this.confluencePackage.getUserProperties(userInt);
            } catch (ConfigurationException e) {
                throw new WikiStreamException("Failed to get user properties", e);
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
                throw new WikiStreamException("Failed to get group properties", e);
            }

            String groupId = groupProperties.getString(ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(groupInt));
            if (this.properties.isConvertToXWiki()) {
                if (groupId.equals("confluence-administrators")) {
                    groupId = "XWikiAdminGroup";
                } else if (groupId.equals("confluence-users")) {
                    groupId = "XWikiAllGroup";
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
            proxyFilter.beginGroup(groupId, groupParameters);

            // Members users
            if (groupProperties.containsKey(ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS)) {
                List<Integer> users =
                    this.confluencePackage.getIntegertList(groupProperties, ConfluenceXMLPackage.KEY_GROUP_MEMBERUSERS);
                for (Integer memberInt : users) {
                    FilterEventParameters memberParameters = new FilterEventParameters();

                    try {
                        String memberId =
                            this.confluencePackage.getUserProperties(memberInt).getString(
                                ConfluenceXMLPackage.KEY_USER_NAME, String.valueOf(memberInt));

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
                List<Integer> groups =
                    this.confluencePackage
                        .getIntegertList(groupProperties, ConfluenceXMLPackage.KEY_GROUP_MEMBERGROUPS);
                for (Integer memberInt : groups) {
                    FilterEventParameters memberParameters = new FilterEventParameters();

                    try {
                        String memberId =
                            this.confluencePackage.getGroupProperties(memberInt).getString(
                                ConfluenceXMLPackage.KEY_GROUP_NAME, String.valueOf(memberInt));

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
            proxyFilter.endGroup(groupId, groupParameters);
        }

        // Generate documents events
        for (Map.Entry<Integer, Set<Integer>> entry : this.confluencePackage.getPages().entrySet()) {
            int spaceId = entry.getKey();

            PropertiesConfiguration spaceProperties;
            try {
                spaceProperties = this.confluencePackage.getSpaceProperties(spaceId);
            } catch (ConfigurationException e) {
                throw new WikiStreamException("Failed to get space properties", e);
            }

            String spaceName = spaceProperties.getString(ConfluenceXMLPackage.KEY_SPACE_NAME);
            FilterEventParameters spaceParameters = new FilterEventParameters();

            // > WikiSpace
            proxyFilter.beginWikiSpace(spaceName, spaceParameters);

            // Main page
            if (spaceProperties.containsKey(ConfluenceXMLPackage.KEY_SPACE_DESCRIPTION)) {
                readPage(spaceProperties.getInt(ConfluenceXMLPackage.KEY_SPACE_DESCRIPTION), filter, proxyFilter);
            }

            // Other pages
            for (int pageId : entry.getValue()) {
                readPage(pageId, filter, proxyFilter);
            }

            // < WikiSpace
            proxyFilter.endWikiSpace(spaceName, spaceParameters);
        }

        // try {
        // this.confluencePackage.close();
        // } catch (IOException e) {
        // throw new WikiStreamException("Failed to close package", e);
        // }
    }

    private void readPage(int pageId, Object filter, ConfluenceFilter proxyFilter) throws WikiStreamException
    {
        PropertiesConfiguration pageProperties;
        try {
            pageProperties = this.confluencePackage.getPageProperties(pageId);
        } catch (ConfigurationException e) {
            throw new WikiStreamException("Failed to get page properties", e);
        }

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
        documentParameters.put(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR,
            pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_CREATION_AUTHOR));
        try {
            documentParameters.put(WikiDocumentFilter.PARAMETER_CREATION_DATE,
                this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_CREATION_DATE));
        } catch (ParseException e) {
            if (this.properties.isVerbose()) {
                this.logger.error("Failed to parse date", e);
            }
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
        readPageRevision(pageId, pageProperties, proxyFilter);

        // < WikiDocumentLocale
        proxyFilter.endWikiDocumentLocale(locale, documentLocaleParameters);

        // < WikiDocument
        proxyFilter.endWikiDocument(documentName, documentParameters);
    }

    private void readPageRevision(Integer pageId, Object filter, ConfluenceFilter proxyFilter)
        throws WikiStreamException
    {
        PropertiesConfiguration pageProperties;
        try {
            pageProperties = this.confluencePackage.getPageProperties(pageId);
        } catch (ConfigurationException e) {
            throw new WikiStreamException("Failed to get page properties", e);
        }

        readPageRevision(pageId, pageProperties, filter, proxyFilter);
    }

    private void readPageRevision(int pageId, PropertiesConfiguration pageProperties, Object filter,
        ConfluenceFilter proxyFilter) throws WikiStreamException
    {
        String revision = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION);

        FilterEventParameters documentRevisionParameters = new FilterEventParameters();
        try {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_PARENT,
                this.confluencePackage.getReferenceFromId(pageProperties, ConfluenceXMLPackage.KEY_PAGE_PARENT));
        } catch (ConfigurationException e) {
            if (this.properties.isVerbose()) {
                this.logger.error("Failed to parse parent", e);
            }
        }
        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION,
            pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION));
        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR,
            pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_AUTHOR));
        try {
            documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_DATE,
                this.confluencePackage.getDate(pageProperties, ConfluenceXMLPackage.KEY_PAGE_REVISION_DATE));
        } catch (ParseException e) {
            if (this.properties.isVerbose()) {
                this.logger.error("Failed to parse date", e);
            }
        }
        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_REVISION_COMMENT,
            pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_REVISION_COMMENT));
        documentRevisionParameters.put(WikiDocumentFilter.PARAMETER_TITLE,
            pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_TITLE));

        // > WikiDocumentRevision
        proxyFilter.beginWikiDocumentRevision(revision, documentRevisionParameters);

        // Content
        String confluenceBody = pageProperties.getString(ConfluenceXMLPackage.KEY_PAGE_BODY);
        int bodyType = pageProperties.getInt(ConfluenceXMLPackage.KEY_PAGE_BODY_TYPE);

        try {
            switch (bodyType) {
                case 0:
                    this.confluenceWIKIParser.parse(new StringReader(confluenceBody), proxyFilter);
                    break;
                case 2:
                    this.confluenceXHTMLParser.parse(new StringReader(confluenceBody), proxyFilter);
                    break;
                default:
                    if (this.properties.isVerbose()) {
                        this.logger.error("Usupported body type [{}]", bodyType);
                    }
                    break;
            }
        } catch (org.xwiki.rendering.parser.ParseException e) {
            throw new WikiStreamException(String.format("Failed parser content [%s]", confluenceBody), e);
        }

        // Attachments
        for (int attachmentId : this.confluencePackage.getAttachments(pageId)) {
            readAttachment(pageId, attachmentId, filter, proxyFilter);
        }

        // < WikiDocumentRevision
        proxyFilter.endWikiDocumentRevision(revision, documentRevisionParameters);
    }

    private void readAttachment(int pageId, int attachmentId, Object filter, ConfluenceFilter proxyFilter)
        throws WikiStreamException
    {
        PropertiesConfiguration attachmentProperties;
        try {
            attachmentProperties = this.confluencePackage.getAttachmentProperties(pageId, attachmentId);
        } catch (ConfigurationException e) {
            throw new WikiStreamException("Failed to get attachment properties", e);
        }

        String attachmentName = attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_NAME);
        long attachmentSize = attachmentProperties.getLong(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_SIZE);

        int version = attachmentProperties.getInt(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION);
        File contentFile = this.confluencePackage.getAttachmentFile(pageId, attachmentId, version);

        FilterEventParameters attachmentParameters = new FilterEventParameters();
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CONTENT_TYPE,
            attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CONTENT_TYPE));
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CREATION_AUTHOR,
            attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_AUTHOR));
        try {
            attachmentParameters
                .put(WikiAttachmentFilter.PARAMETER_CREATION_DATE, this.confluencePackage.getDate(attachmentProperties,
                    ConfluenceXMLPackage.KEY_ATTACHMENT_CREATION_DATE));
        } catch (ParseException e) {
            if (this.properties.isVerbose()) {
                this.logger.error("Failed to parse date", e);
            }
        }
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION, String.valueOf(version));
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR,
            attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_AUTHOR));
        try {
            attachmentParameters
                .put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, this.confluencePackage.getDate(attachmentProperties,
                    ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_DATE));
        } catch (ParseException e) {
            if (this.properties.isVerbose()) {
                this.logger.error("Failed to parse date", e);
            }
        }
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT,
            attachmentProperties.getString(ConfluenceXMLPackage.KEY_ATTACHMENT_REVISION_COMMENT));

        // WikiAttachment

        try {
            FileInputStream fis = new FileInputStream(contentFile);

            try {
                proxyFilter.onWikiAttachment(attachmentName, fis, attachmentSize, attachmentParameters);
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            throw new WikiStreamException("Failed to read attachment", e);
        }
    }
}
