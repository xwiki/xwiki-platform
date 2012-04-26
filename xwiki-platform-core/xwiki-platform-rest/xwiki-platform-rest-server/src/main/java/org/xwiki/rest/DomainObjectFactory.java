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
package org.xwiki.rest;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attribute;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.model.jaxb.Translation;
import org.xwiki.rest.model.jaxb.Translations;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Xwiki;
import org.xwiki.rest.resources.ModificationsResource;
import org.xwiki.rest.resources.SyntaxesResource;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.attachments.AttachmentVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentsAtPageVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.rest.resources.classes.ClassPropertiesResource;
import org.xwiki.rest.resources.classes.ClassPropertyResource;
import org.xwiki.rest.resources.classes.ClassResource;
import org.xwiki.rest.resources.classes.ClassesResource;
import org.xwiki.rest.resources.comments.CommentsResource;
import org.xwiki.rest.resources.comments.CommentsVersionResource;
import org.xwiki.rest.resources.objects.AllObjectsForClassNameResource;
import org.xwiki.rest.resources.objects.ObjectAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectPropertiesAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectPropertiesResource;
import org.xwiki.rest.resources.objects.ObjectPropertyAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectPropertyResource;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.resources.pages.PageChildrenResource;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTagsResource;
import org.xwiki.rest.resources.pages.PageTranslationHistoryResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.pages.PageTranslationVersionResource;
import org.xwiki.rest.resources.pages.PageVersionResource;
import org.xwiki.rest.resources.pages.PagesResource;
import org.xwiki.rest.resources.spaces.SpaceResource;
import org.xwiki.rest.resources.spaces.SpaceSearchResource;
import org.xwiki.rest.resources.spaces.SpacesResource;
import org.xwiki.rest.resources.wikis.WikiSearchResource;
import org.xwiki.rest.resources.wikis.WikisResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.PropertyClass;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * <p>
 * This class contains utility methods for building representations using model objects.
 * </p>
 * 
 * @version $Id$
 */
public class DomainObjectFactory
{
    public static Xwiki createXWikiRoot(ObjectFactory objectFactory, URI baseUri, String version)
    {
        Xwiki xwiki = objectFactory.createXwiki().withVersion(version);

        String wikisUri = UriBuilder.fromUri(baseUri).path(WikisResource.class).build().toString();
        Link wikisLink = objectFactory.createLink();
        wikisLink.setHref(wikisUri);
        wikisLink.setRel(Relations.WIKIS);
        xwiki.getLinks().add(wikisLink);

        String syntaxesUri = UriBuilder.fromUri(baseUri).path(SyntaxesResource.class).build().toString();
        Link syntaxesLink = objectFactory.createLink();
        syntaxesLink.setHref(syntaxesUri);
        syntaxesLink.setRel(Relations.SYNTAXES);
        xwiki.getLinks().add(syntaxesLink);

        return xwiki;
    }

    public static Wiki createWiki(ObjectFactory objectFactory, URI baseUri, String wikiName)
    {
        Wiki wiki = objectFactory.createWiki().withId(wikiName).withName(wikiName);

        String spacesUri = UriBuilder.fromUri(baseUri).path(SpacesResource.class).build(wikiName).toString();
        Link spacesLink = objectFactory.createLink();
        spacesLink.setHref(spacesUri);
        spacesLink.setRel(Relations.SPACES);
        wiki.getLinks().add(spacesLink);

        String classesUri = UriBuilder.fromUri(baseUri).path(ClassesResource.class).build(wikiName).toString();
        Link classesLink = objectFactory.createLink();
        classesLink.setHref(classesUri);
        classesLink.setRel(Relations.CLASSES);
        wiki.getLinks().add(classesLink);

        String modificationsUri =
            UriBuilder.fromUri(baseUri).path(ModificationsResource.class).build(wikiName).toString();
        Link modificationsLink = objectFactory.createLink();
        modificationsLink.setHref(modificationsUri);
        modificationsLink.setRel(Relations.MODIFICATIONS);
        wiki.getLinks().add(modificationsLink);

        String searchUri = UriBuilder.fromUri(baseUri).path(WikiSearchResource.class).build(wikiName).toString();
        Link searchLink = objectFactory.createLink();
        searchLink.setHref(searchUri);
        searchLink.setRel(Relations.SEARCH);
        wiki.getLinks().add(searchLink);

        return wiki;
    }

