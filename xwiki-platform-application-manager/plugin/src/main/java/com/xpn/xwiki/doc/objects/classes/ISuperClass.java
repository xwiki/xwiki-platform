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

package com.xpn.xwiki.doc.objects.classes;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

public interface ISuperClass
{
    /**
     * Default class document suffix.
     */
    static final String XWIKI_CLASS_SUFFIX = "Class";

    /**
     * Default class sheet document suffix.
     */
    static final String XWIKI_CLASSSHEET_SUFFIX = "ClassSheet";
    
    /**
     * Default class template document suffix.
     */
    static final String XWIKI_CLASSTEMPLATE_SUFFIX = "ClassTemplate";
   
    /**
     * Default class document space.
     */
    static final String XWIKI_CLASS_SPACE_PREFIX = "XWiki";

    /**
     * Default class document space.
     */
    static final String XWIKI_CLASS_SPACE_SUFFIX = "Classes";
    
    /**
     * Default class document space.
     */
    static final String XWIKI_CLASSSHEET_SPACE_SUFFIX = "Sheets";
    
    /**
     * Default class document space.
     */
    static final String XWIKI_CLASSTEMPLATE_SPACE_SUFFIX = "Templates";
    
    // ///

    /**
     * @return String   Space prefix of class document.
     */
    String getClassSpacePrefix();
    
    /**
     * @return String   Space of class document.
     */
    String getClassSpace();

    /**
     * @return String   Prefix of class document.
     */
    String getClassPrefix();

    /**
     * @return String   Name of class document.
     */
    String getClassName();

    /**
     * @return String   Full name of class document.
     */
    String getClassFullName();

    /**
     * @return String   Name of class template document.
     */
    String getClassTemplateName();

    /**
     * @return String   Full name of class template document.
     */
    String getClassTemplateFullName();

    /**
     * @return String   Name of class sheet document.
     */
    String getClassSheetName();

    /**
     * @return String   Full name of class sheet document.
     */
    String getClassSheetFullName();

    /**
     * @return BaseClass   BaseClass object managed.
     */
    BaseClass getBaseClass() throws XWikiException;

    /**
     * Return super class document for this context.
     *
     * @param context           Context.
     * 
     * @return XWikiDocument   Class document for this context.
     */
    XWikiDocument getClassDocument(XWikiContext context) throws XWikiException;
    
    /**
     * Return super class sheet document content.
     * 
     * @return String   Default new class sheet document content.
     */
    String getClassSheetDefaultContent();

    /**
     * Return super class sheet document for this context.
     *
     * @param context           Context.
     *
     * @return XWikiDocument    Class sheet document for this context.
     */
    XWikiDocument getClassSheetDocument(XWikiContext context) throws XWikiException;

    /**
     * Return super class template document content.
     * 
     * @return String   Default new class template document content.
     */
    String getClassTemplateDefaultContent();
    
    /**
     * Return super class template document for this context.
     * 
     * @param context       Context.
     * 
     * @return XWikiDocument   Class template document for this context.
     */
    XWikiDocument getClassTemplateDocument(XWikiContext context) throws XWikiException;
        
    /**
     * Determines if the specified <code>doc</code> is compatible with this xwiki class (if he contains class object).
     * 
     * @param doc       XWikidocument to test.
     * @param context   Context.
     * 
     * @return boolean  True if <code>doc</code> support this class.
     * 
     * @throws XWikiException
     */
    boolean isInstance(XWikiDocument doc, XWikiContext context) throws XWikiException;
    
    /**
     * Get document name for item name <code>item</code>.
     * 
     * @param itemName  Name of the item.
     * @param context   Context.
     * 
     * @return String   Name of the document.
     */
    String getItemDocumentDefaultName(String itemName, XWikiContext context);
    
    /**
     * Get document full name for item name <code>itemName</code>.
     * 
     * @param itemName  Full name of the item.
     * @param context   Context.
     * 
     * @return String   Full name of the document.
     */
    String getItemDocumentDefaultFullName(String itemName, XWikiContext context);
    
    /**
     * Get item name extracted from document full name.
     * 
     * @param docFullName   Full name of the document.
     * @param context       Context.
     * 
     * @return String       Item Name.
     */
    public String getItemDefaultName(String docFullName, XWikiContext context);
    
    /**
     * Get document by full name for item name <code>itemName</code>.
     * 
     * @param itemName  Full name of the item.
     * @param context   Context.
     *
     * @return String   Full name of the document.
     * 
     * @throws XWikiException 
     */
    XWikiDocument getItemDocument(String itemName, XWikiContext context) throws XWikiException;
    
    /**
     * Search in instances of this document class.
     * @param fieldName     Name of field.
     * @param fieldValue    Value of field.
     * @param fieldType     Type of field.
     * @param context       Context.
     *
     * @return List         List of found XWikiDocuments.
     *
     * @throws XWikiException
     */
    List searchItemDocumentsByField(String fieldName, String fieldValue, String fieldType, XWikiContext context) throws XWikiException;

    /**
     * Search in instances of this document class.
     * 
     * @param docFullName       Full name of the document. If Null, it is not consider.
     * @param fieldDescriptors  List of fields name/value constraints.
     * @param context           Context.
     *
     * @return List             List of found XWikiDocuments.
     * 
     * @throws XWikiException
     */
    List searchItemDocumentsByFields(String docFullName, String[][] fieldDescriptors, XWikiContext context) throws XWikiException;

    /**
     * Create new super document.
     * 
     * @param doc               Document to encapsulate.
     * @param context           Context.
     *
     * @return ISuperDocument   Super document.
     * 
     * @throws XWikiException
     */
    ISuperDocument newSuperDocument(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * Create new empty super document.
     *
     * @param context           Context.
     * 
     * @return ISuperDocument   Super document.
     * 
     * @throws XWikiException
     */
    ISuperDocument newSuperDocument(XWikiContext context) throws XWikiException;

    /**
     * Create new super document.
     * 
     * @param docFullName       Full name of document to encapsulate.
     * @param context           Context.
     * 
     * @return ISuperDocument   Super document.
     * 
     * @throws XWikiException
     */
    ISuperDocument newSuperDocument(String docFullName, XWikiContext context) throws XWikiException;
}
