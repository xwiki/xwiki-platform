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
 *
 */
package com.xpn.xwiki.xmlrpc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.swizzle.confluence.Attachment;
import org.codehaus.swizzle.confluence.Comment;
import org.codehaus.swizzle.confluence.SearchResult;
import org.codehaus.swizzle.confluence.Space;
import org.codehaus.swizzle.confluence.SpaceSummary;
import org.xwiki.query.QueryException;
import org.xwiki.xmlrpc.model.XWikiClass;
import org.xwiki.xmlrpc.model.XWikiClassSummary;
import org.xwiki.xmlrpc.model.XWikiExtendedId;
import org.xwiki.xmlrpc.model.XWikiObject;
import org.xwiki.xmlrpc.model.XWikiObjectSummary;
import org.xwiki.xmlrpc.model.XWikiPage;
import org.xwiki.xmlrpc.model.XWikiPageHistorySummary;
import org.xwiki.xmlrpc.model.XWikiPageSummary;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.api.PropertyClass;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * This class contains utility methods for building xmlrpc domain objects.
 * 
 * @version $Id$
 */
public class DomainObjectFactory
{
    private static final Log LOG = LogFactory.getLog(DomainObjectFactory.class);

    /**
     * Create a space summary
     * 
     * @return The SpaceSummary representing the space.
     */
    public static SpaceSummary createSpaceSummary(String spaceKey)
    {
        SpaceSummary result = new SpaceSummary();
        result.setKey(spaceKey);
        result.setName(spaceKey);
        result.setUrl("");

        return result;
    }

    /**
     * Create a space summary starting from the space Web home.
     * 
     * @return The SpaceSummary representing the space.
     */
    public static SpaceSummary createSpaceSummary(Document spaceWebHome)
    {
        String spaceKey = spaceWebHome.getSpace();
        String title = spaceWebHome.getTitle();

        if (title == null || title.equals("")) {
            title = spaceKey;
        }

        SpaceSummary result = new SpaceSummary();
        result.setKey(spaceKey);
        result.setName(title);
        result.setUrl(spaceWebHome.getExternalURL("view"));

        return result;
    }

    /**
     * Create a space description.
     * 
     * @return A Space object containing all the information about the space.
     */
    public static Space createSpace(String spaceKey)
    {
        Space result = new Space();
        result.setKey(spaceKey);
        result.setName(spaceKey);
        result.setDescription("No description");
        result.setHomepage("");
        result.setUrl("");

        return result;
    }

    /**
     * Create a space from its WebHome document.
     * 
     * @return A Space object containing all the information about the space.
     */
    public static Space createSpace(Document spaceWebHome)
    {
        Space result = new Space();
        result.setKey(spaceWebHome.getSpace());
        result.setName(spaceWebHome.getTitle());
        result.setDescription("No description available");
        result.setHomepage(spaceWebHome.getFullName());
        result.setUrl(spaceWebHome.getExternalURL("view"));

        return result;
    }

    /**
     * Create a page summary description from an XWiki document.
     * 
     * @return An XWikiPageSummary with the information.
     * @throws XWikiException If there is a problem getting page translations.
     */
    public static XWikiPageSummary createXWikiPageSummary(Document document) throws XWikiException
    {
        XWikiPageSummary result = new XWikiPageSummary();

        String pageTitle = document.getTitle();
        if (pageTitle.equals("")) {
            pageTitle = document.getName();
        }

        result.setId(document.getFullName());
        result.setSpace(document.getSpace());
        result.setParentId(document.getParent());
        result.setTitle(pageTitle);
        result.setUrl(document.getExternalURL("view"));
        result.setTranslations(document.getTranslationList());

        return result;
    }

    /**
     * Create a page description from an XWiki document. The page title is the current title if the current title != "",
     * otherwise it is set to the page name (i.e., the name part in the page Space.Name id)
     * 
     * @param useExtendedPageId true if the id should contain additional information concerning the version, language
     *            etc. In this case the pageId will be in the form Space.Page?param=value&param=value&...
     * @return An XWikiPage object representing the page.
     * @throws XWikiException If there is a problem getting page translations.
     */
    public static XWikiPage createXWikiPage(Document document, boolean useExtendedPageId) throws Exception
    {
        XWikiPage result = new XWikiPage();

        String pageTitle = document.getTitle();
        if (pageTitle.equals("")) {
            pageTitle = document.getName();
        }

        XWikiExtendedId extendedId = new XWikiExtendedId(document.getFullName());
        extendedId.setParameter("version", Integer.toString(document.getRCSVersion().at(0)));
        extendedId.setParameter("minorVersion", Integer.toString(document.getRCSVersion().at(1)));
        extendedId.setParameter("language", document.getLanguage());

        if (useExtendedPageId) {
            result.setId(extendedId.toString());
        } else {
            result.setId(extendedId.getBasePageId());
        }
        result.setSpace(document.getSpace());
        result.setParentId(document.getParent());
        result.setTitle(pageTitle);
        result.setUrl(document.getExternalURL("view"));
        result.setTranslations(document.getTranslationList());
        result.setVersion(document.getRCSVersion().at(0));
        result.setMinorVersion(document.getRCSVersion().at(1));
        result.setContent(document.getContent());
        result.setCreated(document.getCreationDate());
        result.setCreator(document.getCreator());
        result.setModified(document.getContentUpdateDate());
        result.setModifier(document.getContentAuthor());
        result.setHomePage(document.getName().equals("WebHome"));
        result.setLanguage(document.getLanguage());
        result.setSyntaxId(document.getSyntaxId());

        return result;
    }