    public static Space createSpace(ObjectFactory objectFactory, URI baseUri, String wikiName, String spaceName,
        Document home)
    {
        Space space = objectFactory.createSpace();
        space.setId(String.format("%s:%s", wikiName, spaceName));
        space.setWiki(wikiName);
        space.setName(spaceName);
        if (home != null) {
            space.setHome(home.getPrefixedFullName());
            space.setXwikiRelativeUrl(home.getURL("view"));
            space.setXwikiAbsoluteUrl(home.getExternalURL("view"));
        }

        String pagesUri = UriBuilder.fromUri(baseUri).path(PagesResource.class).build(wikiName, spaceName).toString();
        Link pagesLink = objectFactory.createLink();
        pagesLink.setHref(pagesUri);
        pagesLink.setRel(Relations.PAGES);
        space.getLinks().add(pagesLink);

        if (home != null) {
            String homeUri =
                UriBuilder.fromUri(baseUri).path(PageResource.class).build(wikiName, spaceName, home.getName())
                    .toString();
            Link homeLink = objectFactory.createLink();
            homeLink.setHref(homeUri);
            homeLink.setRel(Relations.HOME);
            space.getLinks().add(homeLink);
        }

        String searchUri =
            UriBuilder.fromUri(baseUri).path(SpaceSearchResource.class).build(wikiName, spaceName).toString();
        Link searchLink = objectFactory.createLink();
        searchLink.setHref(searchUri);
        searchLink.setRel(Relations.SEARCH);
        space.getLinks().add(searchLink);

        return space;
    }

    public static Translations createTranslations(ObjectFactory objectFactory, URI baseUri, Document doc)
        throws XWikiException
    {
        Translations translations = objectFactory.createTranslations();

        List<String> languages = doc.getTranslationList();

        if (!languages.isEmpty()) {
            if (!doc.getDefaultLanguage().equals("")) {
                translations.setDefault(doc.getDefaultLanguage());

                Translation translation = objectFactory.createTranslation();
                translation.setLanguage(doc.getDefaultLanguage());

                /* Add the default page with the default translation explicitely */
                String pageTranslationUri =
                    UriBuilder.fromUri(baseUri).path(PageResource.class).build(doc.getWiki(), doc.getSpace(),
                        doc.getName()).toString();
                Link pageTranslationLink = objectFactory.createLink();
                pageTranslationLink.setHref(pageTranslationUri);
                pageTranslationLink.setRel(Relations.PAGE);
                translation.getLinks().add(pageTranslationLink);

                String historyUri =
                    UriBuilder.fromUri(baseUri).path(PageHistoryResource.class).build(doc.getWiki(), doc.getSpace(),
                        doc.getName()).toString();
                Link historyLink = objectFactory.createLink();
                historyLink.setHref(historyUri);
                historyLink.setRel(Relations.HISTORY);
                translation.getLinks().add(historyLink);

                translations.getTranslations().add(translation);
            }
        }

        for (String language : languages) {
            Translation translation = objectFactory.createTranslation();
            translation.setLanguage(language);

            String pageTranslationUri =
                UriBuilder.fromUri(baseUri).path(PageTranslationResource.class).build(doc.getWiki(), doc.getSpace(),
                    doc.getName(), language).toString();
            Link pageTranslationLink = objectFactory.createLink();
            pageTranslationLink.setHref(pageTranslationUri);
            pageTranslationLink.setRel(Relations.PAGE);
            translation.getLinks().add(pageTranslationLink);

            String historyUri =
                UriBuilder.fromUri(baseUri).path(PageTranslationHistoryResource.class).build(doc.getWiki(),
                    doc.getSpace(), doc.getName(), language).toString();
            Link historyLink = objectFactory.createLink();
            historyLink.setHref(historyUri);
            historyLink.setRel(Relations.HISTORY);
            translation.getLinks().add(historyLink);

            translations.getTranslations().add(translation);
        }

        return translations;
    }

