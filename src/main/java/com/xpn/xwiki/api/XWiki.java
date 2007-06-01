/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

package com.xpn.xwiki.api;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import org.suigeneris.jrcs.diff.Chunk;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Object;
import java.util.*;

public class XWiki extends Api
{
    private com.xpn.xwiki.XWiki xwiki;

    /**
     * XWiki API Constructor
     * 
     * @param xwiki XWiki Main Object to wrap
     * @param context XWikiContext to wrap
     */
    public XWiki(com.xpn.xwiki.XWiki xwiki, XWikiContext context)
    {
        super(context);
        this.xwiki = xwiki;
    }

    /**
     * Priviledge API allowing to access the underlying main XWiki Object
     * 
     * @return Priviledged Main XWiki Object
     */
    public com.xpn.xwiki.XWiki getXWiki()
    {
        if (hasProgrammingRights())
            return xwiki;
        return null;
    }

    /**
     * API allowing to access the current XWiki Version
     * 
     * @return Current XWiki Version (x.y.nnnn)
     */
    public String getVersion()
    {
        return xwiki.getVersion();
    }

    /**
     * API Allowing to access the current request URL being requested
     * 
     * @return URL
     * @throws XWikiException
     */
    public String getRequestURL() throws XWikiException
    {
        return getXWikiContext().getURLFactory().getRequestURL(getXWikiContext()).toString();
    }

    /**
     * Loads an Document from the database. Rights are checked before sending back the document.
     * 
     * @param fullname Fullname of the XWiki document to be loaded
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     */
    public Document getDocument(String fullname) throws XWikiException
    {
        XWikiDocument doc = xwiki.getDocument(fullname, getXWikiContext());
        if (xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(),
            doc.getFullName(), getXWikiContext()) == false) {
            return null;
        }