    public static XWikiPage createEmptyXWikiPage()
    {
        XWikiPage result = new XWikiPage();

        result.setId("");

        result.setSpace("");
        result.setParentId("");
        result.setTitle("");
        result.setUrl("");
        result.setTranslations(new ArrayList<String>());
        result.setVersion(0);
        result.setMinorVersion(0);
        result.setContent("");
        result.setCreated(new Date());
        result.setCreator("");
        result.setModified(new Date());
        result.setModifier("");
        result.setHomePage(false);
        result.setLanguage("");
        result.setSyntaxId("");

        return result;
    }

    /**
     * Create a page history summary containing revision information about a document.
     * 
     * @return An XWikiPageHistorySummary object containing the revision information.
     */
    public static XWikiPageHistorySummary createXWikiPageHistorySummary(Document document)
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(document.getFullName());
        extendedId.setParameter("version", Integer.toString(document.getRCSVersion().at(0)));
        extendedId.setParameter("minorVersion", Integer.toString(document.getRCSVersion().at(1)));
        extendedId.setParameter("language", document.getLanguage());

        XWikiPageHistorySummary result = new XWikiPageHistorySummary();
        result.setId(extendedId.toString());
        result.setVersion(document.getRCSVersion().at(0));
        result.setMinorVersion(document.getRCSVersion().at(1));
        result.setModified(document.getContentUpdateDate());
        result.setModifier(document.getContentAuthor());

