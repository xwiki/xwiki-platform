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
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.JobLog;
import org.xwiki.rest.model.jaxb.JobProgress;
import org.xwiki.rest.model.jaxb.JobStatus;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.model.jaxb.Translations;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Xwiki;
import org.xwiki.rest.resources.SyntaxesResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationVersionResource;
import org.xwiki.rest.resources.pages.PageVersionResource;
import org.xwiki.rest.resources.wikis.WikisResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This class contains utility methods for building representations using model objects.
 * 
 * @version $Id$
 */
public class DomainObjectFactory
{
    public static ModelFactory getModelFactory()
    {
        return com.xpn.xwiki.web.Utils.getComponent(ModelFactory.class);
    }

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

    /**
     * @deprecated since 7.3M1, use {@link ModelFactory#toRestWiki(URI, String)} instead
     */
    @Deprecated
    public static Wiki createWiki(ObjectFactory objectFactory, URI baseUri, String wikiName)
    {
        return getModelFactory().toRestWiki(baseUri, wikiName);
    }

    /**
     * @deprecated since 7.3M1, use {@link ModelFactory#toRestSpace(URI, String, List, Document)} instead
     */
    @Deprecated
    public static Space createSpace(ObjectFactory objectFactory, URI baseUri, String wikiName, List<String> spaces,
        Document home)
    {
        return getModelFactory().toRestSpace(baseUri, wikiName, spaces, home);
    }

    /**
     * @deprecated since 7.3M1, use {@link ModelFactory#toRestTranslations(URI, Document)}
     */
    @Deprecated
    public static Translations createTranslations(ObjectFactory objectFactory, URI baseUri, Document doc)
        throws XWikiException
    {
        return getModelFactory().toRestTranslations(baseUri, doc);
    }

    /**
     * @deprecated since 7.3M1, use {@link ModelFactory#toRestPageSummary(URI, Document, XWiki, Boolean)}
     */
    @Deprecated
    public static PageSummary createPageSummary(ObjectFactory objectFactory, URI baseUri, Document doc, XWiki xwikiApi,
        Boolean withPrettyNames) throws XWikiException
    {
        return getModelFactory().toRestPageSummary(baseUri, doc, withPrettyNames);
    }

    /**
     * @deprecated since 7.3M1, use {@link ModelFactory#toRestPage(URI, URI, Document, boolean, XWiki, Boolean)}
     */
    @Deprecated
    public static Page createPage(ObjectFactory objectFactory, URI baseUri, URI self, Document doc, boolean useVersion,
        XWiki xwikiApi, Boolean withPrettyNames) throws XWikiException
    {
        return getModelFactory().toRestPage(baseUri, self, doc, useVersion, withPrettyNames, false, false, false);
    }

    public static HistorySummary createHistorySummary(ObjectFactory objectFactory, URI baseUri, String wikiName,
        List<String> spaces, String pageName, String language, Version version, String modifier, Date modified,
        String comment, XWiki xwikiApi, Boolean withPrettyNames)
    {
        HistorySummary historySummary = objectFactory.createHistorySummary();

        String pageId = Utils.getPageId(wikiName, spaces, pageName);

        historySummary.setPageId(pageId);
        historySummary.setWiki(wikiName);
        historySummary.setSpace(Utils.getLocalSpaceId(spaces));
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
            String pageUri = uri(baseUri, PageVersionResource.class, wikiName, spaces, pageName, version);
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(pageUri);
            pageLink.setRel(Relations.PAGE);
            historySummary.getLinks().add(pageLink);
        } else {
            String pageUri =
                uri(baseUri, PageTranslationVersionResource.class, wikiName, spaces, pageName, language, version);
            Link pageLink = objectFactory.createLink();
            pageLink.setHref(pageUri);
            pageLink.setRel(Relations.PAGE);
            historySummary.getLinks().add(pageLink);
        }

