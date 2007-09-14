/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors.
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

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * SuperDocument inteface.
 */
public interface SuperDocument
{   
    /**
     * Reload document.
     * 
     * @param context   Context.
     * 
     * @throws XWikiException
     */
    void reload(XWikiContext context) throws XWikiException;
    
    /**
     * Merge two documents BaseObject.
     * 
     * @param sdoc Document to merge.
     */
    void mergeBaseObject(SuperDocument sdoc);
       
    /**
     * Return super class.
     * 
     * @return SuperClass  Class manager for this document.
     */
    SuperClass getSuperClass();
      
    /**
     * Indicate if document already exists in database.
     *
     * @return boolean  True if this is a new document of this class (this document can exist but not for this class).
     */
    boolean isNew();

    /**
     * Return the document.
     * 
     * @return XWikiDocument The document.
     */
    XWikiDocument getDocument();
}