    /* This method is used to fill the "common part" of a Page and a PageSummary */
    private static void fillPageSummary(PageSummary pageSummary, ObjectFactory objectFactory, URI baseUri,
        Document doc, boolean useVersion, XWiki xwikiApi) throws XWikiException
    {
        pageSummary.setWiki(doc.getWiki());
        pageSummary.setFullName(doc.getFullName());
        pageSummary.setId(doc.getPrefixedFullName());
        pageSummary.setSpace(doc.getSpace());
        pageSummary.setName(doc.getName());
        pageSummary.setTitle(doc.getDisplayTitle());
        pageSummary.setXwikiRelativeUrl(doc.getURL("view"));
        pageSummary.setXwikiAbsoluteUrl(doc.getExternalURL("view"));
        pageSummary.setTranslations(createTranslations(objectFactory, baseUri, doc));
        pageSummary.setSyntax(doc.getSyntaxId());

        Document parent = Utils.getParentDocument(doc, xwikiApi);
        pageSummary.setParent(doc.getParent());
        // parentId must not be set if the parent document does not exist.
        if (parent != null && !parent.isNew()) {
            pageSummary.setParentId(parent.getPrefixedFullName());
        } else {
            pageSummary.setParentId("");
        }

        String spaceUri =
            UriBuilder.fromUri(baseUri).path(SpaceResource.class).build(doc.getWiki(), doc.getSpace()).toString();
        Link spaceLink = objectFactory.createLink();
        spaceLink.setHref(spaceUri);
        spaceLink.setRel(Relations.SPACE);
        pageSummary.getLinks().add(spaceLink);

        if (parent != null) {
            String parentUri =
                UriBuilder.fromUri(baseUri).path(PageResource.class).build(parent.getWiki(), parent.getSpace(),
                    parent.getName()).toString();
            Link parentLink = objectFactory.createLink();
            parentLink.setHref(parentUri);
            parentLink.setRel(Relations.PARENT);
            pageSummary.getLinks().add(parentLink);
        }

        String historyUri =
            UriBuilder.fromUri(baseUri).path(PageHistoryResource.class).build(doc.getWiki(), doc.getSpace(),
                doc.getName()).toString();
        Link historyLink = objectFactory.createLink();
        historyLink.setHref(historyUri);
        historyLink.setRel(Relations.HISTORY);
        pageSummary.getLinks().add(historyLink);

        if (!doc.getChildren().isEmpty()) {
            String pageChildrenUri =
                UriBuilder.fromUri(baseUri).path(PageChildrenResource.class).build(doc.getWiki(), doc.getSpace(),
                    doc.getName()).toString();
            Link pageChildrenLink = objectFactory.createLink();
            pageChildrenLink.setHref(pageChildrenUri);
            pageChildrenLink.setRel(Relations.CHILDREN);
            pageSummary.getLinks().add(pageChildrenLink);
        }

        if (!doc.getComments().isEmpty()) {
            String commentsUri;
            if (useVersion) {
                commentsUri =
                    UriBuilder.fromUri(baseUri).path(CommentsVersionResource.class).build(doc.getWiki(),
                        doc.getSpace(), doc.getName(), doc.getVersion()).toString();
            } else {
                commentsUri =
                    UriBuilder.fromUri(baseUri).path(CommentsResource.class).build(doc.getWiki(), doc.getSpace(),
                        doc.getName()).toString();
            }

            Link commentsLink = objectFactory.createLink();
            commentsLink.setHref(commentsUri);
            commentsLink.setRel(Relations.COMMENTS);
            pageSummary.getLinks().add(commentsLink);
        }

        if (!doc.getAttachmentList().isEmpty()) {
            String attachmentsUri;
            if (useVersion) {
                attachmentsUri =
                    UriBuilder.fromUri(baseUri).path(AttachmentsAtPageVersionResource.class).build(doc.getWiki(),
                        doc.getSpace(), doc.getName(), doc.getVersion()).toString();
            } else {
                attachmentsUri =
                    UriBuilder.fromUri(baseUri).path(AttachmentsResource.class).build(doc.getWiki(), doc.getSpace(),
                        doc.getName()).toString();
            }

            Link attachmentsLink = objectFactory.createLink();
            attachmentsLink.setHref(attachmentsUri);
            attachmentsLink.setRel(Relations.ATTACHMENTS);
            pageSummary.getLinks().add(attachmentsLink);
        }

        if (!doc.getxWikiObjects().keySet().isEmpty()) {
            String objectsUri;

            if (useVersion) {
                objectsUri =
                    UriBuilder.fromUri(baseUri).path(ObjectsAtPageVersionResource.class).build(doc.getWiki(),
                        doc.getSpace(), doc.getName(), doc.getVersion()).toString();
            } else {
                objectsUri =
                    UriBuilder.fromUri(baseUri).path(ObjectsResource.class).build(doc.getWiki(), doc.getSpace(),
                        doc.getName()).toString();
            }
            Link objectsLink = objectFactory.createLink();
            objectsLink.setHref(objectsUri);
            objectsLink.setRel(Relations.OBJECTS);
            pageSummary.getLinks().add(objectsLink);
        }

        com.xpn.xwiki.api.Object tagsObject = doc.getObject("XWiki.TagClass", 0);
        if (tagsObject != null) {
            if (tagsObject.getProperty("tags") != null) {
                String tagsUri =
                    UriBuilder.fromUri(baseUri).path(PageTagsResource.class).build(doc.getWiki(), doc.getSpace(),
                        doc.getName()).toString();
                Link tagsLink = objectFactory.createLink();
                tagsLink.setHref(tagsUri);
                tagsLink.setRel(Relations.TAGS);
                pageSummary.getLinks().add(tagsLink);
            }
        }

        String syntaxesUri = UriBuilder.fromUri(baseUri).path(SyntaxesResource.class).build().toString();
        Link syntaxesLink = objectFactory.createLink();
        syntaxesLink.setHref(syntaxesUri);
        syntaxesLink.setRel(Relations.SYNTAXES);
        pageSummary.getLinks().add(syntaxesLink);
    }

