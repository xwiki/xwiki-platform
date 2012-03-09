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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerMessageTool;
import com.xpn.xwiki.web.Utils;

/**
 * This class manage wiki document descriptor.
 * 
 * @version $Id$
 */
// TODO: Future: XA2: create a Wiki interface to implement here and rename.
public class Wiki extends Document
{
    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(Wiki.class);

    /**
     * Create instance of wiki descriptor.
     * 
     * @param xdoc the encapsulated XWikiDocument.
     * @param context the XWiki context.
     * @throws XWikiException error when creating {@link Document}.
     */
    public Wiki(XWikiDocument xdoc, XWikiContext context) throws XWikiException
    {
        super(xdoc, context);
    }

    /**
     * @return the name of the wiki.
     * @throws XWikiException error when getting {@link XWikiServerClass} instance.
     */
    public String getWikiName() throws XWikiException
    {
        return XWikiServerClass.getInstance(this.context).getItemDefaultName(getFullName());
    }

    /**
     * Delete the wiki.
     * 
     * @param deleteDatabase if true wiki's database is also removed.
     * @throws XWikiException error deleting the wiki.
     * @since 1.1
     */
    public void delete(boolean deleteDatabase) throws XWikiException
    {
        String wikiName = getWikiName();

        if (wikiName.equals(this.context.getMainXWiki())) {
            throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, WikiManagerMessageTool.getDefault(
                this.context).get(WikiManagerMessageTool.ERROR_DELETEMAINWIKI, wikiName));
        }

        if (deleteDatabase) {
            if (hasAdminRights()) {
                try {
                    this.context.getWiki().getStore().deleteWiki(wikiName, this.context);
                } catch (XWikiException e) {
                    LOGGER.error("Failed to delete wiki from database", e);
                }
            } else {
                throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, WikiManagerMessageTool
                    .getDefault(this.context).get(WikiManagerMessageTool.ERROR_RIGHTTODELETEWIKI, wikiName));
            }
        }

        super.delete();

        Utils.getComponent(ObservationManager.class).notify(new WikiDeletedEvent(wikiName), wikiName, this.context);
    }

    /**
     * @return the number of wiki aliases in this wiki.
     * @throws XWikiException when getting the number of wiki aliases.
     * @since 1.1
     */
    public int countWikiAliases() throws XWikiException
    {
        List<Object> objects = getObjects(XWikiServerClass.getInstance(this.context).getClassFullName());

        int nb = 0;
        for (Iterator<Object> it = objects.iterator(); it.hasNext();) {
            if (it.next() != null) {
                ++nb;
            }
        }

        return nb;
    }

    /**
     * Get wiki alias id from domain name.
     * 
     * @param domain the wiki alias domain name.
     * @return the wiki alias id.
     * @throws XWikiException error when getting wiki alias id from domain name.
     * @since 1.1
     */
    public int getWikiAliasIdFromDomain(String domain) throws XWikiException
    {
        Collection<BaseObject> objects =
            this.doc.getObjects(XWikiServerClass.getInstance(this.context).getClassFullName());

        for (Iterator<BaseObject> it = objects.iterator(); it.hasNext();) {
            BaseObject bobect = it.next();

            if (bobect != null && bobect.getStringValue(XWikiServerClass.FIELD_SERVER).equals(domain)) {
                return bobect.getNumber();
            }
        }

        throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIALIASDOESNOTEXISTS, WikiManagerMessageTool
            .getDefault(this.context).get(WikiManagerMessageTool.ERROR_WIKIALIASDOESNOTEXISTS,
                getWikiName() + " - " + domain));
    }

    /**
     * @return the list of aliases to of this wiki.
     * @throws XWikiException error when getting aliases.
     */
    public Collection<XWikiServer> getWikiAliasList() throws XWikiException
    {
        return XWikiServerClass.getInstance(this.context).newXObjectDocumentList(this.doc, context);
    }

    /**
     * Get wiki alias with provided domain name.
     * 
     * @param domain the domain name of the wiki alias.
     * @return a wiki alias.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting {@link XWikiServerClass} instance</li>
     *             <li>or creating wiki alias object.</li>
     *             </ul>
     */
    public XWikiServer getWikiAlias(String domain) throws XWikiException
    {
        int id = getWikiAliasIdFromDomain(domain);

        return getWikiAlias(id);
    }

    /**
     * Get wiki alias with provided id.
     * 
     * @param id the id of the wiki alias.
     * @return an wiki alias.
     * @throws XWikiException error when creating wiki alias object.
     */
    public XWikiServer getWikiAlias(int id) throws XWikiException
    {
        return XWikiServerClass.getInstance(this.context).newXObjectDocument(this.doc, id, this.context);
    }

    /**
     * @return the first wiki alias used to describe the wiki itself.
     * @throws XWikiException error when creating wiki alias object.
     */
    public XWikiServer getFirstWikiAlias() throws XWikiException
    {
        Collection<BaseObject> objects =
            this.doc.getObjects(XWikiServerClass.getInstance(this.context).getClassFullName());

        return objects != null && objects.size() > 0 ? getWikiAlias(objects.iterator().next().getNumber()) : null;
    }

    /**
     * Check if a wiki alias with provided name exists.
     * 
     * @param domain the domain name of the wiki alias.
     * @return true if the wiki alias with provided domain name exists, false otherwise or if there is any error.
     * @since 1.1
     */
    public boolean containsWikiAlias(String domain)
    {
        boolean contains = false;

        try {
            getWikiAliasIdFromDomain(domain);
            contains = true;
        } catch (XWikiException e) {
            //
        }

        return contains;
    }

    @Override
    public String toString()
    {
        try {
            return getWikiName();
        } catch (XWikiException e) {
            return super.toString();
        }
    }
}
