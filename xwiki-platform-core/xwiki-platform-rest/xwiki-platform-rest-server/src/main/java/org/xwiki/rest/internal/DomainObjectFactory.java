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
package org.xwiki.rest.internal;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.rest.Relations;
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
import org.xwiki.rest.resources.wikis.WikiSearchQueryResource;
import org.xwiki.rest.resources.wikis.WikiSearchResource;
import org.xwiki.rest.resources.wikis.WikisResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.PropertyClass;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * This class contains utility methods for building representations using model objects.
 * 
 * @version $Id$
 */
public class DomainObjectFactory
{
    public static Xwiki createXWikiRoot(ObjectFactory objectFactory, URI baseUri, String version)
    {
        Xwiki xwiki = objectFactory.createXwiki().withVersion(version);

        String wikisUri = uri(baseUri, WikisResource.class);
        Link wikisLink = objectFactory.createLink();
        wikisLink.setHref(wikisUri);
        wikisLink.setRel(Relations.WIKIS);
        xwiki.getLinks().add(wikisLink);

        String syntaxesUri = uri(baseUri, SyntaxesResource.class);
        Link syntaxesLink = objectFactory.createLink();
        syntaxesLink.setHref(syntaxesUri);
        syntaxesLink.setRel(Relations.SYNTAXES);
        xwiki.getLinks().add(syntaxesLink);

        return xwiki;
    }

    public static Wiki createWiki(ObjectFactory objectFactory, URI baseUri, String wikiName)
    {
        Wiki wiki = objectFactory.createWiki().withId(wikiName).withName(wikiName);

        String spacesUri = uri(baseUri, SpacesResource.class, wikiName);
        Link spacesLink = objectFactory.createLink();
        spacesLink.setHref(spacesUri);
        spacesLink.setRel(Relations.SPACES);
        wiki.getLinks().add(spacesLink);

        String classesUri = uri(baseUri, ClassesResource.class, wikiName);
        Link classesLink = objectFactory.createLink();
        classesLink.setHref(classesUri);
        classesLink.setRel(Relations.CLASSES);
        wiki.getLinks().add(classesLink);

        String modificationsUri = uri(baseUri, ModificationsResource.class, wikiName);
        Link modificationsLink = objectFactory.createLink();
        modificationsLink.setHref(modificationsUri);
        modificationsLink.setRel(Relations.MODIFICATIONS);
        wiki.getLinks().add(modificationsLink);

        String searchUri = uri(baseUri, WikiSearchResource.class, wikiName);
        Link searchLink = objectFactory.createLink();
        searchLink.setHref(searchUri);
        searchLink.setRel(Relations.SEARCH);
        wiki.getLinks().add(searchLink);

        String queryUri = uri(baseUri, WikiSearchQueryResource.class, wikiName);
        Link queryLink = objectFactory.createLink();
        queryLink.setHref(queryUri);
        queryLink.setRel(Relations.QUERY);
        wiki.getLinks().add(queryLink);

        return wiki;
    }