    public static PageSummary createPageSummary(ObjectFactory objectFactory, URI baseUri, Document doc, XWiki xwikiApi)
        throws XWikiException
    {
        PageSummary pageSummary = objectFactory.createPageSummary();
        fillPageSummary(pageSummary, objectFactory, baseUri, doc, false, xwikiApi);

        String pageUri =
            UriBuilder.fromUri(baseUri).path(PageResource.class).build(doc.getWiki(), doc.getSpace(), doc.getName())
                .toString();
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        pageSummary.getLinks().add(pageLink);

        return pageSummary;
    }

    public static Page createPage(ObjectFactory objectFactory, URI baseUri, URI self, Document doc, boolean useVersion,
        XWiki xwikiApi) throws XWikiException
    {
        Page page = objectFactory.createPage();
        fillPageSummary(page, objectFactory, baseUri, doc, useVersion, xwikiApi);

        page.setVersion(doc.getVersion());
        page.setMajorVersion(doc.getRCSVersion().at(0));
        page.setMinorVersion(doc.getRCSVersion().at(1));
        page.setLanguage(doc.getLanguage());
        page.setCreator(doc.getCreator());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(doc.getCreationDate());
        page.setCreated(calendar);

        page.setModifier(doc.getContentAuthor());

        calendar = Calendar.getInstance();
        calendar.setTime(doc.getContentUpdateDate());
        page.setModified(calendar);

        page.setContent(doc.getContent());

        if (self != null) {
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(self.toString());
            pageLink.setRel(Relations.SELF);
            page.getLinks().add(pageLink);
        }

        com.xpn.xwiki.api.Class xwikiClass = doc.getxWikiClass();
        if (xwikiClass != null) {
            String classUri =
                UriBuilder.fromUri(baseUri).path(ClassResource.class).build(doc.getWiki(), xwikiClass.getName())
                    .toString();
            Link classLink = objectFactory.createLink();
            classLink.setHref(classUri);
            classLink.setRel(Relations.CLASS);
            page.getLinks().add(classLink);
        }

        return page;
    }

