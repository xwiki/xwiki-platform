package com.xpn.xwiki.plugin.wikimanager.doc;

import java.util.Collection;
import java.util.Iterator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This class manage wiki document descriptor.
 * 
 * @version $Id: $
 */
public class Wiki extends Document
{
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
        return XWikiServerClass.getInstance(context).getItemDefaultName(getFullName());
    }

    /**
     * @return the list of aliases to of this wiki.
     * @throws XWikiException error when getting aliases.
     */
    public Collection getWikiAliasList() throws XWikiException
    {
        return XWikiServerClass.getInstance(context).newSuperDocumentList(doc, context);
    }

    /**
     * Get wiki alias with provided domain name.
     * 
     * @param domain the domain name of the wiki alias.
     * @return an wiki alias.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting {@link XWikiServerClass} instance</li>
     *             <li>or creating wiki alias object.</li>
     *             </ul>
     */
    public XWikiServer getWikiAlias(String domain) throws XWikiException
    {
        Collection objects =
            doc.getObjects(XWikiServerClass.getInstance(context).getClassFullName());

        int id = 0;
        for (Iterator it = objects.iterator(); it.hasNext(); ++id) {
            BaseObject bobect = (BaseObject) it.next();

            if (bobect != null
                && bobect.getStringValue(XWikiServerClass.FIELD_SERVER).equals(domain)) {
                return getWikiAlias(id);
            }
        }

        return null;
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
        return (XWikiServer) XWikiServerClass.getInstance(context).newSuperDocument(doc, id,
            context);
    }
}