    public static Space createSpace(ObjectFactory objectFactory, URI baseUri, String wikiName, String spaceName,
        Document home)
    {
        Space space = objectFactory.createSpace();
        space.setId(Utils.getSpaceId(wikiName, spaceName));
        space.setWiki(wikiName);
        space.setName(spaceName);
        if (home != null) {
            space.setHome(home.getPrefixedFullName());
            space.setXwikiRelativeUrl(home.getURL("view"));
            space.setXwikiAbsoluteUrl(home.getExternalURL("view"));
        }

        String pagesUri = uri(baseUri, PagesResource.class, wikiName, spaceName);
        Link pagesLink = objectFactory.createLink();
        pagesLink.setHref(pagesUri);
        pagesLink.setRel(Relations.PAGES);
        space.getLinks().add(pagesLink);

        if (home != null) {
            String homeUri = uri(baseUri, PageResource.class, wikiName, spaceName, home.getName());
            Link homeLink = objectFactory.createLink();
            homeLink.setHref(homeUri);
            homeLink.setRel(Relations.HOME);
            space.getLinks().add(homeLink);
        }

        String searchUri = uri(baseUri, SpaceSearchResource.class, wikiName, spaceName);
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
                    uri(baseUri, PageResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
                Link pageTranslationLink = objectFactory.createLink();
                pageTranslationLink.setHref(pageTranslationUri);
                pageTranslationLink.setRel(Relations.PAGE);
                translation.getLinks().add(pageTranslationLink);

                String historyUri =
                    uri(baseUri, PageHistoryResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
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
                uri(baseUri, PageTranslationResource.class, doc.getWiki(), doc.getSpace(), doc.getName(), language);
            Link pageTranslationLink = objectFactory.createLink();
            pageTranslationLink.setHref(pageTranslationUri);
            pageTranslationLink.setRel(Relations.PAGE);
            translation.getLinks().add(pageTranslationLink);

            String historyUri =
                uri(baseUri, PageTranslationHistoryResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                    language);
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
        Document doc, boolean useVersion, XWiki xwikiApi, Boolean withPrettyNames) throws XWikiException
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
        pageSummary.setSyntax(doc.getSyntax().toIdString());
        pageSummary.setVersion(doc.getVersion());
        pageSummary.setAuthor(doc.getAuthor());
        if (withPrettyNames) {
            pageSummary.setAuthorName(xwikiApi.getUserName(doc.getAuthor(), false));
        }

        Document parent = Utils.getParentDocument(doc, xwikiApi);
        pageSummary.setParent(doc.getParent());
        // parentId must not be set if the parent document does not exist.
        if (parent != null && !parent.isNew()) {
            pageSummary.setParentId(parent.getPrefixedFullName());
        } else {
            pageSummary.setParentId("");
        }

        String spaceUri = uri(baseUri, SpaceResource.class, doc.getWiki(), doc.getSpace());
        Link spaceLink = objectFactory.createLink();
        spaceLink.setHref(spaceUri);
        spaceLink.setRel(Relations.SPACE);
        pageSummary.getLinks().add(spaceLink);

        if (parent != null) {
            String parentUri = uri(baseUri, PageResource.class, parent.getWiki(), parent.getSpace(), parent.getName());
            Link parentLink = objectFactory.createLink();
            parentLink.setHref(parentUri);
            parentLink.setRel(Relations.PARENT);
            pageSummary.getLinks().add(parentLink);
        }

        String historyUri = uri(baseUri, PageHistoryResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
        Link historyLink = objectFactory.createLink();
        historyLink.setHref(historyUri);
        historyLink.setRel(Relations.HISTORY);
        pageSummary.getLinks().add(historyLink);

        if (!doc.getChildren().isEmpty()) {
            String pageChildrenUri =
                uri(baseUri, PageChildrenResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
            Link pageChildrenLink = objectFactory.createLink();
            pageChildrenLink.setHref(pageChildrenUri);
            pageChildrenLink.setRel(Relations.CHILDREN);
            pageSummary.getLinks().add(pageChildrenLink);
        }

        if (!doc.getComments().isEmpty()) {
            String commentsUri;
            if (useVersion) {
                commentsUri =
                    uri(baseUri, CommentsVersionResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                        doc.getVersion());
            } else {
                commentsUri = uri(baseUri, CommentsResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
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
                    uri(baseUri, AttachmentsAtPageVersionResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                        doc.getVersion());
            } else {
                attachmentsUri = uri(baseUri, AttachmentsResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
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
                    uri(baseUri, ObjectsAtPageVersionResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                        doc.getVersion());
            } else {
                objectsUri = uri(baseUri, ObjectsResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
            }
            Link objectsLink = objectFactory.createLink();
            objectsLink.setHref(objectsUri);
            objectsLink.setRel(Relations.OBJECTS);
            pageSummary.getLinks().add(objectsLink);
        }

        com.xpn.xwiki.api.Object tagsObject = doc.getObject("XWiki.TagClass", 0);
        if (tagsObject != null) {
            if (tagsObject.getProperty("tags") != null) {
                String tagsUri = uri(baseUri, PageTagsResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
                Link tagsLink = objectFactory.createLink();
                tagsLink.setHref(tagsUri);
                tagsLink.setRel(Relations.TAGS);
                pageSummary.getLinks().add(tagsLink);
            }
        }

        String syntaxesUri = uri(baseUri, SyntaxesResource.class);
        Link syntaxesLink = objectFactory.createLink();
        syntaxesLink.setHref(syntaxesUri);
        syntaxesLink.setRel(Relations.SYNTAXES);
        pageSummary.getLinks().add(syntaxesLink);
    }

    public static PageSummary createPageSummary(ObjectFactory objectFactory, URI baseUri, Document doc, XWiki xwikiApi,
        Boolean withPrettyNames) throws XWikiException
    {
        PageSummary pageSummary = objectFactory.createPageSummary();
        fillPageSummary(pageSummary, objectFactory, baseUri, doc, false, xwikiApi, withPrettyNames);

        String pageUri = uri(baseUri, PageResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        pageSummary.getLinks().add(pageLink);

        return pageSummary;
    }

    public static Page createPage(ObjectFactory objectFactory, URI baseUri, URI self, Document doc, boolean useVersion,
        XWiki xwikiApi, Boolean withPrettyNames) throws XWikiException
    {
        Page page = objectFactory.createPage();
        fillPageSummary(page, objectFactory, baseUri, doc, useVersion, xwikiApi, withPrettyNames);

        page.setMajorVersion(doc.getRCSVersion().at(0));
        page.setMinorVersion(doc.getRCSVersion().at(1));
        page.setLanguage(doc.getLocale().toString());
        page.setCreator(doc.getCreator());
        if (withPrettyNames) {
            page.setCreatorName(xwikiApi.getUserName(doc.getCreator(), false));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(doc.getCreationDate());
        page.setCreated(calendar);

        page.setModifier(doc.getContentAuthor());
        if (withPrettyNames) {
            page.setModifierName(xwikiApi.getUserName(doc.getContentAuthor(), false));
        }

        calendar = Calendar.getInstance();
        calendar.setTime(doc.getContentUpdateDate());
        page.setModified(calendar);

        page.setComment(doc.getComment());
        page.setContent(doc.getContent());

        if (self != null) {
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(self.toString());
            pageLink.setRel(Relations.SELF);
            page.getLinks().add(pageLink);
        }

        com.xpn.xwiki.api.Class xwikiClass = doc.getxWikiClass();
        if (xwikiClass != null) {
            String classUri = uri(baseUri, ClassResource.class, doc.getWiki(), xwikiClass.getName());
            Link classLink = objectFactory.createLink();
            classLink.setHref(classUri);
            classLink.setRel(Relations.CLASS);
            page.getLinks().add(classLink);
        }

        return page;
    }

    public static HistorySummary createHistorySummary(ObjectFactory objectFactory, URI baseUri, String wikiName,
        String spaceName, String pageName, String language, Version version, String modifier, Date modified,
        String comment, XWiki xwikiApi, Boolean withPrettyNames)
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
        historySummary.setComment(comment);
        historySummary.setModifier(modifier);
        if (withPrettyNames) {
            historySummary.setModifierName(xwikiApi.getUserName(modifier, false));
        }

        historySummary.setLanguage(language);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(modified);
        historySummary.setModified(calendar);

        if (language == null) {
            String pageUri = uri(baseUri, PageVersionResource.class, wikiName, spaceName, pageName, version);
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(pageUri);
            pageLink.setRel(Relations.PAGE);
            historySummary.getLinks().add(pageLink);
        } else {
            String pageUri =
                uri(baseUri, PageTranslationVersionResource.class, wikiName, spaceName, pageName, language, version);
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(pageUri);
            pageLink.setRel(Relations.PAGE);
            historySummary.getLinks().add(pageLink);
        }

        return historySummary;
    }

    private static void fillAttachment(Attachment attachment, ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl, XWiki xwikiApi,
        Boolean withPrettyNames)
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
        if (withPrettyNames) {
            attachment.setAuthorName(xwikiApi.getUserName(xwikiAttachment.getAuthor(), false));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(xwikiAttachment.getDate());
        attachment.setDate(calendar);

        attachment.setXwikiRelativeUrl(xwikiRelativeUrl);
        attachment.setXwikiAbsoluteUrl(xwikiAbsoluteUrl);

        String pageUri = uri(baseUri, PageResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        attachment.getLinks().add(pageLink);
    }

    public static Attachment createAttachment(ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl, XWiki xwikiApi,
        Boolean withPrettyNames)
    {
        Attachment attachment = objectFactory.createAttachment();

        fillAttachment(attachment, objectFactory, baseUri, xwikiAttachment, xwikiRelativeUrl, xwikiAbsoluteUrl,
            xwikiApi, withPrettyNames);

        Document doc = xwikiAttachment.getDocument();
        String attachmentUri =
            uri(baseUri, AttachmentResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                xwikiAttachment.getFilename());

        Link attachmentLink = objectFactory.createLink();
        attachmentLink.setHref(attachmentUri);
        attachmentLink.setRel(Relations.ATTACHMENT_DATA);
        attachment.getLinks().add(attachmentLink);

        return attachment;
    }

    public static Attachment createAttachmentAtVersion(ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl, XWiki xwikiApi,
        Boolean withPrettyNames)
    {
        Attachment attachment = new Attachment();

        fillAttachment(attachment, objectFactory, baseUri, xwikiAttachment, xwikiRelativeUrl, xwikiAbsoluteUrl,
            xwikiApi, withPrettyNames);

        Document doc = xwikiAttachment.getDocument();
        String attachmentUri =
            uri(baseUri, AttachmentVersionResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                xwikiAttachment.getFilename(), xwikiAttachment.getVersion());

        Link attachmentLink = objectFactory.createLink();
        attachmentLink.setHref(attachmentUri);
        attachmentLink.setRel(Relations.ATTACHMENT_DATA);
        attachment.getLinks().add(attachmentLink);

        return attachment;
    }

    public static Comment createComment(ObjectFactory objectFactory, URI baseUri, Document doc,
        com.xpn.xwiki.api.Object xwikiComment, XWiki xwikiApi, Boolean withPrettyNames)
    {
        Comment comment = objectFactory.createComment();
        comment.setId(xwikiComment.getNumber());

        com.xpn.xwiki.api.Property property = xwikiComment.getProperty("author");
        if (property != null) {
            comment.setAuthor((String) property.getValue());
            if (withPrettyNames) {
                comment.setAuthorName(xwikiApi.getUserName((String) property.getValue(), false));
            }
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

        String pageUri = uri(baseUri, PageResource.class, doc.getWiki(), doc.getSpace(), doc.getName());
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        comment.getLinks().add(pageLink);

        return comment;
    }

    private static void fillObjectSummary(ObjectSummary objectSummary, ObjectFactory objectFactory, URI baseUri,
        Document doc, BaseObject xwikiObject, XWiki xwikiApi, Boolean withPrettyNames) throws XWikiException
    {

        objectSummary.setId(String.format("%s:%s", doc.getPrefixedFullName(), xwikiObject.getGuid()));
        objectSummary.setGuid(xwikiObject.getGuid());
        objectSummary.setPageId(doc.getPrefixedFullName());
        objectSummary.setPageVersion(doc.getVersion());
        objectSummary.setPageAuthor(doc.getAuthor());
        if (withPrettyNames) {
            objectSummary.setPageAuthorName(xwikiApi.getUserName(doc.getAuthor(), false));
        }
        objectSummary.setWiki(doc.getWiki());
        objectSummary.setSpace(doc.getSpace());
        objectSummary.setPageName(doc.getName());
        objectSummary.setClassName(xwikiObject.getClassName());
        objectSummary.setNumber(xwikiObject.getNumber());

        String[] propertyNames = xwikiObject.getPropertyNames();
        if (propertyNames.length > 0) {
            objectSummary.setHeadline(serializePropertyValue(xwikiObject.get(propertyNames[0])));
        }
    }

    public static ObjectSummary createObjectSummary(ObjectFactory objectFactory, URI baseUri,
        XWikiContext xwikiContext, Document doc, BaseObject xwikiObject, boolean useVersion, XWiki xwikiApi,
        Boolean withPrettyNames) throws XWikiException
    {
        ObjectSummary objectSummary = objectFactory.createObjectSummary();
        fillObjectSummary(objectSummary, objectFactory, baseUri, doc, xwikiObject, xwikiApi, withPrettyNames);

        Link objectLink = getObjectLink(objectFactory, baseUri, doc, xwikiObject, useVersion, Relations.OBJECT);
        objectSummary.getLinks().add(objectLink);

        String propertiesUri;

        if (useVersion) {
            propertiesUri =
                uri(baseUri, ObjectPropertiesAtPageVersionResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                    doc.getVersion(), xwikiObject.getClassName(), xwikiObject.getNumber());
        } else {
            propertiesUri =
                uri(baseUri, ObjectPropertiesResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                    xwikiObject.getClassName(), xwikiObject.getNumber());
        }

        Link propertyLink = objectFactory.createLink();
        propertyLink.setHref(propertiesUri);
        propertyLink.setRel(Relations.PROPERTIES);
        objectSummary.getLinks().add(propertyLink);

        return objectSummary;
    }

    public static Object createObject(ObjectFactory objectFactory, URI baseUri, XWikiContext xwikiContext,
        Document doc, BaseObject xwikiObject, boolean useVersion, XWiki xwikiApi, Boolean withPrettyNames)
        throws XWikiException
    {
        Object object = objectFactory.createObject();
        fillObjectSummary(object, objectFactory, baseUri, doc, xwikiObject, xwikiApi, withPrettyNames);

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
            property.setValue(serializePropertyValue(xwikiObject.get(propertyClass.getName())));

            String propertyUri;

            if (useVersion) {
                propertyUri =
                    uri(baseUri, ObjectPropertyAtPageVersionResource.class, doc.getWiki(), doc.getSpace(),
                        doc.getName(), doc.getVersion(), xwikiObject.getClassName(), xwikiObject.getNumber(),
                        propertyClass.getName());
            } else {
                propertyUri =
                    uri(baseUri, ObjectPropertyResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                        xwikiObject.getClassName(), xwikiObject.getNumber(), propertyClass.getName());
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
                uri(baseUri, ObjectAtPageVersionResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                    doc.getVersion(), xwikiObject.getClassName(), xwikiObject.getNumber());
        } else {
            objectUri =
                uri(baseUri, ObjectResource.class, doc.getWiki(), doc.getSpace(), doc.getName(),
                    xwikiObject.getClassName(), xwikiObject.getNumber());
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
                uri(baseUri, ClassPropertyResource.class, wikiName, xwikiClass.getName(), xwikiPropertyClass.getName());
            Link propertyLink = objectFactory.createLink();
            propertyLink.setHref(propertyUri);
            propertyLink.setRel(Relations.SELF);
            property.getLinks().add(propertyLink);

            clazz.getProperties().add(property);
        }

        String classUri = uri(baseUri, ClassResource.class, wikiName, xwikiClass.getName());
        Link classLink = objectFactory.createLink();
        classLink.setHref(classUri);
        classLink.setRel(Relations.SELF);
        clazz.getLinks().add(classLink);

        String propertiesUri = uri(baseUri, ClassPropertiesResource.class, wikiName, xwikiClass.getName());
        Link propertyLink = objectFactory.createLink();
        propertyLink.setHref(propertiesUri);
        propertyLink.setRel(Relations.PROPERTIES);
        clazz.getLinks().add(propertyLink);

        String objectsUri = uri(baseUri, AllObjectsForClassNameResource.class, wikiName, xwikiClass.getName());
        Link objectsLink = objectFactory.createLink();
        objectsLink.setHref(objectsUri);
        objectsLink.setRel(Relations.OBJECTS);
        clazz.getLinks().add(objectsLink);

        return clazz;
    }

    /**
     * Creates an URI to access the specified resource. The given path elements are encoded before being inserted into
     * the resource path.
     * 
     * @param baseURI the base URI
     * @param resourceClass the resource class, used to get the URI path
     * @param pathElements the path elements to insert in the resource path
     * @return an URI that can be used to access the specified resource
     */
    private static String uri(URI baseURI, java.lang.Class< ? > resourceClass, java.lang.Object... pathElements)
    {
        return Utils.createURI(baseURI, resourceClass, pathElements).toString();
    }

    /**
     * Serializes the value of the given XObject property.
     * 
     * @param property an XObject property
     * @return the String representation of the property value
     */
    private static String serializePropertyValue(PropertyInterface property)
    {
        if (property == null) {
            return "";
        }

        java.lang.Object value = ((BaseProperty) property).getValue();
        if (value instanceof List) {
            return StringUtils.join((List) value, "|");
        } else if (value != null) {
            return value.toString();
        } else {
            return "";
        }
    }
}