    public static HistorySummary createHistorySummary(ObjectFactory objectFactory, URI baseUri, String wikiName,
        String spaceName, String pageName, String language, Version version, String modifier, Date modified)
    {
        HistorySummary historySummary = objectFactory.createHistorySummary();

        String pageId = Utils.getPageId(wikiName, spaceName, pageName);

        historySummary.setPageId(pageId);
        historySummary.setWiki(wikiName);
        historySummary.setSpace(spaceName);
        historySummary.setName(pageName);
        historySummary.setVersion(version.toString());
        historySummary.setMajorVersion(version.at(0));
        historySummary.setMinorVersion(version.at(1));
        historySummary.setModifier(modifier);
        historySummary.setLanguage(language);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(modified);
        historySummary.setModified(calendar);

        if (language == null) {
            String pageUri =
                UriBuilder.fromUri(baseUri).path(PageVersionResource.class).build(wikiName, spaceName, pageName,
                    version).toString();
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(pageUri);
            pageLink.setRel(Relations.PAGE);
            historySummary.getLinks().add(pageLink);
        } else {
            String pageUri =
                UriBuilder.fromUri(baseUri).path(PageTranslationVersionResource.class).build(wikiName, spaceName,
                    pageName, language, version).toString();
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(pageUri);
            pageLink.setRel(Relations.PAGE);
            historySummary.getLinks().add(pageLink);
        }

        return historySummary;
    }

    private static void fillAttachment(Attachment attachment, ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl)
    {
        Document doc = xwikiAttachment.getDocument();

        attachment.setId(String.format("%s@%s", doc.getPrefixedFullName(), xwikiAttachment.getFilename()));
        attachment.setName(xwikiAttachment.getFilename());
        attachment.setSize(xwikiAttachment.getFilesize());
        attachment.setVersion(xwikiAttachment.getVersion());
        attachment.setPageId(doc.getPrefixedFullName());
        attachment.setPageVersion(doc.getVersion());
        attachment.setMimeType(xwikiAttachment.getMimeType());
        attachment.setAuthor(xwikiAttachment.getAuthor());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(xwikiAttachment.getDate());
        attachment.setDate(calendar);

        attachment.setXwikiRelativeUrl(xwikiRelativeUrl);
        attachment.setXwikiAbsoluteUrl(xwikiAbsoluteUrl);

        String pageUri =
            UriBuilder.fromUri(baseUri).path(PageResource.class).build(doc.getWiki(), doc.getSpace(), doc.getName())
                .toString();
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        attachment.getLinks().add(pageLink);
    }

    public static Attachment createAttachment(ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl)
    {
        Attachment attachment = objectFactory.createAttachment();

        fillAttachment(attachment, objectFactory, baseUri, xwikiAttachment, xwikiRelativeUrl, xwikiAbsoluteUrl);

        Document doc = xwikiAttachment.getDocument();

        String attachmentUri =
            UriBuilder.fromUri(baseUri).path(AttachmentResource.class).build(doc.getWiki(), doc.getSpace(),
                doc.getName(), xwikiAttachment.getFilename()).toString();
        Link attachmentLink = objectFactory.createLink();
        attachmentLink.setHref(attachmentUri);
        attachmentLink.setRel(Relations.ATTACHMENT_DATA);
        attachment.getLinks().add(attachmentLink);

        return attachment;
    }