        return result;
    }

    /**
     * Create a comment object containing all the information concerning a document comment.
     * 
     * @param commentObject The XWiki object of type "XWiki.Comment" containing the actual comment.
     * @return A Comment Object containing comment information.
     */
    public static Comment createComment(Document document, com.xpn.xwiki.api.Object commentObject)
    {
        Property dateProperty = commentObject.getProperty("date");
        Property authorProperty = commentObject.getProperty("author");
        Property contentProperty = commentObject.getProperty("comment");

        Date date = dateProperty != null ? (Date) dateProperty.getValue() : new Date();
        String author = authorProperty != null ? (String) authorProperty.getValue() : "No author";
        String content = contentProperty != null ? (String) contentProperty.getValue() : "";

        Comment result = new Comment();
        XWikiExtendedId extendedId = new XWikiExtendedId(document.getFullName());
        extendedId.setParameter("commentId", Integer.toString(commentObject.getNumber()));
        result.setId(extendedId.toString());
        result.setPageId(document.getFullName());
        result.setTitle(String.format("Comment %d", commentObject.getNumber()));
        result.setContent(content);
        result.setUrl(document.getExternalURL("view"));
        result.setCreated(date);
        result.setCreator(author);

        return result;
    }

    /**
     * Create an Attachment object containing information about an attachment.
     * 
     * @return An Attachment object containing all the information.
     */
    public static Attachment createAttachment(com.xpn.xwiki.api.Attachment xwikiAttachment)
    {
        Attachment result = new Attachment();
        result.setId(String.format("%d", xwikiAttachment.getId()));
        result.setPageId(xwikiAttachment.getDocument().getFullName());
        result.setTitle(xwikiAttachment.getFilename());
        result.setFileName(xwikiAttachment.getFilename());
        /*
         * Due to a confluence API mismatch, we need to convert file sizes to strings :(
         */
        result.setFileSize(String.format("%d", xwikiAttachment.getFilesize()));
        result.setContentType(xwikiAttachment.getMimeType());
        result.setCreated(xwikiAttachment.getDate());
        result.setCreator(xwikiAttachment.getAuthor());
        result.setUrl(xwikiAttachment.getDocument().getAttachmentURL(xwikiAttachment.getFilename()));
        result.setComment(xwikiAttachment.getComment());

        return result;
    }

    /**
     * Create a summary of an XWiki class.
     * 
     * @return An XWikiClassSummary containing information about the class.
     */
    public static XWikiClassSummary createXWikiClassSummary(String className)
    {
        XWikiClassSummary result = new XWikiClassSummary();
        result.setId(className);

        return result;
    }

    /**
     * Create an XWikiClass object with all the information about a given XWiki class
     * 
     * @return An XWikiClass object with all the information.
     */
    public static XWikiClass createXWikiClass(com.xpn.xwiki.api.Class xwikiClass)
    {
        Map<String, Map<String, Object>> userClassPropertyToAttributesMap = new HashMap<String, Map<String, Object>>();

        for (Object o : xwikiClass.getProperties()) {
            PropertyClass userClassProperty = (PropertyClass) o;

            Map<String, Object> attributeToValueMap = new HashMap<String, Object>();
            attributeToValueMap.put(XWikiClass.XWIKICLASS_ATTRIBUTE, userClassProperty.getxWikiClass().getName());
            for (Object ucp : userClassProperty.getProperties()) {
                Property property = (Property) ucp;
                Object value = property.getValue();

                if (value != null) {
                    attributeToValueMap.put(property.getName(), XWikiUtils.xmlRpcConvert(value));
                }
            }

            userClassPropertyToAttributesMap.put(userClassProperty.getName(), attributeToValueMap);
        }

        XWikiClass result = new XWikiClass();
        result.setId(xwikiClass.getName());
        result.setPropertyToAttributesMap(userClassPropertyToAttributesMap);

        return result;
    }

    /**
     * Create a summary of a given xwiki object.
     * 
     * @return An XWikiObjectSummary object containing all the information.
     */
    public static XWikiObjectSummary createXWikiObjectSummary(Document document, com.xpn.xwiki.api.Object object)
    {
        String prettyName = object.getPrettyName();
        if (prettyName == null || prettyName.equals("")) {
            prettyName = String.format("%s[%d]", object.getxWikiClass().getName(), object.getNumber());
        }

        XWikiObjectSummary result = new XWikiObjectSummary();
        result.setPageId(document.getFullName());
        result.setPageVersion(document.getRCSVersion().at(0));
        result.setPageMinorVersion(document.getRCSVersion().at(1));
        result.setClassName(object.getxWikiClass().getName());
        result.setId(object.getNumber());
        result.setGuid(object.getGuid());
        result.setPrettyName(prettyName);

        return result;
    }

    /**
     * Create an XWikiObject containing all the information and attributed of a given xwiki object.
     * 
     * @return An XWikiObject containing all the information.
     * @throws QueryException
     * @throws XWikiException
     */
    public static XWikiObject createXWikiObject(com.xpn.xwiki.XWiki xwiki, XWikiContext xwikiContext,
        Document document, com.xpn.xwiki.api.Object object) throws QueryException, XWikiException
    {
        XWikiObject result = new XWikiObject();
        for (Object o : object.getProperties()) {
            Property property = (Property) o;

            String propertyName = property.getName();
            Object propertyType = getPropertyType(xwiki, xwikiContext, object, propertyName);

            if (propertyType != null) {
                result.setPropertyType(propertyName, propertyType.getClass().getName());
            } else {
                LOG.warn(String.format("Property %s of object %s:%s has a null type", propertyName,
                    document.getFullName(), object.getPrettyName()));
            }

            if (propertyType instanceof ListClass) {
                ListClass listClass = (ListClass) propertyType;
                result.setPropertyAllowedValues(propertyName, listClass.getList(xwikiContext));

            }

            /* Send only non-null values */
            Object value = property.getValue();
            if (value != null) {
                result.setProperty(propertyName, XWikiUtils.xmlRpcConvert(value));
            }
        }

        String prettyName = object.getPrettyName();
        if (prettyName == null || prettyName.equals("")) {
            prettyName = String.format("%s[%d]", object.getxWikiClass().getName(), object.getNumber());
        }

        result.setPageId(document.getFullName());
        result.setPageVersion(document.getRCSVersion().at(0));
        result.setPageMinorVersion(document.getRCSVersion().at(1));
        result.setClassName(object.getxWikiClass().getName());
        result.setId(object.getNumber());
        result.setGuid(object.getGuid());
        result.setPrettyName(prettyName);

        return result;
    }

    private static com.xpn.xwiki.objects.classes.PropertyClass getPropertyType(com.xpn.xwiki.XWiki xwiki,
        XWikiContext xwikiContext, com.xpn.xwiki.api.Object object, String propertyName) throws XWikiException
    {
        BaseClass c = xwiki.getClass(object.getxWikiClass().getName(), xwikiContext);

        for (Object o : c.getProperties()) {
            com.xpn.xwiki.objects.classes.PropertyClass propertyClass = (com.xpn.xwiki.objects.classes.PropertyClass) o;
            if (propertyClass.getName().equals(propertyName)) {
                return propertyClass;
            }
        }

        return null;
    }

    public static XWikiObject createEmptyXWikiObject()
    {
        XWikiObject result = new XWikiObject();
        result.setPageId("");
        result.setPageVersion(0);
        result.setPageMinorVersion(0);
        result.setClassName("");
        result.setId(0);
        result.setPrettyName("");

        return result;
    }

    /**
     * Create a search result object.
     * 
     * @param pageId The page id representing the page associated with this result.
     * @return A SearchResult object containing the information.
     */
    public static SearchResult createSearchResult(String pageId)
    {
        SearchResult result = new SearchResult();
        result.setId(pageId);
        result.setTitle(pageId);
        result.setUrl("");
        result.setExcerpt("");
        result.setType("pageid");

        return result;
    }
}
