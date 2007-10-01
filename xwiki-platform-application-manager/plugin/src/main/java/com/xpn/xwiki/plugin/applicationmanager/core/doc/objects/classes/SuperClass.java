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

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Interface that give way to manage a XWiki class :
 * <ul>
 * <li>assume that the XWiki class exist in the context we are working
 * <li>search in documents that contains this class with conditions on class fields
 * <li>support the XWiki norm about spaces and documents naming
 * </ul>
 * 
 * @todo See http://jira.xwiki.org/jira/browse/XWIKI-1571. When that issue is applied in XWiki Core
 *       and when this plugin moves to the version of XWiki Core where it was applied then remove
 *       this class.
 */
public interface SuperClass
{
    /**
     * Default suffix for the document containing the class.
     */
    static final String XWIKI_CLASS_SUFFIX = "Class";

    /**
     * Default suffix for a document containing a class sheet.
     */
    static final String XWIKI_CLASSSHEET_SUFFIX = "ClassSheet";

    /**
     * Default suffix for a document containing a class template.
     */
    static final String XWIKI_CLASSTEMPLATE_SUFFIX = "ClassTemplate";

    /**
     * Default prefix for a document's space containing a class.
     */
    static final String XWIKI_CLASS_SPACE_PREFIX = "XWiki";

    /**
     * Default suffix for a document's space containing a class.
     */
    static final String XWIKI_CLASS_SPACE_SUFFIX = "Classes";

    /**
     * Default suffix for a document's space containing a class sheet.
     */
    static final String XWIKI_CLASSSHEET_SPACE_SUFFIX = "Sheets";

    /**
     * Default suffix for a document's space containing a class template.
     */
    static final String XWIKI_CLASSTEMPLATE_SPACE_SUFFIX = "Templates";

    // ///

    /**
     * @return the space prefix name of the document containing the class.
     */
    String getClassSpacePrefix();

    /**
     * @return the prefix name of the document containing the class. Usually extracted from :
     *         ClassSpace.ClassPrefixXWIKI_CLASS_SUFFIX.
     * @see #getClassSpace()
     */
    String getClassPrefix();

    // Classes

    /**
     * @return the space name of the document containing the class. Usually class space is :
     *         SpacePrefixXWIKI_CLASS_SPACE_SUFFIX.
     * @see #getClassSpacePrefix()
     * @see #XWIKI_CLASS_SPACE_SUFFIX
     */
    String getClassSpace();

    /**
     * @return the name of the document containing the class. Usually class name is :
     *         ClassPrefixXWIKI_CLASS_SUFFIX.
     * @see #getClassFullName()
     * @see #getClassPrefix()
     * @see #XWIKI_CLASS_SUFFIX
     */
    String getClassName();

    /**
     * @return the full name of the document containing the class. Usually class full name is :
     *         ClassSpace.ClassName.
     * @see #getClassName()
     * @see #getClassSpace()
     */
    String getClassFullName();

    // Templates

    /**
     * @return the space name of the document containing the class template. Usually class template
     *         space name is : ClassSpacePrefixXWIKI_CLASSTEMPLATE_SPACE_SUFFIX.
     * @see #getClassPrefix()
     * @see #XWIKI_CLASSTEMPLATE_SPACE_SUFFIX
     */
    String getClassTemplateSpace();

    /**
     * @return the name of the document containing the class template. Usually class template name
     *         is : ClassPrefixClassXWIKI_CLASSTEMPLATE_SUFFIX.
     * @see #getClassPrefix()
     * @see #getClassTemplateFullName()
     * @see #XWIKI_CLASSTEMPLATE_SUFFIX
     */
    String getClassTemplateName();

    /**
     * @return the full name of the document containing the class template. Usually class template
     *         full name is : ClassTemplateSpace.ClassTemplateName.
     * @see #getClassTemplateSpace()
     * @see #getClassTemplateName()
     */
    String getClassTemplateFullName();

    // Sheets

    /**
     * @return the space name of the document containing the class sheet. Usually class sheet space
     *         name is : ClassSpacePrefixXWIKI_CLASSSHEET_SPACE_SUFFIX.
     * @see #getClassSpacePrefix()
     * @see #XWIKI_CLASSSHEET_SPACE_SUFFIX
     */
    String getClassSheetSpace();

    /**
     * @return the name of the document containing the class sheet. Usually class sheet name is :
     *         ClassPrefixXWIKI_CLASSSHEET_SUFFIX.
     * @see #getClassPrefix()
     * @see #getClassSheetFullName()
     * @see #XWIKI_CLASSSHEET_SUFFIX
     */
    String getClassSheetName();

    /**
     * @return the full name of the document containing the class sheet. Usually class sheet full
     *         name is : ClassSheetSpace.ClassSheetName.
     * @see #getClassSheetSpace()
     * @see #getClassSheetName()
     */
    String getClassSheetFullName();

    // ///