    public static Attachment createAttachmentAtVersion(ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl)
    {
        Attachment attachment = new Attachment();

        fillAttachment(attachment, objectFactory, baseUri, xwikiAttachment, xwikiRelativeUrl, xwikiAbsoluteUrl);

        Document doc = xwikiAttachment.getDocument();

        String attachmentUri =
            UriBuilder.fromUri(baseUri).path(AttachmentVersionResource.class).build(doc.getWiki(), doc.getSpace(),
                doc.getName(), xwikiAttachment.getFilename(), xwikiAttachment.getVersion()).toString();
        Link attachmentLink = objectFactory.createLink();
        attachmentLink.setHref(attachmentUri);
        attachmentLink.setRel(Relations.ATTACHMENT_DATA);
        attachment.getLinks().add(attachmentLink);

        return attachment;
    }

    public static Comment createComment(ObjectFactory objectFactory, URI baseUri, Document doc,
        com.xpn.xwiki.api.Object xwikiComment)
    {
        Comment comment = objectFactory.createComment();
        comment.setId(xwikiComment.getNumber());

        com.xpn.xwiki.api.Property property = xwikiComment.getProperty("author");
        if (property != null) {
            comment.setAuthor((String) property.getValue());
        }

        property = xwikiComment.getProperty("date");
        if (property != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime((Date) property.getValue());
            comment.setDate(calendar);
        }

        property = xwikiComment.getProperty("highlight");
        if (property != null) {
            comment.setHighlight((String) property.getValue());
        }

        property = xwikiComment.getProperty("comment");
        if (property != null) {
            comment.setText((String) property.getValue());
        }

        property = xwikiComment.getProperty("replyto");
        if (property != null) {
            comment.setReplyTo((Integer) property.getValue());
        }
        
        String pageUri =
            UriBuilder.fromUri(baseUri).path(PageResource.class).build(doc.getWiki(), doc.getSpace(), doc.getName())
                .toString();
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        comment.getLinks().add(pageLink);

        return comment;
    }

    private static void fillObjectSummary(ObjectSummary objectSummary, ObjectFactory objectFactory, URI baseUri,
        Document doc, BaseObject xwikiObject) throws XWikiException
    {
        objectSummary.setId(String.format("%s:%s", doc.getPrefixedFullName(), xwikiObject.getGuid()));
        objectSummary.setGuid(xwikiObject.getGuid());
        objectSummary.setPageId(doc.getPrefixedFullName());
        objectSummary.setWiki(doc.getWiki());
        objectSummary.setSpace(doc.getSpace());
        objectSummary.setPageName(doc.getName());
        objectSummary.setClassName(xwikiObject.getClassName());
        objectSummary.setNumber(xwikiObject.getNumber());

        String[] propertyNames = xwikiObject.getPropertyNames();
        if (propertyNames.length > 0) {
            objectSummary.setHeadline(xwikiObject.get(propertyNames[0]).toFormString());
        }

    }

