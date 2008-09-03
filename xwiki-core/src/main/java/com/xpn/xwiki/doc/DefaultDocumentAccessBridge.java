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
package com.xpn.xwiki.doc;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Exposes methods for accessing Document data. This is temporary until we remodel the Model classes and the Document
 * services. The implementation is inside the old core, and not in a component because it has dependencies on the old
 * core.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class DefaultDocumentAccessBridge implements DocumentAccessBridge
{
    /** Execution context handler, needed for accessing the XWikiContext. */
    private Execution execution;

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String)
     */
    public String getDocumentContent(String documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentName, xcontext).getContent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getDocumentContent(String, String)
     */
    public String getDocumentContent(String documentName, String language) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentName, xcontext).getTranslatedContent(language, xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#exists(String)
     */
    public boolean exists(String documentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
        return !doc.isNew();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, int, String)
     */
    public String getProperty(String documentName, String className, int objectNumber, String propertyName)
        throws Exception
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getObject(className,
                objectNumber).get(propertyName)).getValue()
                + "";
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String, String)
     */
    public String getProperty(String documentName, String className, String propertyName) throws Exception
    {
        try {
            XWikiContext xcontext = getContext();

            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getObject(className).get(
                propertyName)).getValue()
                + "";
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getProperty(String, String)
     */
    public String getProperty(String documentName, String propertyName) throws Exception
    {
        try {
            XWikiContext xcontext = getContext();
            return ((BaseProperty) xcontext.getWiki().getDocument(documentName, xcontext).getFirstObject(propertyName,
                xcontext).get(propertyName)).getValue()
                + "";
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getPropertyType(String, String)
     */
    public String getPropertyType(String className, String propertyName) throws Exception
    {
        XWikiContext xcontext = getContext();
        PropertyClass pc = xcontext.getWiki().getPropertyClassFromName(className+"_"+propertyName, xcontext);
        return pc.newProperty().getClass().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getAttachmentContent(String, String)
     */
    public byte[] getAttachmentContent(String documentName, String attachmentName) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(documentName, xcontext).getAttachment(attachmentName)
            .getContent(xcontext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentAccessBridge#getURL(String, String, String, String)
     */
    public String getURL(String documentName, String action, String queryString, String anchor) throws Exception
    {
        XWikiContext xcontext = getContext();
        return xcontext.getWiki().getDocument(
            documentName == null && anchor != null ? xcontext.getDoc().getFullName() : documentName, xcontext).getURL(
            action, queryString, anchor, xcontext);
    }
}
