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
package com.xpn.xwiki.plugin.wikimanager.doc;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultXObjectDocument;

/**
 * This class manage an XWiki document containing XWiki.XWikiServerClass object. It add some specifics methods, getters
 * and setters for this type of object and fields.
 * 
 * @version $Id$
 */
public class XWikiServer extends DefaultXObjectDocument
{
    /**
     * Comma string.
     */
    private static final String COMMA = ",";

    /**
     * Create new XWikiServer managing provided XWikiDocument.
     * 
     * @param xdoc the encapsulated XWikiDocument.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting XWikiServerClass instance.</li>
     *             <li>or when calling {@link #reload(XWikiContext)}</li>
     *             </ul>
     */
    public XWikiServer(XWikiDocument xdoc, int objectId, XWikiContext context) throws XWikiException
    {
        super(XWikiServerClass.getInstance(context), xdoc, objectId, context);
    }

    /**
     * @return the name of the wiki.
     * @see #setWikiName(String)
     */
    public String getWikiName()
    {
        return sclass.getItemDefaultName(getFullName());
    }

    /**
     * Modify the name of the application.
     * 
     * @param wikiName the new name of the wiki.
     */
    public void setWikiName(String wikiName)
    {
        getDoc().setFullName(context.getMainXWiki() + ":" + sclass.getItemDocumentDefaultFullName(wikiName, context));
    }

    /**
     * @return the pretty name of the wiki.
     * @see #setWikiPrettyName(String)
     */
    public String getWikiPrettyName()
    {
        return getStringValue(XWikiServerClass.FIELD_WIKIPRETTYNAME);
    }

    /**
     * Modify the pretty name of the application.
     * 
     * @param wikiPrettyName the new name of the wiki.
     */
    public void setWikiPrettyName(String wikiPrettyName)
    {
        setStringValue(XWikiServerClass.FIELD_WIKIPRETTYNAME, wikiPrettyName);
    }

    /**
     * @return the name of the owner of the wiki.
     */
    public String getOwner()
    {
        return getStringValue(XWikiServerClass.FIELD_OWNER);
    }

    /**
     * Modify the owner of the wiki.
     * 
     * @param owner the new owner of the wiki.
     */
    public void setOwner(String owner)
    {
        setStringValue(XWikiServerClass.FIELD_OWNER, owner);
    }

    /**
     * @return the description od the wiki.
     */
    public String getDescription()
    {
        return getStringValue(XWikiServerClass.FIELD_DESCRIPTION);
    }

    /**
     * Modify the description of the wiki.
     * 
     * @param description the new description of the wiki.
     */
    public void setDescription(String description)
    {
        setStringValue(XWikiServerClass.FIELD_DESCRIPTION, description);
    }

    /**
     * @return the domain name of the wiki.
     */
    public String getServer()
    {
        return getStringValue(XWikiServerClass.FIELD_SERVER);
    }

    /**
     * Modify the domain name of the wiki.
     * 
     * @param server the new domain name of the wiki.
     */
    public void setServer(String server)
    {
        setStringValue(XWikiServerClass.FIELD_SERVER, server);
    }

    /**
     * @return the visibility of the wiki. Can be:
     *         <ul>
     *         <li>{@link XWikiServerClass#FIELDL_VISIBILITY_PUBLIC}</li>
     *         <li>{@link XWikiServerClass#FIELDL_VISIBILITY_PRIVATE}</li>
     *         </ul>
     */
    public String getVisibility()
    {
        return getStringValue(XWikiServerClass.FIELD_VISIBILITY);
    }

    /**
     * Modify the visibility of the wiki.
     * 
     * @param visibility the new visibility of the wiki. Can be:
     *            <ul>
     *            <li>{@link XWikiServerClass#FIELDL_VISIBILITY_PUBLIC}</li>
     *            <li>{@link XWikiServerClass#FIELDL_VISIBILITY_PRIVATE}</li>
     *            </ul>
     */
    public void setVisibility(String visibility)
    {
        setStringValue(XWikiServerClass.FIELD_VISIBILITY, visibility);
    }

    /**
     * @return the language of the wiki.
     */
    public String getWikiLanguage()
    {
        return getStringValue(XWikiServerClass.FIELD_LANGUAGE);
    }