    public static ObjectSummary createObjectSummary(ObjectFactory objectFactory, URI baseUri,
        XWikiContext xwikiContext, Document doc, BaseObject xwikiObject, boolean useVersion) throws XWikiException
    {
        ObjectSummary objectSummary = objectFactory.createObjectSummary();
        fillObjectSummary(objectSummary, objectFactory, baseUri, doc, xwikiObject);

        Link objectLink = getObjectLink(objectFactory, baseUri, doc, xwikiObject, useVersion, Relations.OBJECT);
        objectSummary.getLinks().add(objectLink);

        String propertiesUri;

        if (useVersion) {
            propertiesUri =
                UriBuilder.fromUri(baseUri).path(ObjectPropertiesAtPageVersionResource.class).build(doc.getWiki(),
                    doc.getSpace(), doc.getName(), doc.getVersion(), xwikiObject.getClassName(),
                    xwikiObject.getNumber()).toString();
        } else {
            propertiesUri =
                UriBuilder.fromUri(baseUri).path(ObjectPropertiesResource.class).build(doc.getWiki(), doc.getSpace(),
                    doc.getName(), xwikiObject.getClassName(), xwikiObject.getNumber()).toString();
        }

        Link propertyLink = objectFactory.createLink();
        propertyLink.setHref(propertiesUri);
        propertyLink.setRel(Relations.PROPERTIES);
        objectSummary.getLinks().add(propertyLink);

        return objectSummary;
    }

    public static Object createObject(ObjectFactory objectFactory, URI baseUri, XWikiContext xwikiContext,
        Document doc, BaseObject xwikiObject, boolean useVersion) throws XWikiException
    {
        Object object = objectFactory.createObject();
        fillObjectSummary(object, objectFactory, baseUri, doc, xwikiObject);

        BaseClass xwikiClass = xwikiObject.getXClass(xwikiContext);

        for (java.lang.Object propertyClassObject : xwikiClass.getProperties()) {
            com.xpn.xwiki.objects.classes.PropertyClass propertyClass =
                (com.xpn.xwiki.objects.classes.PropertyClass) propertyClassObject;

            Property property = objectFactory.createProperty();

            for (java.lang.Object o : propertyClass.getProperties()) {
                BaseProperty baseProperty = (BaseProperty) o;
                Attribute attribute = objectFactory.createAttribute();
                attribute.setName(baseProperty.getName());

                /* Check for null values in order to prevent NPEs */
                if (baseProperty.getValue() != null) {
                    attribute.setValue(baseProperty.getValue().toString());
                } else {
                    attribute.setValue("");
                }

                property.getAttributes().add(attribute);
            }

            if (propertyClass instanceof ListClass) {
                ListClass listClass = (ListClass) propertyClass;

                List allowedValueList = listClass.getList(xwikiContext);

                if (!allowedValueList.isEmpty()) {
                    Formatter f = new Formatter();
                    for (int i = 0; i < allowedValueList.size(); i++) {
                        if (i != allowedValueList.size() - 1) {
                            f.format("%s,", allowedValueList.get(i).toString());
                        } else {
                            f.format("%s", allowedValueList.get(i).toString());
                        }
                    }

                    Attribute attribute = objectFactory.createAttribute();
                    attribute.setName(Constants.ALLOWED_VALUES_ATTRIBUTE_NAME);
                    attribute.setValue(f.toString());
                    property.getAttributes().add(attribute);
                }
            }

            property.setName(propertyClass.getName());
            property.setType(propertyClass.getClassType());
            if (xwikiObject.get(propertyClass.getName()) != null) {
                property.setValue(xwikiObject.get(propertyClass.getName()).toFormString());
            } else {
                property.setValue("");
            }

            String propertyUri;

            if (useVersion) {
                propertyUri =
                    UriBuilder.fromUri(baseUri).path(ObjectPropertyAtPageVersionResource.class).build(doc.getWiki(),
                        doc.getSpace(), doc.getName(), doc.getVersion(), xwikiObject.getClassName(),
                        xwikiObject.getNumber(), propertyClass.getName()).toString();
            } else {
                propertyUri =
                    UriBuilder.fromUri(baseUri).path(ObjectPropertyResource.class).build(doc.getWiki(), doc.getSpace(),
                        doc.getName(), xwikiObject.getClassName(), xwikiObject.getNumber(), propertyClass.getName())
                        .toString();
            }
            Link propertyLink = objectFactory.createLink();
            propertyLink.setHref(propertyUri);
            propertyLink.setRel(Relations.SELF);
            property.getLinks().add(propertyLink);

            object.getProperties().add(property);

        }

        Link objectLink = getObjectLink(objectFactory, baseUri, doc, xwikiObject, useVersion, Relations.SELF);
        object.getLinks().add(objectLink);

        return object;
    }

