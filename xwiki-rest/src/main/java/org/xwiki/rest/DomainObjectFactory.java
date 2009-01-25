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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.xwiki.rest.model.Comment;
import org.xwiki.rest.model.HistorySummary;
import org.xwiki.rest.model.Link;
import org.xwiki.rest.model.Page;
import org.xwiki.rest.model.PageSummary;
import org.xwiki.rest.model.Relations;
import org.xwiki.rest.model.Space;
import org.xwiki.rest.model.Translations;
import org.xwiki.rest.model.Wikis;
import org.xwiki.rest.model.XWikiRoot;
import org.xwiki.rest.resources.RootResource;
import org.xwiki.rest.resources.comments.CommentResource;
import org.xwiki.rest.resources.comments.CommentsResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.pages.PageTranslationVersionResource;
import org.xwiki.rest.resources.pages.PageVersionResource;
import org.xwiki.rest.resources.pages.PagesResource;
import org.xwiki.rest.resources.spaces.SpaceResource;
import org.xwiki.rest.resources.spaces.SpacesResource;
import org.xwiki.rest.resources.wikis.WikisResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;

/**
 * @version $Id$
 */
public class DomainObjectFactory
{
    public static XWikiRoot createXWikiRoot(Request request, com.xpn.xwiki.api.XWiki xwikiApi,
        XWikiResourceClassRegistry registry)
    {
        XWikiRoot xwikiRoot = new XWikiRoot(xwikiApi.getVersion());

        String fullUri =
            String.format("%s%s", request.getRootRef(), registry.getUriPatternForResourceClass(WikisResource.class));
        Link link = new Link(fullUri);
        link.setRel(Relations.WIKIS);
        xwikiRoot.addLink(link);

        fullUri =
            String.format("%s%s", request.getRootRef(), registry.getUriPatternForResourceClass(RootResource.class));
        link = new Link(fullUri);
        link.setRel(Relations.WADL);
        link.setType(MediaType.APPLICATION_WADL_XML.toString());
        xwikiRoot.addLink(link);

        return xwikiRoot;
    }

    public static Wikis createWikis(Request request, com.xpn.xwiki.XWiki xwiki,
        com.xpn.xwiki.XWikiContext xwikiContext, XWikiResourceClassRegistry registry)
    {
        Wikis wikis = new Wikis();

        List<String> databaseNames = new ArrayList<String>();

        try {
            databaseNames = xwiki.getVirtualWikisDatabaseNames(xwikiContext);
        } catch (XWikiException e) {
            /* Ignore */
        }

        for (String databaseName : databaseNames) {
            String fullUri =
                String.format("%s%s", request.getRootRef(), registry
                    .getUriPatternForResourceClass(SpacesResource.class));
            Link link = new Link(fullUri);
            link.setRel(Relations.SPACES);
            wikis.addLink(link);
        }

        return wikis;

    }