        Document newdoc = doc.newDocument(getXWikiContext());
        return newdoc;
    }

    /**
     * Returns wether a document exists or not
     * 
     * @param fullname Fullname of the XWiki document to be loaded
     * @return true if the document exists, false if not
     * @throws XWikiException
     */
    public boolean exists(String fullname) throws XWikiException
    {
        return xwiki.exists(fullname, getXWikiContext());
    }

    /**
     * Verify the rights the current user has on a document. If the document requires rights and the
     * user is not authenticated he will be redirected to the login page.
     * 
     * @param docname fullname of the document
     * @param right right to check ("view", "edit", "admin", "delete")
     * @return true if it exists
     */
    public boolean checkAccess(String docname, String right)
    {
        try {
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(docname, getXWikiContext());
            return getXWikiContext().getWiki().checkAccess(right, doc, getXWikiContext());
        } catch (XWikiException e) {
            return false;
        }
    }

    /**
     * Loads an Document from the database. Rights are checked before sending back the document.
     * 
     * @param web Space to use in case no space is defined in the fullname
     * @param fullname Fullname or relative name of the document to load
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     */
    public Document getDocument(String web, String fullname) throws XWikiException
    {
        XWikiDocument doc = xwiki.getDocument(web, fullname, getXWikiContext());
        if (xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(),
            doc.getFullName(), getXWikiContext()) == false) {
            return null;
        }

        Document newdoc = doc.newDocument(getXWikiContext());
        return newdoc;
    }

    /**
     * Load a specific revision of a document
     * 
     * @param doc Document for which to load a specific revision
     * @param rev Revision number
     * @return Specific revision of a document
     * @throws XWikiException
     */
    public Document getDocument(Document doc, String rev) throws XWikiException
    {
        if ((doc == null) || (doc.getDoc() == null))
            return null;

        if (xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(),
            doc.getFullName(), getXWikiContext()) == false) {
            // Finally we return null, otherwise showing search result is a real pain
            return null;
        }

        try {
            XWikiDocument revdoc = xwiki.getDocument(doc.getDoc(), rev, getXWikiContext());
            Document newdoc = revdoc.newDocument(getXWikiContext());
            return newdoc;
        } catch (Exception e) {
            // Can't read versioned document
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Transform a text in a form compatible text
     * 
     * @param content text to transform
     * @return encoded result
     */
    public String getFormEncoded(String content)
    {
        return com.xpn.xwiki.XWiki.getFormEncoded(content);
    }

    /**
     * Transform a text in a URL compatible text
     * 
     * @param content text to transform
     * @return encoded result
     */

    public String getURLEncoded(String content)
    {
        return xwiki.getURLEncoded(content);
    }

    /**
     * Transform a text in a XML compatible text
     * 
     * @param content text to transform
     * @return encoded result
     */
    public String getXMLEncoded(String content)
    {
        return com.xpn.xwiki.XWiki.getXMLEncoded(content);
    }

    /**
     * Output content in the edit content textarea
     * 
     * @param content content to output
     * @return the textarea text content
     */
    public String getTextArea(String content)
    {
        return com.xpn.xwiki.XWiki.getTextArea(content, getXWikiContext());
    }

    /**
     * Output content in the edit content htmlarea
     * 
     * @param content content to output
     * @return the htmlarea text content
     */
    public String getHTMLArea(String content)
    {
        return xwiki.getHTMLArea(content, getXWikiContext());
    }

    /**
     * Get the list of available classes in the wiki
     * 
     * @return list of classes names
     * @throws XWikiException
     */
    public List getClassList() throws XWikiException
    {
        return xwiki.getClassList(getXWikiContext());
    }

    /**
     * Get the global MetaClass object
     * 
     * @return MetaClass object
     */
    public MetaClass getMetaclass()
    {
        return xwiki.getMetaclass();
    }

    /**
     * Priviledged API allowing to run a search on the database returning a list of data This search
     * is send to the store engine (Hibernate HQL, JCR XPATH or other)
     * 
     * @param wheresql Query to be run (HQL, XPath)
     * @return A list of rows (Object[])
     * @throws XWikiException
     */
    public List search(String wheresql) throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.search(wheresql, getXWikiContext());
        return null;
    }

    /**
     * Priviledged API allowing to run a search on the database returning a list of data This search
     * is send to the store engine (Hibernate HQL, JCR XPATH or other)
     * 
     * @param wheresql Query to be run (HQL, XPath)
     * @param nb return only 'nb' rows
     * @param start skip the 'start' first elements
     * @return A list of rows (Object[])
     * @throws XWikiException
     */
    public List search(String wheresql, int nb, int start) throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.search(wheresql, nb, start, getXWikiContext());
        return null;
    }

    /**
     * API allowing to search for document names matching a query Examples: Query: "where
     * doc.web='Main' order by doc.creationDate desc" Result: All the documents in space 'Main'
     * ordered by the creation date from the most recent Query: "where doc.name like '%sport%' order
     * by doc.name asc" Result: All the documents containing 'sport' in their name ordered by
     * document name Query: "where doc.content like '%sport%' order by doc.author" Result: All the
     * documents containing 'sport' in their content ordered by the author Query: "where doc.creator =
     * 'XWiki.LudovicDubost' order by doc.creationDate desc" Result: All the documents with creator
     * LudovicDubost ordered by the creation date from the most recent Query: "where doc.author =
     * 'XWiki.LudovicDubost' order by doc.date desc" Result: All the documents with last author
     * LudovicDubost ordered by the last modification date from the most recent Query: ",BaseObject
     * as obj where doc.fullName=obj.name and obj.className='XWiki.XWikiComments' order by doc.date
     * desc" Result: All the documents with at least one comment ordered by the last modification
     * date from the most recent Query: ",BaseObject as obj, StringProperty as prop where
     * doc.fullName=obj.name and obj.className='XWiki.XWikiComments' and obj.id=prop.id.id and
     * prop.id.name='author' and prop.value='XWiki.LudovicDubost' order by doc.date desc" Result:
     * All the documents with at least one comment from LudovicDubost ordered by the last
     * modification date from the most recent
     * 
     * @param wheresql Query to be run (either starting with ", BaseObject as obj where.." or by
     *            "where ..."
     * @return List of document names matching (Main.Page1, Main.Page2)
     * @throws XWikiException
     */
    public List searchDocuments(String wheresql) throws XWikiException
    {
        return xwiki.getStore().searchDocumentsNames(wheresql, getXWikiContext());
    }

    /**
     * API allowing to search for document names matching a query return only a limited number of
     * elements and skipping the first rows. The query part is the same as searchDocuments
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param nb return only 'nb' rows
     * @param start skip the first 'start' rows
     * @return List of document names matching
     * @throws XWikiException
     * @see List searchDocuments(String where sql)
     */
    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException
    {
        return xwiki.getStore().searchDocumentsNames(wheresql, nb, start, getXWikiContext());
    }

    /**
     * Priviledged API allowing to search for document names matching a query return only a limited
     * number of elements and skipping the first rows. The return values contain the list of columns
     * spciefied in addition to the document space and name The query part is the same as
     * searchDocuments
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param nb return only 'nb' rows
     * @param start skip the first 'start' rows
     * @param selectColumns List of columns to add to the result
     * @return List of Object[] with the column values of the matching rows
     * @throws XWikiException
     */
    public List searchDocuments(String wheresql, int nb, int start, String selectColumns)
        throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.getStore().searchDocumentsNames(wheresql, nb, start, selectColumns,
                getXWikiContext());
        return null;
    }

    /**
     * API allowing to search for documents allowing to have mutliple entries per language
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param distinctbylanguage true to return multiple rows per language
     * @return List of Document object matching
     * @throws XWikiException
     */
    public List searchDocuments(String wheresql, boolean distinctbylanguage)
        throws XWikiException
    {
        return wrapDocs(xwiki.getStore().searchDocuments(wheresql, getXWikiContext()));
    }

    /**
     * API allowing to search for documents allowing to have mutliple entries per language
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param distinctbylanguage true to return multiple rows per language
     * @return List of Document object matching
     * @param nb return only 'nb' rows
     * @param start skip the first 'start' rows
     * @throws XWikiException
     */
    public List searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start)
        throws XWikiException
    {
        return wrapDocs(xwiki.getStore().searchDocuments(wheresql, nb, start, getXWikiContext()));
    }

    /**
     * Function to wrap a list of XWikiDocument into Document objects
     * 
     * @param docs list of XWikiDocument
     * @return list of Document objects
     */
    public List wrapDocs(List docs)
    {
        List result = new ArrayList();
        if (docs != null) {
            for (Iterator iter = docs.iterator(); iter.hasNext();) {
                Object obj = iter.next();
                try {
                    if (obj instanceof XWikiDocument) {
                        XWikiDocument doc = (XWikiDocument) obj;
                        Document wrappedDoc = doc.newDocument(getXWikiContext());
                        result.add(wrappedDoc);
                    } else if (obj instanceof Document) {
                        result.add(obj);
                    } else if (obj instanceof String) {
                        Document doc = getDocument(obj.toString());
                        result.add(doc);
                    }
                } catch (XWikiException ex) {
                }
            }
        }
        return result;
    }

    /**
     * API allowing to parse a text content to evaluate velocity scripts
     * 
     * @param content
     * @return evaluated content if the content contains velocity scripts
     */
    public String parseContent(String content)
    {
        return xwiki.parseContent(content, getXWikiContext());
    }

    /**
     * API to parse the message being stored in the Context A message can be an error message or an
     * information message either as text or as a message ID pointing to ApplicationResources The
     * message is also parse for velocity scripts
     * 
     * @return Final message
     */
    public String parseMessage()
    {
        return xwiki.parseMessage(getXWikiContext());
    }

    /**
     * API to parse a message A message can be an error message or an information message either as
     * text or as a message ID pointing to ApplicationResources The message is also parse for
     * velocity scripts
     * 
     * @return Final message
     * @param id
     * @return the result of the parsed message
     */
    public String parseMessage(String id)
    {
        return xwiki.parseMessage(id, getXWikiContext());
    }

    /**
     * API to get a message A message can be an error message or an information message either as
     * text or as a message ID pointing to ApplicationResources The message is also parsed for
     * velocity scripts
     * 
     * @return Final message
     * @param id
     * @return the result of the parsed message
     */
    public String getMessage(String id)
    {
        return xwiki.getMessage(id, getXWikiContext());
    }

    /**
     * API to parse a velocity template provided by the current Skin The template is first looked in
     * the skin active for the user, the space or the wiki. If the template does not exist in that
     * skin, the template is looked up in the "parent skin" of the skin
     * 
     * @param template Template name ("view", "edit", "comment")
     * @return Evaluated content from the template
     */
    public String parseTemplate(String template)
    {
        return xwiki.parseTemplate(template, getXWikiContext());
    }

    /**
     * API to render a velocity template provided by the current Skin The template is first looked
     * in the skin active for the user, the space or the wiki. If the template does not exist in
     * that skin, the template is looked up in the "parent skin" of the skin
     * 
     * @param template Template name ("view", "edit", "comment")
     * @return Evaluated content from the template
     */
    public String renderTemplate(String template)
    {
        return xwiki.renderTemplate(template, getXWikiContext());
    }

    /**
     * Designed to include dynamic content, such as Servlets or JSPs, inside Velocity templates;
     * works by creating a RequestDispatcher, buffering the output, then returning it as a string.
     * 
     * @param url URL of the servlet
     * @return text result of the servlet
     */
    public String invokeServletAndReturnAsString(String url)
    {
        return xwiki.invokeServletAndReturnAsString(url, getXWikiContext());
    }

    /**
     * Return the URL of the static file provided by the current skin The file is first looked in
     * the skin active for the user, the space or the wiki. If the file does not exist in that skin,
     * the file is looked up in the "parent skin" of the skin. The file can be a CSS file, an image
     * file, a javascript file, etc.
     * 
     * @param filename Filename to be looked up in the skin (logo.gif, style.css)
     * @return URL to access this file
     */
    public String getSkinFile(String filename)
    {
        return xwiki.getSkinFile(filename, getXWikiContext());
    }

    /**
     * Return the URL of the static file provided by the current skin The file is first looked in
     * the skin active for the user, the space or the wiki. If the file does not exist in that skin,
     * the file is looked up in the "parent skin" of the skin. The file can be a CSS file, an image
     * file, a javascript file, etc.
     * 
     * @param filename Filename to be looked up in the skin (logo.gif, style.css)
     * @param forceSkinAction true to make sure that static files are retrieved through the skin
     *            action, to allow parsing of velocity on CSS files
     * @return URL to access this file
     */
    public String getSkinFile(String filename, boolean forceSkinAction)
    {
        return xwiki.getSkinFile(filename, forceSkinAction, getXWikiContext());
    }

    /**
     * API to retrieve the current skin for this request and user The skin is first derived from the
     * request "skin" parameter If this parameter does not exist, the user preference "skin" is
     * looked up If this parameter does not exist or is empty, the space preference "skin" is looked
     * up If this parameter does not exist or is empty, the XWiki preference "skin" is looked up If
     * this parameter does not exist or is empty, the xwiki.cfg parameter xwiki.defaultskin is
     * looked up If this parameter does not exist or is empty, the xwiki.cfg parameter
     * xwiki.defaultbaseskin is looked up If this parameter does not exist or is empty, the skin is
     * "albatross"
     * 
     * @return The current skin for this request and user
     */
    public String getSkin()
    {
        return xwiki.getSkin(getXWikiContext());
    }

    /**
     * API to retrieve the current skin for this request and user. Each skin has a skin it is based
     * on. If not the base skin is the xwiki.cfg parameter "xwiki.defaultbaseskin". If this
     * parameter does not exist or is empty, the base skin is "albatross".
     * 
     * @return The current baseskin for this request and user
     */
    public String getBaseSkin()
    {
        return xwiki.getBaseSkin(getXWikiContext());
    }

    /**
     * API to access the copyright for this space. The copyright is read in the space preferences.
     * If it does not exist or is empty it is read from the XWiki preferences.
     * 
     * @return the text for the copyright
     */
    public String getWebCopyright()
    {
        return xwiki.getWebCopyright(getXWikiContext());
    }

    /**
     * API to access an XWiki Preference There can be one preference object per language This
     * function will find the right preference object associated to the current active language
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language
     */
    public String getXWikiPreference(String prefname)
    {
        return xwiki.getXWikiPreference(prefname, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference There can be one preference object per language This
     * function will find the right preference object associated to the current active language
     * 
     * @param prefname Preference name
     * @param default_value default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language
     */
    public String getXWikiPreference(String prefname, String default_value)
    {
        return xwiki.getXWikiPreference(prefname, default_value, getXWikiContext());
    }

    /**
     * API to access an Space Preference There can be one preference object per language This
     * function will find the right preference object associated to the current active language If
     * no preference is found it will look in the XWiki Preferences
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language
     */
    public String getWebPreference(String prefname)
    {
        return xwiki.getWebPreference(prefname, getXWikiContext());
    }

    /**
     * API to access an Space Preference There can be one preference object per language This
     * function will find the right preference object associated to the current active language If
     * no preference is found it will look in the XWiki Preferences
     * 
     * @param prefname Preference name
     * @param space The space for which this preference is requested
     * @return The preference for this wiki and the current language
     */
    public String getWebPreferenceFor(String prefname, String space)
    {
        return xwiki.getWebPreference(prefname, space, "", getXWikiContext());
    }

    /**
     * API to access an Space Preference There can be one preference object per language This
     * function will find the right preference object associated to the current active language If
     * no preference is found it will look in the XWiki Preferences
     * 
     * @param prefname Preference name
     * @param default_value default value to return if the preference does not exist or is empty
     * @return The preference for this wiki and the current language
     */
    public String getWebPreference(String prefname, String default_value)
    {
        return xwiki.getWebPreference(prefname, default_value, getXWikiContext());
    }

    /**
     * API to access a Skin Preference The skin object is the current user's skin
     * 
     * @param prefname Preference name
     * @return The preference for the current skin
     */
    public String getSkinPreference(String prefname)
    {
        return xwiki.getSkinPreference(prefname, getXWikiContext());
    }

    /**
     * API to access a Skin Preference The skin object is the current user's skin
     * 
     * @param prefname Preference name
     * @param default_value default value to return if the preference does not exist or is empty
     * @return The preference for the current skin
     */
    public String getSkinPreference(String prefname, String default_value)
    {
        return xwiki.getSkinPreference(prefname, default_value, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a long number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language
     * 
     * @param prefname Preference name
     * @param space The space for which this preference is requested
     * @param default_value default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in long format
     */
    public String getWebPreferenceFor(String prefname, String space, String default_value)
    {
        return xwiki.getWebPreference(prefname, space, default_value, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a long number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language
     * 
     * @param prefname Preference name
     * @param default_value default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in long format
     */
    public long getXWikiPreferenceAsLong(String prefname, long default_value)
    {
        return xwiki.getXWikiPreferenceAsLong(prefname, default_value, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a long number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language in long format
     */
    public long getXWikiPreferenceAsLong(String prefname)
    {
        return xwiki.getXWikiPreferenceAsLong(prefname, getXWikiContext());
    }

    /**
     * API to access an Web Preference as a long number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language If no preference is found it will look for the XWiki Preference
     * 
     * @param prefname Preference name
     * @param default_value default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in long format
     */
    public long getWebPreferenceAsLong(String prefname, long default_value)
    {
        return xwiki.getWebPreferenceAsLong(prefname, default_value, getXWikiContext());
    }

    /**
     * API to access an Web Preference as a long number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language If no preference is found it will look for the XWiki Preference
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language in long format
     */
    public long getWebPreferenceAsLong(String prefname)
    {
        return xwiki.getWebPreferenceAsLong(prefname, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as an int number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language
     * 
     * @param prefname Preference name
     * @param default_value default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in int format
     */
    public int getXWikiPreferenceAsInt(String prefname, int default_value)
    {
        return xwiki.getXWikiPreferenceAsInt(prefname, default_value, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a int number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language in int format
     */
    public int getXWikiPreferenceAsInt(String prefname)
    {
        return xwiki.getXWikiPreferenceAsInt(prefname, getXWikiContext());
    }

    /**
     * API to access an Web Preference as a int number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language If no preference is found it will look for the XWiki Preference
     * 
     * @param prefname Preference name
     * @param default_value default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in int format
     */
    public int getWebPreferenceAsInt(String prefname, int default_value)
    {
        return xwiki.getWebPreferenceAsInt(prefname, default_value, getXWikiContext());
    }

    /**
     * API to access an Web Preference as a int number There can be one preference object per
     * language This function will find the right preference object associated to the current active
     * language If no preference is found it will look for the XWiki Preference
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language in int format
     */
    public int getWebPreferenceAsInt(String prefname)
    {
        return xwiki.getWebPreferenceAsInt(prefname, getXWikiContext());
    }

    /**
     * API to access a User Preference This function will look in the User profile for the
     * preference If no preference is found it will look in the Space Preferences If no preference
     * is found it will look in the XWiki Preferences
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language
     */
    public String getUserPreference(String prefname)
    {
        return xwiki.getUserPreference(prefname, getXWikiContext());
    }

    /**
     * API to access a User Preference from cookie This function will look in the session cookie for
     * the preference
     * 
     * @param prefname Preference name
     * @return The preference for this wiki and the current language
     */
    public String getUserPreferenceFromCookie(String prefname)
    {
        return xwiki.getUserPreferenceFromCookie(prefname, getXWikiContext());
    }

    /**
     * First try to find the current language in use from the XWiki context. If none is used
     * and if the wiki is not multilingual use the default language defined in the XWiki
     * preferences. If the wiki is multilingual try to get the language passed in the request.
     * If none was passed try to get it from a cookie. If no language cookie exists then use the
     * user default language and barring that use the browser's "Accept-Language" header sent in
     * HTTP request. If none is defined use the default language.
     *
     * @return the language to use
     */
    public String getLanguagePreference()
    {
        return xwiki.getLanguagePreference(getXWikiContext());
    }

    /**
     * @deprecated use {@link #getLanguagePreference()} instead
     */
    public String getDocLanguagePreference()
    {
        return xwiki.getDocLanguagePreference(getXWikiContext());
    }

    /**
     * API to access the interface language preference for the request Order of evaluation is:
     * Language of the wiki in mono-lingual mode language request paramater language in context
     * language user preference language in cookie language accepted by the navigator
     * 
     * @return the document language preference for the request
     */
    public String getInterfaceLanguagePreference()
    {
        return xwiki.getInterfaceLanguagePreference(getXWikiContext());
    }

    /**
     * API to check if wiki is in multi-wiki mode (virtual)
     * 
     * @return true for multi-wiki/false for mono-wiki
     */
    public boolean isVirtual()
    {
        return xwiki.isVirtual();
    }

    /**
     * API to check is wiki is multi-lingual
     * 
     * @return true for multi-lingual/false for mono-lingual
     */
    public boolean isMultiLingual()
    {
        return xwiki.isMultiLingual(getXWikiContext());
    }

    /**
     * Priviledged API to flush the cache of the Wiki installation This flushed the cache of all
     * wikis, all plugins, all renderers
     */
    public void flushCache()
    {
        if (hasProgrammingRights())
            xwiki.flushCache(getXWikiContext());
    }

    /**
     * Priviledged API to reset the rendenring engine This would restore the rendering engine
     * evaluation loop and take into account new configuration parameters
     */
    public void resetRenderingEngine()
    {
        if (hasProgrammingRights())
            try {
                xwiki.resetRenderingEngine(getXWikiContext());
            } catch (XWikiException e) {
            }
    }

    /**
     * Priviledged API to create a new user from the request This API is used by RegisterNewUser
     * wiki page
     * 
     * @return true for success/false for failure
     * @throws XWikiException
     */
    public int createUser() throws XWikiException
    {
        return createUser(false, "edit");
    }

    /**
     * Priviledged API to create a new user from the request This API is used by RegisterNewUser
     * wiki page This version sends a validation email to the user Configuration of validation email
     * is in the XWiki Preferences
     * 
     * @param withValidation true to send the validationemail
     * @return true for success/false for failure
     * @throws XWikiException
     */
    public int createUser(boolean withValidation) throws XWikiException
    {
        return createUser(withValidation, "edit");
    }

    /**
     * Priviledged API to create a new user from the request This API is used by RegisterNewUser
     * wiki page This version sends a validation email to the user Configuration of validation email
     * is in the XWiki Preferences
     * 
     * @param withValidation true to send the validation email
     * @param userRights Rights to set for the user for it's own page(defaults to "edit")
     * @return true for success/false for failure
     * @throws XWikiException
     */
    public int createUser(boolean withValidation, String userRights) throws XWikiException
    {
        boolean registerRight;
        try {
            // So, what's the register right for? This says that if the creator of the page
            // (Admin) has programming rights, anybody can register. Is this OK?
            if (hasProgrammingRights()) {
                registerRight = true;
            } else {
                registerRight =
                    xwiki.getRightService().hasAccessLevel("register",
                        getXWikiContext().getUser(), "XWiki.XWikiPreferences", getXWikiContext());
            }

            if (registerRight)
                return xwiki.createUser(withValidation, userRights, getXWikiContext());
            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -2;
        }

    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies
     * to documents from a existing wiki Assigns the admin rights, creates the Wiki identification
     * page in the main wiki
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for
     *         other errors
     * @throws XWikiException
     */
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
        String baseWikiName, boolean failOnExist) throws XWikiException
    {
        return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, "", null, failOnExist);
    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies
     * to documents from a existing wiki Assigns the admin rights, creates the Wiki identification
     * page in the main wiki
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param description Description of the Wiki
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for
     *         other errors
     * @throws XWikiException
     */
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
        String baseWikiName, String description, boolean failOnExist) throws XWikiException
    {
        return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, null,
            failOnExist);
    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies
     * to documents from a existing wiki Assigns the admin rights, creates the Wiki identification
     * page in the main wiki Copy is limited to documents of a specified language. If a document for
     * the language is not found, the default language document is used
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param description Description of the Wiki
     * @param language Language to copy
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for
     *         other errors
     * @throws XWikiException
     */
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
        String baseWikiName, String description, String language, boolean failOnExist)
        throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description,
                language, failOnExist, getXWikiContext());
        return -1;
    }

    /**
     * Priviledged API to validate the return code given by a user in response to an email
     * validation email The validation information are taken from the request object
     * 
     * @param withConfirmEmail true to send a account confirmation email/false to not send it
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for
     *         other errors
     * @throws XWikiException
     */
    public int validateUser(boolean withConfirmEmail) throws XWikiException
    {
        return xwiki.validateUser(withConfirmEmail, getXWikiContext());
    }

    /**
     * Priviledged API to add a user to the XWiki.XWikiAllGroup
     * 
     * @param fullwikiname user name to add
     * @throws XWikiException
     */
    public void addToAllGroup(String fullwikiname) throws XWikiException
    {
        if (hasProgrammingRights())
            xwiki.setUserDefaultGroup(fullwikiname, getXWikiContext());
    }

    /**
     * Priviledged API to send a confirmation email to a user
     * 
     * @param xwikiname user to send the email to
     * @param password password to put in the mail
     * @param email email to send to
     * @param add_message Additional message to send to the user
     * @param contentfield Preference field to use as a mail template
     * @throws XWikiException if the mail was not send successfully
     */
    public void sendConfirmationMail(String xwikiname, String password, String email,
        String add_message, String contentfield) throws XWikiException
    {
        if (hasProgrammingRights())
            xwiki.sendConfirmationEmail(xwikiname, password, email, add_message, contentfield,
                getXWikiContext());
    }

    /**
     * Priviledged API to send a confirmation email to a user
     * 
     * @param xwikiname user to send the email to
     * @param password password to put in the mail
     * @param email email to send to
     * @param contentfield Preference field to use as a mail template
     * @throws XWikiException if the mail was not send successfully
     */
    public void sendConfirmationMail(String xwikiname, String password, String email,
        String contentfield) throws XWikiException
    {
        if (hasProgrammingRights())
            xwiki.sendConfirmationEmail(xwikiname, password, email, "", contentfield,
                getXWikiContext());
    }

    /**
     * Priviledged API to send a message to an email address
     * 
     * @param sender email of the sender of the message
     * @param recipient email of the recipient of the message
     * @param message Message to send
     * @throws XWikiException if the mail was not send successfully
     */
    public void sendMessage(String sender, String recipient, String message)
        throws XWikiException
    {
        if (hasProgrammingRights())
            xwiki.sendMessage(sender, recipient, message, getXWikiContext());
    }

    /**
     * Priviledged API to send a message to an email address
     * 
     * @param sender email of the sender of the message
     * @param recipient emails of the recipients of the message
     * @param message Message to send
     * @throws XWikiException if the mail was not send successfully
     */
    public void sendMessage(String sender, String[] recipient, String message)
        throws XWikiException
    {
        if (hasProgrammingRights())
            xwiki.sendMessage(sender, recipient, message, getXWikiContext());
    }

    /**
     * Priviledged API to copy a document to another document in the same wiki
     * 
     * @param docname source document
     * @param targetdocname target document
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname) throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.copyDocument(docname, targetdocname, null, null, null, false,
                getXWikiContext());
        return false;
    }

    /**
     * Priviledged API to copy a translation of a document to another document in the same wiki
     * 
     * @param docname source document
     * @param targetdocname target document
     * @param wikilanguage language to copy
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname, String wikilanguage)
        throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.copyDocument(docname, targetdocname, null, null, wikilanguage, false,
                getXWikiContext());
        return false;
    }

    /**
     * Priviledged API to copy a translation of a document to another document of the same name in
     * another wiki
     * 
     * @param docname source document
     * @param sourceWiki source wiki
     * @param targetWiki target wiki
     * @param wikilanguage language to copy
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String sourceWiki, String targetWiki,
        String wikilanguage) throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.copyDocument(docname, docname, sourceWiki, targetWiki, wikilanguage,
                true, getXWikiContext());
        return false;
    }

    /**
     * Priviledged API to copy a translation of a document to another document of the same name in
     * another wiki additionally resetting the version
     * 
     * @param docname source document
     * @param sourceWiki source wiki
     * @param targetWiki target wiki
     * @param wikilanguage language to copy
     * @param reset true to reset versions
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname, String sourceWiki,
        String targetWiki, String wikilanguage, boolean reset) throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.copyDocument(docname, targetdocname, sourceWiki, targetWiki,
                wikilanguage, reset, getXWikiContext());
        return false;
    }

    /**
     * Priviledged API to copy a translation of a document to another document of the same name in
     * another wiki additionally resetting the version and overwriting the previous document
     * 
     * @param docname source document
     * @param sourceWiki source wiki
     * @param targetWiki target wiki
     * @param wikilanguage language to copy
     * @param reset true to reset versions
     * @param force true to overwrite the previous document
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname, String sourceWiki,
        String targetWiki, String wikilanguage, boolean reset, boolean force)
        throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.copyDocument(docname, targetdocname, sourceWiki, targetWiki,
                wikilanguage, reset, force, getXWikiContext());
        return false;
    }

    /**
     * Priviledged API to copy a space to another wiki, optionally deleting all document of the
     * target space
     * 
     * @param web source Space
     * @param sourceWiki source Wiki
     * @param targetWiki target Wiki
     * @param wikiLanguage language to copy
     * @param clean true to delete all document of the target space
     * @return number of copied documents
     * @throws XWikiException if the space was not copied properly
     */
    public int copyWikiWeb(String web, String sourceWiki, String targetWiki, String wikiLanguage,
        boolean clean) throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.copyWikiWeb(web, sourceWiki, targetWiki, wikiLanguage, clean,
                getXWikiContext());
        return -1;
    }

    /**
     * API to include a topic into another The topic is rendered fully in the context of itself
     * 
     * @param topic page name of the topic to include
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeTopic(String topic) throws XWikiException
    {
        return includeTopic(topic, true);
    }

    /**
     * API to execute a form in the context of an including topic The rendering is evaluated in the
     * context of the including topic All velocity variables are the one of the including topic This
     * api is usually called using #includeForm in a page, which modifies the behavior of "Edit this
     * page" button to direct for Form mode (inline)
     * 
     * @param topic page name of the form to execute
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeForm(String topic) throws XWikiException
    {
        return includeForm(topic, true);
    }

    /**
     * API to include a topic into another, optionnaly surrounding the content with {pre}{/pre} to
     * avoid future wiki rendering The topic is rendered fully in the context of itself
     * 
     * @param topic page name of the topic to include
     * @param pre true to add {pre} {/pre}
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeTopic(String topic, boolean pre) throws XWikiException
    {
        if (pre)
            return "{pre}" + xwiki.include(topic, false, getXWikiContext()) + "{/pre}";
        return xwiki.include(topic, false, getXWikiContext());
    }

    /**
     * API to execute a form in the context of an including topic, optionnaly surrounding the
     * content with {pre}{/pre} to avoid future wiki rendering The rendering is evaluated in the
     * context of the including topic All velocity variables are the one of the including topic This
     * api is usually called using #includeForm in a page, which modifies the behavior of "Edit this
     * page" button to direct for Form mode (inline)
     * 
     * @param topic page name of the form to execute
     * @param pre true to add {pre} {/pre}
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeForm(String topic, boolean pre) throws XWikiException
    {
        if (pre)
            return "{pre}" + xwiki.include(topic, true, getXWikiContext()) + "{/pre}";
        return xwiki.include(topic, true, getXWikiContext());
    }

    /**
     * API to check rights on the current document for the current user
     * 
     * @param level right to check (view, edit, comment, delete)
     * @return true if right is granted/false if not
     */
    public boolean hasAccessLevel(String level)
    {
        try {
            return xwiki.getRightService().hasAccessLevel(level, getXWikiContext().getUser(),
                getXWikiContext().getDoc().getFullName(), getXWikiContext());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * v * API to check rights on a document for a given user
     * 
     * @param level right to check (view, edit, comment, delete)
     * @param user user for which to check the right
     * @param docname document on which to check the rights
     * @return true if right is granted/false if not
     */
    public boolean hasAccessLevel(String level, String user, String docname)
    {
        try {
            return xwiki.getRightService()
                .hasAccessLevel(level, user, docname, getXWikiContext());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * API to render a text in the context of a document
     * 
     * @param text text to render
     * @param doc the text is evaluated in the content of this document
     * @return evaluated content
     * @throws XWikiException if the evaluation went wrong
     */
    public String renderText(String text, Document doc) throws XWikiException
    {
        return xwiki.getRenderingEngine().renderText(text, doc.getDoc(), getXWikiContext());
    }

    /**
     * API to render a chunk (difference between two versions
     * 
     * @param chunk difference between versions to render
     * @param doc document to use as a context for rendering
     * @return resuilt of the rendering
     */
    public String renderChunk(Chunk chunk, Document doc)
    {
        return renderChunk(chunk, false, doc);
    }

    /**
     * API to render a chunk (difference between two versions
     * 
     * @param chunk difference between versions to render
     * @param doc document to use as a context for rendering
     * @param source true to render the difference as wiki source and not as wiki rendered text
     * @return resuilt of the rendering
     */
    public String renderChunk(Chunk chunk, boolean source, Document doc)
    {
        StringBuffer buf = new StringBuffer();
        chunk.toString(buf, "", "\n");
        if (source == true)
            return buf.toString();

        try {
            return xwiki.getRenderingEngine().renderText(buf.toString(), doc.getDoc(),
                getXWikiContext());
        } catch (Exception e) {
            return buf.toString();
        }
    }

    /**
     * API to list the current spaces in thiswiki
     * 
     * @return a list for strings reprenseting the spaces
     * @throws XWikiException if something went wrong
     */
    public List getSpaces() throws XWikiException
    {
        return xwiki.getSpaces(getXWikiContext());
    }

    /**
     * API to list all documents in a space
     * 
     * @param SpaceName space tolest
     * @return A list of strings to lest the document
     * @throws XWikiException if the loading went wrong
     */
    public List getSpaceDocsName(String SpaceName) throws XWikiException
    {
        return xwiki.getSpaceDocsName(SpaceName, getXWikiContext());
    }

    /**
     * API to retrieve a java object with the current date
     * 
     * @return the current date
     */
    public Date getCurrentDate()
    {
        return xwiki.getCurrentDate();
    }

    /**
     * API to retrieve a java object with the current date
     * 
     * @return the current date
     */
    public Date getDate()
    {
        return xwiki.getCurrentDate();
    }

    /**
     * API to retrieve the time delta in milliseconds between the current date and the time passed
     * as parameter.
     * 
     * @param time
     * @return delta of the time in milliseconds
     */
    public int getTimeDelta(long time)
    {
        return xwiki.getTimeDelta(time);
    }

    /**
     * API to convert a date from a time in milliseconds since 01/01/1970 to a Java Date Object
     * 
     * @param time time in milliseconds since 1970, 00:00:00 GMT
     * @return Date object
     */
    public Date getDate(long time)
    {
        return xwiki.getDate(time);
    }

    /**
     * API to split a text to an array of texts, according to a separator
     * 
     * @param str original text
     * @param sep separator characters. The separator is one or more of the separator characters
     * @return An array of the splitted text
     */
    public String[] split(String str, String sep)
    {
        return xwiki.split(str, sep);
    }

    /**
     * API to retrieve an exception stack trace in a String
     * 
     * @param e Exception to retrieve the stack trace from
     * @return Text showing the exception stack trace
     */
    public String printStrackTrace(Throwable e)
    {
        return xwiki.printStrackTrace(e);
    }

    /**
     * API to retrieve the current encoding of the wiki engine The encoding is stored in xwiki.cfg
     * Default encoding is ISO-8891-1
     * 
     * @return encoding active in this wiki
     */
    public String getEncoding()
    {
        return xwiki.getEncoding();
    }

    /**
     * API to retrieve a NULL object This is usefull in Velocity where there is no real null object
     * for comparaisons
     * 
     * @return A null Object
     */
    public Object getNull()
    {
        return null;
    }

    /**
     * API to retrieve a New Line character This is usefull in Velocity where there is no real new
     * line character for inclusion in texts
     * 
     * @return A new line character
     */
    public String getNl()
    {
        return "\n";
    }

    /**
     * API to retrieve the URL of an attached file in a Wiki Document The URL is generated
     * differently depending on the environement (Servlet, Portlet, PDF, etc..) The URL generation
     * can be modified by implementing a new XWikiURLFactory object For compatibility with any
     * target environement (and especially the portlet environment) It is important to always use
     * the URL functions to generate URL and never hardcode URLs
     * 
     * @param fullname page name which includes the attached file
     * @param filename attached filename to create a link for
     * @return a URL as a string pointing to the filename
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getAttachmentURL(String fullname, String filename) throws XWikiException
    {
        return xwiki.getAttachmentURL(fullname, filename, getXWikiContext());
    }

    /**
     * API to retrieve the URL of an a Wiki Document in view mode The URL is generated differently
     * depending on the environement (Servlet, Portlet, PDF, etc..) The URL generation can be
     * modified by implementing a new XWikiURLFactory object For compatibility with any target
     * environement (and especially the portlet environment) It is important to always use the URL
     * functions to generate URL and never hardcode URLs
     * 
     * @param fullname page name which includes the attached file
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getURL(String fullname) throws XWikiException
    {
        return xwiki.getURL(fullname, "view", getXWikiContext());
    }

    /**
     * API to retrieve the URL of an a Wiki Document in any mode The URL is generated differently
     * depending on the environement (Servlet, Portlet, PDF, etc..) The URL generation can be
     * modified by implementing a new XWikiURLFactory object For compatibility with any target
     * environement (and especially the portlet environment) It is important to always use the URL
     * functions to generate URL and never hardcode URLs
     * 
     * @param fullname page name which includes the attached file
     * @param action mode in which to access the document (view/edit/save/..). Any valid XWiki
     *            action is possible.
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getURL(String fullname, String action) throws XWikiException
    {
        return xwiki.getURL(fullname, action, getXWikiContext());
    }

    /**
     * API to retrieve the URL of an a Wiki Document in any mode, optionally adding a query string
     * The URL is generated differently depending on the environement (Servlet, Portlet, PDF, etc..)
     * The URL generation can be modified by implementing a new XWikiURLFactory object The query
     * string will be modified to be added in the way the environement needs it It is important to
     * not add the query string parameter manually after a URL Some environements will not accept
     * this (like the Portlet environement)
     * 
     * @param fullname page name which includes the attached file
     * @param action mode in which to access the document (view/edit/save/..). Any valid XWiki
     *            action is possible.
     * @param querystring Query String to provide in the usual mode (name1=value1&name2=value=2)
     *            including encoding.
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getURL(String fullname, String action, String querystring)
        throws XWikiException
    {
        return xwiki.getURL(fullname, action, querystring, getXWikiContext());
    }

    /**
     * @see #getExoService(String)
     * @deprecated use {@link #getExoService(String)} instead
     */
    public java.lang.Object getService(String className) throws XWikiException
    {
        return getExoService(className);
    }

    /**
     * Privileged API to access an eXo Platform service from the Wiki Engine
     *
     * @param className eXo classname to retrieve the service from
     * @return A object representing the service or null if the user doesn't have programming
     *         rights
     * @throws XWikiException if the service cannot be loaded
     * @since 1.1 Beta 1
     */
    public java.lang.Object getExoService(String className) throws XWikiException
    {
        java.lang.Object service = null;

        if (hasProgrammingRights()) {
            service = xwiki.getExoService(className);
        }

        return service;
    }

    /**
     * @see #getExoPortalService(String)
     * @deprecated use {@link #getExoPortalService(String)} instead
     */
    public java.lang.Object getPortalService(String className) throws XWikiException
    {
        return getExoPortalService(className);
    }

    /**
     * Privileged API to access an eXo Platform Portal service from the Wiki Engine
     * 
     * @param className eXo classname to retrieve the service from
     * @return A object representing the service or null if the user doesn't have programming
     *         rights
     * @throws XWikiException if the service cannot be loaded
     * @since 1.1 Beta 1
     */
    public java.lang.Object getExoPortalService(String className) throws XWikiException
    {
        java.lang.Object portalService = null;

        if (hasProgrammingRights()) {
            portalService = xwiki.getExoPortalService(className);
        }

        return portalService;
    }

    /**
     * API to retrieve an List object This is usefull is velocity where you cannot create objects
     * 
     * @return a java.util.ArrayList object casted to List
     */
    public List getArrayList()
    {
        return new ArrayList();
    }

    /**
     * API to retrieve an Map object This is usefull is velocity where you cannot create objects
     * 
     * @return a java.util.HashMap object casted to Map
     */
    public Map getHashMap()
    {
        return new HashMap();
    }

    public Map getTreeMap()
    {
        return new TreeMap();
    }

    /**
     * API to sort a list over standard comparator. Elements need to be mutally comparable and
     * implement the Comparable interface
     * 
     * @param list List to sort
     * @return the sorted list (in the same oject)
     * @see Collections void sort(List list)
     */
    public List sort(List list)
    {
        Collections.sort(list);
        return list;
    }

    public Number toNumber(Object o)
    {
        try {
            return new Long(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * API to generate a random string
     *
     * @param size Desired size of the string
     * @return the generated string
     */
    public String generateRandomString(int size)
    {
        return xwiki.generateRandomString(size);
    }

    /**
     * API to Outpout an BufferedImage object into the response outputstream Once this function has
     * been called, not further action is possible Users should set $context.setFinished(true) to
     * avoid template output The image is outpout as image/jpeg
     * 
     * @param image BufferedImage to output
     * @throws IOException exception if the output fails
     */
    public void outputImage(BufferedImage image) throws IOException
    {
        JPEGImageEncoder encoder;
        OutputStream ostream = getXWikiContext().getResponse().getOutputStream();
        encoder = JPEGCodec.createJPEGEncoder(ostream);
        encoder.encode(image);
        ostream.flush();
    }

    /**
     * API to access the current starts for the Wiki for a specific action It retrieves the number
     * of times the action was performed for the whole wiki The statistics module need to be
     * activated (xwiki.stats=1 in xwiki.cfg)
     * 
     * @param action action for which to retrieve statistics (view/save/download)
     * @return A DocumentStats object with number of actions performed, unique visitors, number of
     *         visits
     */
    public DocumentStats getCurrentMonthXWikiStats(String action)
    {
        return getXWikiContext().getWiki().getStatsService(getXWikiContext()).getDocMonthStats(
            "", action, new Date(), getXWikiContext());
    }

    /**
     * API to retrieve a viewable referer text for a referer Referers are URL where users have
     * clicked on a link to an XWiki page Search engine referer URLs are transformed to a nicer view
     * (Google: search query string) For other URL the http:// part is stripped
     * 
     * @param referer referer URL to transform
     * @return A viewable string
     */
    public String getRefererText(String referer)
    {
        try {
            return xwiki.getRefererText(referer, getXWikiContext());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * API to retrieve a viewable referer text for a referer with a maximum length Referers are URL
     * where users have clicked on a link to an XWiki page Search engine referer URLs are
     * transformed to a nicer view (Google: search query string) For other URL the http:// part is
     * stripped
     * 
     * @param referer referer URL to transform
     * @param length Maximum length. "..." is added to the end of the text
     * @return A viewable string
     */
    public String getShortRefererText(String referer, int length)
    {
        try {
            return xwiki.getRefererText(referer, getXWikiContext()).substring(0, length);
        } catch (Exception e) {
            return xwiki.getRefererText(referer, getXWikiContext());
        }
    }

    /**
     * Deprecated API which was retrieving the SQL to represent the fullName Document field
     * depending on the database used This is not needed anymore and returns 'doc.fullName' for all
     * databases
     * 
     * @deprecated
     * @return "doc.fullName"
     */
    public String getFullNameSQL()
    {
        return xwiki.getFullNameSQL();
    }

    /**
     * API to retrieve a link to the User Name page displayed for the first name and last name of
     * the user The link will link to the page on the wiki where the user is registered (in virtual
     * wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user)
    {
        return xwiki.getUserName(user, null, getXWikiContext());
    }

    /**
     * API to retrieve a link to the User Name page displayed with a custom view The link will link
     * to the page on the wiki where the user is registered (in virtual wiki mode) The formating is
     * done using the format parameter which can contain velocity scripting and access all
     * properties of the User profile using variables ($first_name $last_name $email $city)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user, String format)
    {
        return xwiki.getUserName(user, format, getXWikiContext());
    }

    /**
     * API to retrieve a link to the User Name page displayed for the first name and last name of
     * the user The link will link to the page on the local wiki even if the user is registered on a
     * different wiki (in virtual wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user)
    {
        try {
            return xwiki.getUserName(user.substring(user.indexOf(":") + 1), null,
                getXWikiContext());
        } catch (Exception e) {
            return xwiki.getUserName(user, null, getXWikiContext());
        }
    }

    /**
     * API to retrieve a link to the User Name page displayed with a custom view The link will link
     * to the page on the local wiki even if the user is registered on a different wiki (in virtual
     * wiki mode) The formating is done using the format parameter which can contain velocity
     * scripting and access all properties of the User profile using variables ($first_name
     * $last_name $email $city)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user, String format)
    {
        try {
            return xwiki.getUserName(user.substring(user.indexOf(":") + 1), format,
                getXWikiContext());
        } catch (Exception e) {
            return xwiki.getUserName(user, format, getXWikiContext());
        }
    }

    /**
     * API to retrieve a text representing the user with the first name and last name of the user
     * With the link param set to false it will not link to the user page With the link param set to
     * true, the link will link to the page on the wiki where the user was registered (in virtual
     * wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user, boolean link)
    {
        return xwiki.getUserName(user, null, link, getXWikiContext());
    }

    /**
     * API to retrieve a text representing the user with a custom view With the link param set to
     * false it will not link to the user page With the link param set to true, the link will link
     * to the page on the wiki where the user was registered (in virtual wiki mode) The formating is
     * done using the format parameter which can contain velocity scripting and access all
     * properties of the User profile using variables ($first_name $last_name $email $city)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user, String format, boolean link)
    {
        return xwiki.getUserName(user, format, link, getXWikiContext());
    }

    /**
     * API to retrieve a text representing the user with the first name and last name of the user
     * With the link param set to false it will not link to the user page With the link param set to
     * true, the link will link to the page on the local wiki even if the user is registered on a
     * different wiki (in virtual wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user, boolean link)
    {
        try {
            return xwiki.getUserName(user.substring(user.indexOf(":") + 1), null, link,
                getXWikiContext());
        } catch (Exception e) {
            return xwiki.getUserName(user, null, link, getXWikiContext());
        }
    }

    /**
     * API to retrieve a text representing the user with a custom view The formating is done using
     * the format parameter which can contain velocity scripting and access all properties of the
     * User profile using variables ($first_name $last_name $email $city) With the link param set to
     * false it will not link to the user page With the link param set to true, the link will link
     * to the page on the local wiki even if the user is registered on a different wiki (in virtual
     * wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user, String format, boolean link)
    {
        try {
            return xwiki.getUserName(user.substring(user.indexOf(":") + 1), format, link,
                getXWikiContext());
        } catch (Exception e) {
            return xwiki.getUserName(user, format, link, getXWikiContext());
        }
    }

    public User getUser()
    {
        return xwiki.getUser(getXWikiContext());
    }

    public User getUser(String username)
    {
        return xwiki.getUser(username, getXWikiContext());
    }

    /**
     * API allowing to format a date according to the default Wiki setting The date format is
     * provided in the 'dateformat' parameter of the XWiki Preferences
     * 
     * @param date date object to format
     * @return A string with the date formating from the default Wiki setting
     */
    public String formatDate(Date date)
    {
        return xwiki.formatDate(date, null, getXWikiContext());
    }

    /**
     * API allowing to format a date according to a custom format The date format is from
     * java.text.SimpleDateFormat Example: "dd/MM/yyyy HH:mm:ss" or "d MMM yyyy" If the format is
     * invalid the default format will be used to show the date
     * 
     * @param date date to format
     * @param format format of the date to be used
     * @return the formatted date
     * @see java.text.SimpleDateFormat
     */
    public String formatDate(Date date, String format)
    {
        return xwiki.formatDate(date, format, getXWikiContext());
    }

    /**
     * Returns a plugin from the plugin API. Plugin Rights can be verified.
     * 
     * @param name Name of the plugin to retrieve (either short of full class name)
     * @return a plugin object
     */
    public Api get(String name)
    {
        return xwiki.getPluginApi(name, getXWikiContext());
    }

    /**
     * Returns a plugin from the plugin API. Plugin Rights can be verified.
     * 
     * @param name Name of the plugin to retrieve (either short of full class name)
     * @return a plugin object
     */
    public Api getPlugin(String name)
    {
        return xwiki.getPluginApi(name, getXWikiContext());
    }

    /**
     * Returns the recently visited pages for a specific action
     * 
     * @param action ("view" or "edit")
     * @param size how many recent actions to retrieve
     * @return a ArrayList of document names
     */
    public java.util.Collection getRecentActions(String action, int size)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return new ArrayList();
        return stats.getRecentActions(action, size, getXWikiContext());
    }

    /**
     * Returns the Advertisement system from the preferences
     * 
     * @return "google" or "none"
     */
    public String getAdType()
    {
        return xwiki.getAdType(getXWikiContext());
    }

    /**
     * Returns the Advertisement client ID from the preferences
     * 
     * @return an Ad affiliate ID
     */
    public String getAdClientId()
    {
        return xwiki.getAdClientId(getXWikiContext());
    }

    /**
     * Retrieves a int from a String
     * 
     * @param str String to convert to int
     * @return the int or zero in case of exception
     */
    public int parseInt(String str)
    {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Retrieves a int from a String
     * 
     * @param str String to convert to int
     * @return the int or zero in case of exception
     */
    public Integer parseInteger(String str)
    {
        return new Integer(parseInt(str));
    }

    /**
     * Retrieves a long from a String
     * 
     * @param str String to convert to long
     * @return the long or zero in case of exception
     */
    public long parseLong(String str)
    {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Retrieves a float from a String
     * 
     * @param str String to convert to float
     * @return the float or zero in case of exception
     */
    public float parseFloat(String str)
    {
        try {
            return Float.parseFloat(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Retrieves a double from a String
     * 
     * @param str String to convert to double
     * @return the double or zero in case of exception
     */
    public double parseDouble(String str)
    {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication
     * 
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl, String username, String password) throws IOException
    {
        try {
            return xwiki.getURLContent(surl, username, password, context);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL
     * 
     * @param surl url to retrieve
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl) throws IOException
    {
        try {
            return xwiki.getURLContent(surl, context);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication
     *
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @param timeout manuel timeout in milliseconds
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl, String username, String password, int timeout) throws IOException
    {
        try {
            return xwiki.getURLContent(surl, username, password, timeout, xwiki.getHttpUserAgent(context));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL
     *
     * @param surl url to retrieve
     * @param timeout manuel timeout in milliseconds
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl, int timeout) throws IOException
    {
        try {
            return xwiki.getURLContent(surl, timeout, xwiki.getHttpUserAgent(context));
        } catch (Exception e) {
            return "";
        }
    }


    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication as Bytes
     * 
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @return Content of the specified URL
     * @throws IOException
     */
    public byte[] getURLContentAsBytes(String surl, String username, String password)
        throws IOException
    {
        try {
            return xwiki.getURLContentAsBytes(surl, username, password, context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL as Bytes
     * 
     * @param surl url to retrieve
     * @return Content of the specified URL
     * @throws IOException
     */
    public byte[] getURLContentAsBytes(String surl) throws IOException
    {
        try {
            return xwiki.getURLContentAsBytes(surl, context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Filters text to be include in = or like clause in SQL
     * 
     * @param text text to filter
     * @return filtered text
     */
    public String sqlfilter(String text)
    {
        return Utils.SQLFilter(text);
    }

    /**
     * Returns the list of Macros documents in the specified content
     * 
     * @param defaultweb Default Web to use for relative path names
     * @param content Content to parse
     * @return ArrayList of document names
     */
    public List getIncludedMacros(String defaultweb, String content)
    {
        return xwiki.getIncludedMacros(defaultweb, content, getXWikiContext());
    }

    /**
     * returns true if xwiki.readonly is set in the configuration file
     * 
     * @return the value of xwiki.isReadOnly()
     * @see com.xpn.xwiki.XWiki
     */
    public boolean isReadOnly()
    {
        return xwiki.isReadOnly();
    }

    /**
     * Priviledged API to set/unset the readonly status of the Wiki After setting this to true no
     * writing to the database will be performed All Edit buttons will be removed and save actions
     * disabled This is used for maintenance purposes
     * 
     * @param ro true to set read-only mode/false to unset
     */
    public void setReadOnly(boolean ro)
    {
        if (hasAdminRights()) {
            xwiki.setReadOnly(ro);
        }
    }

    /**
     * Priviledge API to regenerate the links/backlinks table Normally links and backlinks are
     * stored when a page is modified This function will regenerate all the backlinks This function
     * can be long to run
     * 
     * @throws XWikiException exception if the generation fails
     */
    public void refreshLinks() throws XWikiException
    {
        if (hasAdminRights()) {
            xwiki.refreshLinks(getXWikiContext());
        }
    }

    /**
     * API to check if the backlinks feature is active
     * Backlinks are activated in xwiki.cfg or in the XWiki Preferences
     * @return true if the backlinks feature is active
     * @throws XWikiException exception if the preference could not be retrieved
     */
    public boolean hasBacklinks() throws XWikiException
    {
        return xwiki.hasBacklinks(getXWikiContext());
    }

    /**
     * API to check if the tags feature is active.
     * Tags are activated in xwiki.cfg or in the XWiki Preferences
     * @return true if the tags feature is active, false otherwise
     * @throws XWikiException exception if the preference could not be retrieved
     */
    public boolean hasTags() throws XWikiException
    {
        return xwiki.hasTags(getXWikiContext());
    }


    /**
     * API to check if the edit comment feature is active
     * Edit comments are activated in xwiki.cfg or in the XWiki Preferences
     * @return
     */
    public boolean hasEditComment()
    {
        return xwiki.hasEditComment(context);
    }

    /**
     * API to check if the edit comment field is shown in the edit form
     * Edit comments are activated in xwiki.cfg or in the XWiki Preferences
     * @return
     */
    public boolean isEditCommentFieldHidden()
    {
        return xwiki.isEditCommentFieldHidden(context);
    }

    /**
     * API to check if the edit comment is suggested (prompted once by Javascript if empty)
     * Edit comments are activated in xwiki.cfg or in the XWiki Preferences
     * @return
     */
    public boolean isEditCommentSuggested()
    {
        return xwiki.isEditCommentSuggested(context);
    }

    /**
     * API to check if the edit comment is mandatory (prompted by Javascript if empty)
     * Edit comments are activated in xwiki.cfg or in the XWiki Preferences
     * @return
     */
    public boolean isEditCommentMandatory()
    {
        return xwiki.isEditCommentMandatory(context);
    }

    /**
     * API to rename a page (experimental) Rights are necessary to edit the source and target page
     * All objects and attachments ID are modified in the process to link to the new page name
     * 
     * @param doc page to rename
     * @param newFullName target page name to move the information to
     * @throws XWikiException exception if the rename fails
     */
    public boolean renamePage(Document doc, String newFullName)
    {
        try {
            if (xwiki.exists(newFullName, getXWikiContext())
                && !xwiki.getRightService().hasAccessLevel("delete", getXWikiContext().getUser(),
                    newFullName, getXWikiContext()))
                return false;
            if (xwiki.getRightService().hasAccessLevel("edit", getXWikiContext().getUser(),
                doc.getFullName(), getXWikiContext())) {
                xwiki.renamePage(doc.getFullName(), newFullName, getXWikiContext());
            }
        } catch (XWikiException e) {
            return false;
        }
        return true;
    }

    /**
     * Retrieves the current editor preference for the request The preference is first looked up in
     * the user preference and then in the space and wiki preference
     * 
     * @return "wysiwyg" or "text"
     */
    public String getEditorPreference()
    {
        return xwiki.getEditorPreference(getXWikiContext());
    }

    /**
     * Priviledged API to retrieve an object instanciated from groovy code in a String Groovy
     * scripts compilation is cached
     * 
     * @param script script containing a Groovy class definition (public class MyClass { ... })
     * @return An object instanciating this class
     * @throws XWikiException
     */
    public Object parseGroovyFromString(String script) throws XWikiException
    {
        if (hasProgrammingRights())
            return xwiki.parseGroovyFromString(script, getXWikiContext());
        return "groovy_missingrights";
    }

    /**
     * Priviledged API to retrieve an object instanciated from groovy code in a String Groovy
     * scripts compilation is cached
     * 
     * @param fullname // script containing a Groovy class definition (public class MyClass { ... })
     * @return An object instanciating this class
     * @throws XWikiException
     */
    public Object parseGroovyFromPage(String fullname) throws XWikiException
    {
        XWikiDocument doc = xwiki.getDocument(fullname, getXWikiContext());
        if (xwiki.getRightService().hasProgrammingRights(doc, getXWikiContext()))
            return xwiki.parseGroovyFromString(doc.getContent(), getXWikiContext());
        return "groovy_missingrights";
    }

    /**
     * API to get the macro list from the XWiki Preferences The macro list are the macros available
     * from the Macro Mapping System
     * 
     * @return String with each macro on each line
     */
    public String getMacroList()
    {
        return xwiki.getMacroList(getXWikiContext());
    }

    /**
     * API to check if using the style toolbar in Wysiwyg editor
     * 
     * @return a boolean value
     */
    public boolean useWysiwygStyleToolbar()
    {
        return xwiki.useWysiwygStyleToolbar(getXWikiContext());
    }

    /**
     * API to create an object from the request The parameters are the ones that are created from
     * doc.display("field","edit") calls
     * 
     * @param className XWiki Class Name to create the object from
     * @return a BaseObject wrapped in an Object
     * @throws XWikiException exception if the object could not be read
     */
    public com.xpn.xwiki.api.Object getObjectFromRequest(String className) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(xwiki.getObjectFromRequest(className,
            getXWikiContext()), getXWikiContext());
    }

    /**
     * API to create an empty document
     * 
     * @return an XWikiDocument wrapped in a Document
     */
    public Document createDocument()
    {
        return new XWikiDocument().newDocument(getXWikiContext());
    }

    /**
     * API to convert the username depending on the configuration The username can be converted from
     * email to a valid XWiki page name hidding the email address The username can be then used to
     * login and link to the right user page
     * 
     * @param username username to use for login
     * @return converted wiki page name for this username
     */
    public String convertUsername(String username)
    {
        return xwiki.convertUsername(username, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class This field
     * data can then be used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchColumns(String className, XWikiQuery query) throws XWikiException
    {
        return xwiki.displaySearchColumns(className, "", query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class, optionally
     * adding a prefix This field data can then be used to generate an XWiki Query showing a table
     * with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param prefix Prefix to add to the field name
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchColumns(String className, String prefix, XWikiQuery query)
        throws XWikiException
    {
        return xwiki.displaySearchColumns(className, prefix, query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class This field
     * data can then be used to generate the order element of an XWiki Query showing a table with the relevant data
     *
     * @param className XWiki Class Name to display the list of columns for
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchOrder(String className, XWikiQuery query) throws XWikiException
    {
        return xwiki.displaySearchOrder(className, "", query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class, optionally
     * adding a prefix This field data can then be used to generate the order element of an XWiki Query showing a table
     * with the relevant data
     *
     * @param className XWiki Class Name to display the list of columns for
     * @param prefix Prefix to add to the field name
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchOrder(String className, String prefix, XWikiQuery query)
        throws XWikiException
    {
        return xwiki.displaySearchOrder(className, prefix, query, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class without preselected values This
     * field data can then be used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearch(String fieldname, String className) throws XWikiException
    {
        return xwiki.displaySearch(fieldname, className, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class with preselected values This field
     * data can then be used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @param criteria XWikiCriteria object (usually the XWikiQuery object) to take the preselected
     *            values from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearch(String fieldname, String className, XWikiCriteria criteria)
        throws XWikiException
    {
        return xwiki.displaySearch(fieldname, className, criteria, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class with preselected values,
     * optionally adding a prefix to the field name This field data can then be used to generate an
     * XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @param prefix prefix to add to the field name
     * @param criteria XWikiCriteria object (usually the XWikiQuery object) to take the preselected
     *            values from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearch(String fieldname, String className, String prefix,
        XWikiCriteria criteria) throws XWikiException
    {
        return xwiki.displaySearch(fieldname, className, prefix, criteria, getXWikiContext());
    }

    /**
     * API to run a search from an XWikiQuery Object An XWikiQuery object can be created from a
     * request using the createQueryFromRequest function
     * 
     * @param query query to run the search for
     * @return A list of document names matching the query
     * @throws XWikiException exception is a failure occured
     */
    public List search(XWikiQuery query) throws XWikiException
    {
        return xwiki.search(query, getXWikiContext());
    }

    /**
     * API to create a query from a request Object The request object is the result of a form
     * created from the displaySearch() and displaySearchColumns() functions
     * 
     * @param className class name to create the query from
     * @return an XWikiQuery object matching the selected values in the request object
     * @throws XWikiException exception is a failure occured
     */
    public XWikiQuery createQueryFromRequest(String className) throws XWikiException
    {
        return xwiki.createQueryFromRequest(className, getXWikiContext());
    }

    /**
     * API to run a search from an XWikiQuery Object and display it as a HTML table An XWikiQuery
     * object can be created from a request using the createQueryFromRequest function
     * 
     * @param query query to run the search for
     * @return An HTML table showing the result
     * @throws XWikiException exception is a failure occured
     */
    public String searchAsTable(XWikiQuery query) throws XWikiException
    {
        return xwiki.searchAsTable(query, getXWikiContext());
    }

    /**
     * API to get the Property object from a class based on a property path A property path looks
     * like XWiki.ArticleClass_fieldname
     * 
     * @param propPath Property path
     * @return a PropertyClass object from a BaseClass object
     */
    public com.xpn.xwiki.api.PropertyClass getPropertyClassFromName(String propPath)
    {
        return new PropertyClass(xwiki.getPropertyClassFromName(propPath, getXWikiContext()),
            getXWikiContext());
    }

    /**
     * Generates a unique page name based on initial page name and already existing pages
     * 
     * @param name
     * @return a unique page name
     */
    public String getUniquePageName(String name)
    {
        return xwiki.getUniquePageName(name, getXWikiContext());
    }

    /**
     * Generates a unique page name based on initial page name and already existing pages
     * 
     * @param space
     * @param name
     * @return a unique page name
     */
    public String getUniquePageName(String space, String name)
    {
        return xwiki.getUniquePageName(space, name, getXWikiContext());
    }

    /**
     * Cleans up the page name to make it valid
     * 
     * @param name
     * @return A valid page name
     */
    public String clearName(String name)
    {
        return xwiki.clearName(name, getXWikiContext());
    }

    /**
     * Inserts a tooltip using toolTip.js
     * 
     * @param html HTML viewed
     * @param message HTML Tooltip message
     * @param params Parameters in Javascropt added to the tooltip config
     * @return HTML with working tooltip
     */
    public String addTooltip(String html, String message, String params)
    {
        return xwiki.addTooltip(html, message, params, getXWikiContext());
    }

    /**
     * Inserts a tooltip using toolTip.js
     * 
     * @param html HTML viewed
     * @param message HTML Tooltip message
     * @return HTML with working tooltip
     */
    public String addTooltip(String html, String message)
    {
        return xwiki.addTooltip(html, message, getXWikiContext());
    }

    /**
     * Inserts the tooltip Javascript
     * 
     * @return
     */
    public String addTooltipJS()
    {
        return xwiki.addTooltipJS(getXWikiContext());
    }

    /*
     * Inserts a Mandatory asterix
     */
    public String addMandatory()
    {
        return xwiki.addMandatory(getXWikiContext());
    }

    /*
     * Clear accents
     */
    public String clearAccents(String text) {
        return Util.noaccents(text);
    }

    /**
     * Get the XWiki Class object defined in the passed Document name.
     *
     * <p>Note: This method doesn't require any rights for accessing the passed Document (as
     *          opposed to the {@link com.xpn.xwiki.api.Document#getxWikiClass()} method which
     *          does require to get a Document object first. This is thus useful in cases where
     *          the calling code doesn't have the access right to the specified Document. It is
     *          safe because there are no sensitive data stored in a Class definition.
     * </p>
     * 
     * @param documentName the name of the document for which to get the Class object.
     *        For example "XWiki.XWikiPreferences"
     * @return the XWiki Class object defined in the passed Document name. If the passed Document
     *         name points to a Document with no Class defined then an empty Class object is
     *         returned (i.e. a Class object with no properties).
     * @throws XWikiException if the passed document name doesn't point to a valid Document
     */
    public Class getClass(String documentName) throws XWikiException
    {
        // TODO: The implementation should be done in com.xpn.xwiki.XWiki as this class should
        // delegate all implementations to that Class.
        return new Class(xwiki.getDocument(documentName, context).getxWikiClass(), context);
    }


    /**
     * Provides an absolute counter
     * @param name Counter name
     * @return  String
     */
    public String getCounter(String name) {
        XWikiEngineContext econtext = context.getEngineContext();
        Integer counter = (Integer) econtext.getAttribute(name);
        if (counter==null) {
           counter = new Integer(0);
        }
        counter = new Integer(counter.intValue() + 1);
        econtext.setAttribute(name, counter);
        return counter.toString();
    }

    /**
     * Check authentication from request and set according persitent login information
     * If it fails user is unlogged
     * @return  null if failed, non null XWikiUser if sucess
     * @throws XWikiException
     */
    public XWikiUser checkAuth() throws XWikiException
    {
        return context.getWiki().getAuthService().checkAuth(context);
    }

    /**
     * Check authentication from username and password and set according persitent login information
     * If it fails user is unlogged
     * @param username  username to check
     * @param password  password to check
     * @param rememberme "1" if you want to remember the login accross navigator restart
     * @return  null if failed, non null XWikiUser if sucess
     * @throws XWikiException
     */
    public XWikiUser checkAuth(String username, String password, String rememberme) throws XWikiException
    {
        return context.getWiki().getAuthService().checkAuth(username, password, rememberme, context);
    }

    /**
     * Add a and b because velocity operations are not always working
     * @param a
     * @param b
     * @return a+b
     */
    public int add(int a, int b) {
        return a+b;
    }

    /**
     * Add a and b because velocity operations are not working with longs
     * @param a
     * @param b
     * @return a+b
     */
    public long add(long a, long b) {
        return a+b;
    }

    /**
     * Add a and b because velocity operations are not working with longs
     * @param a
     * @param b
     * @return a+b
     */
    public String add(String a, String b) {
        long c = Long.parseLong(a) + Long.parseLong(b);
        return "" + c;
    }
}