    private static Link getObjectLink(ObjectFactory objectFactory, URI baseUri, Document doc, BaseObject xwikiObject,
        boolean useVersion, String relation)
    {
        String objectUri;

        if (useVersion) {
            objectUri =
                UriBuilder.fromUri(baseUri).path(ObjectAtPageVersionResource.class).build(doc.getWiki(),
                    doc.getSpace(), doc.getName(), doc.getVersion(), xwikiObject.getClassName(),
                    xwikiObject.getNumber()).toString();
        } else {
            objectUri =
                UriBuilder.fromUri(baseUri).path(ObjectResource.class).build(doc.getWiki(), doc.getSpace(),
                    doc.getName(), xwikiObject.getClassName(), xwikiObject.getNumber()).toString();
        }
        Link objectLink = objectFactory.createLink();
        objectLink.setHref(objectUri);
        objectLink.setRel(relation);

        return objectLink;
    }

    public static Class createClass(ObjectFactory objectFactory, URI baseUri, String wikiName,
        com.xpn.xwiki.api.Class xwikiClass)
    {
        Class clazz = objectFactory.createClass();
        clazz.setId(xwikiClass.getName());
        clazz.setName(xwikiClass.getName());

        for (java.lang.Object xwikiPropertyClassObject : xwikiClass.getProperties()) {
            PropertyClass xwikiPropertyClass = (PropertyClass) xwikiPropertyClassObject;

            Property property = objectFactory.createProperty();
            property.setName(xwikiPropertyClass.getName());
            property.setType(xwikiPropertyClass.getxWikiClass().getName());

            for (java.lang.Object xwikiPropertyObject : xwikiPropertyClass.getProperties()) {
                com.xpn.xwiki.api.Property xwikiProperty = (com.xpn.xwiki.api.Property) xwikiPropertyObject;
                java.lang.Object value = xwikiProperty.getValue();

                Attribute attribute = objectFactory.createAttribute();
                attribute.setName(xwikiProperty.getName());

                if (value != null) {
                    attribute.setValue(value.toString());
                } else {
                    attribute.setValue("");
                }

                property.getAttributes().add(attribute);
            }

            String propertyUri =
                UriBuilder.fromUri(baseUri).path(ClassPropertyResource.class).build(wikiName, xwikiClass.getName(),
                    xwikiPropertyClass.getName()).toString();
            Link propertyLink = objectFactory.createLink();
            propertyLink.setHref(propertyUri);
            propertyLink.setRel(Relations.SELF);
            property.getLinks().add(propertyLink);

            clazz.getProperties().add(property);
        }

        String classUri =
            UriBuilder.fromUri(baseUri).path(ClassResource.class).build(wikiName, xwikiClass.getName()).toString();
        Link classLink = objectFactory.createLink();
        classLink.setHref(classUri);
        classLink.setRel(Relations.SELF);
        clazz.getLinks().add(classLink);

        String propertiesUri =
            UriBuilder.fromUri(baseUri).path(ClassPropertiesResource.class).build(wikiName, xwikiClass.getName())
                .toString();
        Link propertyLink = objectFactory.createLink();
        propertyLink.setHref(propertiesUri);
        propertyLink.setRel(Relations.PROPERTIES);
        clazz.getLinks().add(propertyLink);

        String objectsUri =
            UriBuilder.fromUri(baseUri).path(AllObjectsForClassNameResource.class)
                .build(wikiName, xwikiClass.getName()).toString();
        Link objectsLink = objectFactory.createLink();
        objectsLink.setHref(objectsUri);
        objectsLink.setRel(Relations.OBJECTS);
        clazz.getLinks().add(objectsLink);

        return clazz;
    }
}