    /**
     * Modify the language of the wiki.
     * 
     * @param language the new language of the wiki.
     */
    public void setWikiLanguage(String language)
    {
        setStringValue(XWikiServerClass.FIELD_LANGUAGE, language);
    }

    /**
     * @return the state of the wiki.
     */
    public String getState()
    {
        return getStringValue(XWikiServerClass.FIELD_STATE);
    }

    /**
     * Modify the state of the wiki.
     * 
     * @param state the new state of the wiki.
     */
    public void setState(String state)
    {
        setStringValue(XWikiServerClass.FIELD_STATE, state);
    }

    /**
     * @return true if wiki is in secure mode (https), false if in simple non-secure (http) mode.
     */
    public boolean getSecure()
    {
        Boolean secure = getBooleanValue(XWikiServerClass.FIELD_SECURE);

        return secure != null && secure.booleanValue();
    }

    /**
     * Modify the secure mode of the wiki.
     * 
     * @param secure true if wiki is in secure mode (https), false if in simple non-secure (http) mode.
     */
    public void setSecure(boolean secure)
    {
        setBooleanValue(XWikiServerClass.FIELD_SECURE, Boolean.valueOf(secure));
    }

    /**
     * @return the home page of the wiki.
     */
    public String getHomePage()
    {
        return getStringValue(XWikiServerClass.FIELD_HOMEPAGE);
    }

    /**
     * Modify the home page of the wiki.
     * 
     * @param homePage the new home page of the wiki.
     */
    public void setHomePage(String homePage)
    {
        setStringValue(XWikiServerClass.FIELD_HOMEPAGE, homePage);
    }

    /**
     * @param isWikiTemplate true if it's a wiki template, false otherwise.
     */
    public void setIsWikiTemplate(boolean isWikiTemplate)
    {
        setBooleanValue(XWikiServerClass.FIELD_ISWIKITEMPLATE, isWikiTemplate);
    }

    /**
     * @return true if it's a wiki template, false otherwise.
     */
    public boolean isWikiTemplate()
    {
        return getBooleanValue(XWikiServerClass.FIELD_ISWIKITEMPLATE);
    }

    @Override
    public String toString()
    {
        return getFullName() + COMMA + getServer() + COMMA + getOwner();
    }

    /**
     * @return the complete {@link URL} of the wiki.
     * @throws MalformedURLException error occurred when creating the {@link URL}.
     */
    public URL getServerUrl() throws MalformedURLException
    {
        return getServerUrl(null, null);
    }

    /**
     * @return the complete {@link URL} of the wiki home page.
     * @throws MalformedURLException error occurred when creating the {@link URL}.
     */
    public URL getHomePageUrl() throws MalformedURLException
    {
        return getWikiUrl(getHomePage());
    }

    /**
     * Get the complete {@link URL} of the provided wiki page.
     * 
     * @param pageFullName the page full name for which to get the complete {@link URL}.
     * @return the complete {@link URL} of the provided wiki page.
     * @throws MalformedURLException error occurred when creating the {@link URL}.
     */
    public URL getWikiUrl(String pageFullName) throws MalformedURLException
    {
        if (!StringUtils.isEmpty(pageFullName)) {
            XWikiDocument document = new XWikiDocument();
            document.setFullName(pageFullName);

            return getServerUrl(document.getSpace(), document.getName());
        } else {
            return getServerUrl(null, null);
        }
    }

    /**
     * Get the complete {@link URL} of the provided wiki page.
     * 
     * @param spaceName the space name of the page for which to get the complete {@link URL}.
     * @param pageName the name of the page for which to get the complete {@link URL}.
     * @return the complete {@link URL} of the provided wiki page.
     * @throws MalformedURLException error occurred when creating the {@link URL}.
     */
    public URL getServerUrl(String spaceName, String pageName) throws MalformedURLException
    {
        URL url;

        if (spaceName == null || pageName == null) {
            // TODO : implement {@link XWiki#getServerURL(String, XWikiContext)} here and use
            // WikiManager plugin from core.
            url = context.getWiki().getServerURL(getWikiName(), context);
        } else {
            url = context.getURLFactory().createURL(spaceName, pageName, "view", null, null, getWikiName(), context);
        }

        return url;
    }
}