    public static Space createSpace(Request request, XWikiResourceClassRegistry resourceClassRegistry, String wiki,
        String spaceName, String home, int numberOfPages)
    {
        Space space = new Space(wiki, spaceName, home, numberOfPages);

        String fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(PagesResource.class));
        Map<String, String> parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, wiki);
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, spaceName);
        Link link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGES);
        space.addLink(link);

        return space;
    }

    public static PageSummary createPageSummary(Request request, XWikiResourceClassRegistry resourceClassRegistry,
        Document doc)
    {
        try {
            PageSummary pageSummary = new PageSummary();

            pageSummary.setWiki(doc.getWiki());
            pageSummary.setId(doc.getFullName());
            pageSummary.setFullId(doc.getPrefixedFullName());
            pageSummary.setSpace(doc.getSpace());
            pageSummary.setName(doc.getName());
            pageSummary.setTitle(doc.getTitle());

            Translations translations = pageSummary.getTranslations();

            List<String> languages = doc.getTranslationList();

            if (!languages.isEmpty()) {
                if (!doc.getDefaultLanguage().equals("")) {
                    translations.setDefaultTranslation(doc.getDefaultLanguage());
                }
            }

            String fullUri;
            Map<String, String> parametersMap;
            Link link;

            for (String language : languages) {
                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(PageTranslationResource.class));
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
                parametersMap.put(Constants.LANGUAGE_ID_PARAMETER, language);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.TRANSLATION);
                link.setHrefLang(language);
                translations.addLink(link);
            }

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.PAGE);
            pageSummary.addLink(link);

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(SpaceResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.SPACE);
            pageSummary.addLink(link);

            String parent = doc.getParent();
            if (parent != null && parent.indexOf('.') != -1) {
                pageSummary.setParent(doc.getParent());

                String[] components = doc.getParent().split("\\.");
                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(PageResource.class));
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, components[0]);
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, components[1]);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.PARENT);
                pageSummary.addLink(link);
            }

            return pageSummary;
        } catch (Exception e) {
            return null;
        }
    }

    public static Page createPage(Request request, XWikiResourceClassRegistry resourceClassRegistry, Document doc)
    {
        try {
            Page page = new Page();

            page.setWiki(doc.getWiki());
            page.setId(doc.getFullName());
            page.setFullId(doc.getPrefixedFullName());
            page.setSpace(doc.getSpace());
            page.setName(doc.getName());
            page.setTitle(doc.getTitle());
            page.setVersion(doc.getRCSVersion().at(0));
            page.setMinorVersion(doc.getRCSVersion().at(1));
            page.setLanguage(doc.getLanguage());
            page.setXWikiUrl(doc.getExternalURL("view"));
            page.setCreator(doc.getCreator());
            page.setCreated(doc.getCreationDate().getTime());
            page.setModifier(doc.getContentAuthor());
            page.setModified(doc.getContentUpdateDate().getTime());
            page.setContent(doc.getContent());

            Translations translations = page.getTranslations();

            List<String> languages = doc.getTranslationList();

            if (!languages.isEmpty()) {
                if (!doc.getDefaultLanguage().equals("")) {
                    translations.setDefaultTranslation(doc.getDefaultLanguage());
                }
            }

            String fullUri;
            Map<String, String> parametersMap;
            Link link;

            for (String language : languages) {
                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(PageTranslationResource.class));
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
                parametersMap.put(Constants.LANGUAGE_ID_PARAMETER, language);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.TRANSLATION);
                link.setHrefLang(language);
                translations.addLink(link);
            }

            link = new Link(request.getResourceRef().getIdentifier());
            link.setRel(Relations.SELF);
            page.addLink(link);

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(SpaceResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.SPACE);
            page.addLink(link);

            String parent = doc.getParent();
            if (parent != null && parent.indexOf('.') != -1) {
                page.setParent(doc.getParent());

                String[] components = doc.getParent().split("\\.");

                fullUri =
                    String.format("%s%s", request.getRootRef(), resourceClassRegistry
                        .getUriPatternForResourceClass(PageResource.class));
                parametersMap = new HashMap<String, String>();
                parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
                parametersMap.put(Constants.SPACE_NAME_PARAMETER, components[0]);
                parametersMap.put(Constants.PAGE_NAME_PARAMETER, components[1]);
                link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
                link.setRel(Relations.PARENT);
                page.addLink(link);
            }

            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(CommentsResource.class));

            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.COMMENTS);
            page.addLink(link);

            return page;
        } catch (Exception e) {
            return null;
        }
    }

    public static HistorySummary createHistorySummary(Request request,
        XWikiResourceClassRegistry resourceClassRegistry, String wikiName, String languageId, Object[] fields)
    {
        String pageId = (String) fields[0];

        String[] components = pageId.split("\\.");
        String spaceName = components[0];
        String pageName = components[1];

        XWikiRCSNodeId nodeId = (XWikiRCSNodeId) fields[1];
        int version = nodeId.getVersion().at(0);
        int minorVersion = nodeId.getVersion().at(1);

        Timestamp timestamp = (Timestamp) fields[2];
        String author = (String) fields[3];

        HistorySummary historySummary = new HistorySummary();
        historySummary.setPageId(pageId);
        historySummary.setModified(timestamp.getTime());
        historySummary.setModifier(author);
        historySummary.setVersion(version);
        historySummary.setMinorVersion(minorVersion);

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        if (languageId != null) {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageTranslationVersionResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, wikiName);
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, spaceName);
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, pageName);
            parametersMap.put(Constants.LANGUAGE_ID_PARAMETER, languageId);
            parametersMap.put(Constants.VERSION_PARAMETER, String.format("%d.%d", version, minorVersion));
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.PAGE);
        } else {
            fullUri =
                String.format("%s%s", request.getRootRef(), resourceClassRegistry
                    .getUriPatternForResourceClass(PageVersionResource.class));
            parametersMap = new HashMap<String, String>();
            parametersMap.put(Constants.WIKI_NAME_PARAMETER, wikiName);
            parametersMap.put(Constants.SPACE_NAME_PARAMETER, spaceName);
            parametersMap.put(Constants.PAGE_NAME_PARAMETER, pageName);
            parametersMap.put(Constants.VERSION_PARAMETER, String.format("%d.%d", version, minorVersion));
            link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
            link.setRel(Relations.PAGE);
        }

        historySummary.addLink(link);

        return historySummary;
    }

    public static Comment createComment(Request request, XWikiResourceClassRegistry resourceClassRegistry,
        Document doc, com.xpn.xwiki.api.Object xwikiComment)
    {
        Comment comment = new Comment();
        comment.setId(xwikiComment.getNumber());

        Property property = xwikiComment.getProperty("author");
        if (property != null) {
            comment.setAuthor((String) property.getValue());
        }

        property = xwikiComment.getProperty("date");
        if (property != null) {
            comment.setDate(((Date) property.getValue()).getTime());
        }

        property = xwikiComment.getProperty("highlight");
        if (property != null) {
            comment.setHighlight((String) property.getValue());
        }

        property = xwikiComment.getProperty("comment");
        if (property != null) {
            comment.setText((String) property.getValue());
        }

        String fullUri;
        Map<String, String> parametersMap;
        Link link;

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(PageResource.class));
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.PAGE);
        comment.addLink(link);

        fullUri =
            String.format("%s%s", request.getRootRef(), resourceClassRegistry
                .getUriPatternForResourceClass(CommentResource.class));
        parametersMap = new HashMap<String, String>();
        parametersMap.put(Constants.WIKI_NAME_PARAMETER, doc.getWiki());
        parametersMap.put(Constants.SPACE_NAME_PARAMETER, doc.getSpace());
        parametersMap.put(Constants.PAGE_NAME_PARAMETER, doc.getName());
        parametersMap.put(Constants.COMMENT_ID_PARAMETER, String.format("%d", xwikiComment.getNumber()));
        link = new Link(Utils.formatUriTemplate(fullUri, parametersMap));
        link.setRel(Relations.SELF);
        comment.addLink(link);

        return comment;
    }
}