        return historySummary;
    }

    /**
     * @deprecated since 7.3M1, use
     *             {@link ModelFactory#toRestAttachment(URI, com.xpn.xwiki.api.Attachment, String, String, XWiki, Boolean, boolean)}
     *             instead
     */
    @Deprecated
    public static Attachment createAttachment(ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl, XWiki xwikiApi,
        Boolean withPrettyNames)
    {
        return getModelFactory().toRestAttachment(baseUri, xwikiAttachment, xwikiRelativeUrl, xwikiAbsoluteUrl,
            withPrettyNames, false);
    }

    /**
     * @deprecated since 7.3M1, use
     *             {@link ModelFactory#toRestAttachment(URI, com.xpn.xwiki.api.Attachment, String, String, XWiki, Boolean, boolean)}
     *             instead
     */
    @Deprecated
    public static Attachment createAttachmentAtVersion(ObjectFactory objectFactory, URI baseUri,
        com.xpn.xwiki.api.Attachment xwikiAttachment, String xwikiRelativeUrl, String xwikiAbsoluteUrl, XWiki xwikiApi,
        Boolean withPrettyNames)
    {
        return getModelFactory().toRestAttachment(baseUri, xwikiAttachment, xwikiRelativeUrl, xwikiAbsoluteUrl,
            withPrettyNames, true);
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

        String pageUri = uri(baseUri, PageResource.class, doc.getWiki(), Utils.getSpacesFromSpaceId(doc.getSpace()),
            doc.getDocumentReference().getName());
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        comment.getLinks().add(pageLink);

        return comment;
    }

    /**
     * @deprecated since 7.3M1, use
     *             {@link ModelFactory#toRestObjectSummary(ObjectFactory, URI, XWikiContext, Document, BaseObject, boolean, XWiki, Boolean)}
     *             instead
     */
    @Deprecated
    public static ObjectSummary createObjectSummary(ObjectFactory objectFactory, URI baseUri, XWikiContext xwikiContext,
        Document doc, BaseObject xwikiObject, boolean useVersion, XWiki xwikiApi, Boolean withPrettyNames)
        throws XWikiException
    {
        return getModelFactory().toRestObjectSummary(baseUri, doc, xwikiObject, useVersion, withPrettyNames);
    }

    /**
     * @deprecated since 7.3M1, use
     *             {@link ModelFactory#toRestObject(URI, XWikiContext, Document, BaseObject, boolean, XWiki, Boolean, Boolean)}
     *             instead
     */
    @Deprecated
    public static Object createObject(ObjectFactory objectFactory, URI baseUri, XWikiContext xwikiContext, Document doc,
        BaseObject xwikiObject, boolean useVersion, XWiki xwikiApi, Boolean withPrettyNames) throws XWikiException
    {
        return getModelFactory().toRestObject(baseUri, doc, xwikiObject, useVersion, withPrettyNames);
    }

    /**
     * @deprecated since 9.1RC1, use
     *             {@link ModelFactory#toRestJobStatus(org.xwiki.job.event.status.JobStatus, boolean, boolean, boolean)}
     *             instead
     */
    @Deprecated
    public static JobStatus createJobStatus(ObjectFactory objectFactory, URI self,
        org.xwiki.job.event.status.JobStatus jobStatus) throws XWikiRestException
    {
        return getModelFactory().toRestJobStatus(jobStatus, self, false, true, false, null);
    }

    /**
     * @deprecated since 9.1RC1, use {@link ModelFactory#toRestJobProgress(org.xwiki.job.event.status.JobProgress)}
     *             instead
     */
    @Deprecated
    public static JobProgress createJobProgress(ObjectFactory objectFactory,
        org.xwiki.job.event.status.JobProgress jobProgress)
    {
        return getModelFactory().toRestJobProgress(jobProgress);
    }

    /**
     * @deprecated since 9.1RC1, use
     *             {@link ModelFactory#toRestJobStatus(org.xwiki.job.event.status.JobStatus, boolean, boolean, boolean)}
     *             instead
     */
    @Deprecated
    public static JobLog createLog(ObjectFactory objectFactory, URI self, Collection<LogEvent> logs)
    {
        return getModelFactory().toRestJobLog(logs, self);
    }

    /**
     * @deprecated since 7.3M1, use {@link ModelFactory#toRestClass(URI, String, com.xpn.xwiki.api.Class)}
     */
    public static org.xwiki.rest.model.jaxb.Class createClass(ObjectFactory objectFactory, URI baseUri, String wikiName,
        com.xpn.xwiki.api.Class xwikiClass)
    {
        return getModelFactory().toRestClass(baseUri, xwikiClass);
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
    private static String uri(URI baseURI, java.lang.Class<?> resourceClass, java.lang.Object... pathElements)
    {
        return Utils.createURI(baseURI, resourceClass, pathElements).toString();
    }
}