    /**
     * @return the BaseClass managed by this SuperClass.
     */
    BaseClass getBaseClass() throws XWikiException;

    /**
     * Get the document containing the class in this context's database.
     * 
     * @param context the XWiki context.
     * @return the document containing the class for this context.
     */
    XWikiDocument getClassDocument(XWikiContext context) throws XWikiException;

    /**
     * @return the default content to add in a new class sheet document.
     */
    String getClassSheetDefaultContent();

    /**
     * Get the document containing the class sheet for this context's database.
     * 
     * @param context the XWiki context.
     * @return the document containing the class sheet for this context.
     */
    XWikiDocument getClassSheetDocument(XWikiContext context) throws XWikiException;

    /**
     * @return the default content to add in a new class template document.
     */
    String getClassTemplateDefaultContent();

    /**
     * Get the document containing the class template for this context's database.
     * 
     * @param context the XWiki context.
     * @return the class template document for this context.
     */
    XWikiDocument getClassTemplateDocument(XWikiContext context) throws XWikiException;

    /**
     * Determines if the specified <code>doc</code> is compatible with this xwiki class (if he
     * contains class object).
     * 
     * @param doc the XWikidocument to test.
     * @param context the XWiki context.
     * @return true if <code>doc</code> support this class, false otherwise.
     * @throws XWikiException
     */
    boolean isInstance(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * Get document name from item name <code>item</code>. Usually a Document name is
     * DocumentTypeItemName.
     * 
     * @param itemName the name of the item to find.
     * @param context the XWiki context.
     * @return the name of the document.
     * @see #getItemDocumentDefaultFullName(String, XWikiContext)
     * @see #getItemDefaultName(String, XWikiContext)
     */
    String getItemDocumentDefaultName(String itemName, XWikiContext context);

    /**
     * Get document full name from item name <code>item</code>. Usually a Document full name is
     * Space.DocumentTypeItemName.
     * 
     * @param itemName the name of the item.
     * @param context the XWiki context.
     * @return the full name of the document.
     * @see #getItemDocumentDefaultName(String, XWikiContext)
     * @see #getItemDefaultName(String, XWikiContext)
     */
    String getItemDocumentDefaultFullName(String itemName, XWikiContext context);

    /**
     * Get item name extracted from document full name. Usually a Document full name is
     * Space.DocumentTypeItemName.
     * 
     * @param docFullName the full name of the document.
     * @param context the XWiki context.
     * @return the item name extracted from document name.
     * @see #getItemDocumentDefaultFullName(String, XWikiContext)
     */
    public String getItemDefaultName(String docFullName, XWikiContext context);

    /**
     * Get document by full name from item name <code>itemName</code>.
     * 
     * @param itemName the full name of the item.
     * @param context the XWiki context.
     * @return the full name of the document.
     * @throws XWikiException
     * @see #getItemDefaultName(String, XWikiContext)
     * @see #getItemDocumentDefaultFullName(String, XWikiContext)
     */
    XWikiDocument getItemDocument(String itemName, XWikiContext context) throws XWikiException;

    /**
     * Search in instances of this document class.
     * 
     * @param fieldName the name of field.
     * @param fieldValue the value of field.
     * @param fieldType the type of field.
     * @param context the XWiki context.
     * @return the list of found XWikiDocuments.
     * @throws XWikiException
     */
    List searchItemDocumentsByField(String fieldName, String fieldValue, String fieldType,
        XWikiContext context) throws XWikiException;

    /**
     * Search in instances of this document class.
     * 
     * @param docFullName the full name of the document. If Null, it is not consider.
     * @param fieldDescriptors the list of fields name/value constraints.
     * @param context the XWiki context.
     * @return the list of found XWikiDocuments.
     * @throws XWikiException
     */
    List searchItemDocumentsByFields(String docFullName, String[][] fieldDescriptors,
        XWikiContext context) throws XWikiException;

    /**
     * Create new super document containing object of class {@link #getClassFullName()}. If
     * document already exist it is returned with new object if it does not contains any.
     * 
     * @param doc the XWiki document to manage.
     * @param context the XWiki context.
     * @return a new SuperDocument instance.
     * @throws XWikiException
     */
    SuperDocument newSuperDocument(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * Create new super document containing object of class {@link #getClassFullName()}. If
     * document already exist it is returned with new object if it does not contains any.
     * 
     * @param context the XWiki context.
     * @return a new SuperDocument instance.
     * @throws XWikiException
     */
    SuperDocument newSuperDocument(XWikiContext context) throws XWikiException;

    /**
     * Create new super document containing object of class {@link #getClassFullName()}. If
     * document already exist it is returned with new object if it does not contains any.
     * 
     * @param docFullName the full name of document to manage.
     * @param context the XWiki context.
     * @return a new SuperDocument instance.
     * @throws XWikiException
     * @see #getClassFullName()
     */
    SuperDocument newSuperDocument(String docFullName, XWikiContext context)
        throws XWikiException;
}
