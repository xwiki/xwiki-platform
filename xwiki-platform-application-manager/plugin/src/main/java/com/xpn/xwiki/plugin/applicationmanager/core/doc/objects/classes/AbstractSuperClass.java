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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Abstract implementation of SuperClass.
 * <p>
 * This class has to be extended with at least :
 * <ul>
 * <li>overload {@link #updateBaseClass(BaseClass)}
 * <li>in constructor call AbstractSuperClass constructor with a name that will be used to generate
 * all the documents and spaces needed.
 * </ul>
 * 
 * @version $Id: $
 * @see SuperClass
 * @todo See http://jira.xwiki.org/jira/browse/XWIKI-1571. When that issue is applied in XWiki Core
 *       and when this plugin moves to the version of XWiki Core where it was applied then remove
 *       this class.
 */
public abstract class AbstractSuperClass implements SuperClass
{
    /**
     * FullName of the default parent page for a document containing xwiki class.
     */
    private static final String DEFAULT_XWIKICLASS_PARENT = "XWiki.XWikiClasses";

    /**
     * The resource file extension containing pages contents.
     */
    private static final String DOCUMENTCONTENT_EXT = ".svn";

    /**
     * Resource path prefix for the class sheets documents content.
     */
    private static final String DOCUMENTCONTENT_SHEET_PREFIX = "sheets/";

    /**
     * Resource path prefix for the class templates documents content.
     */
    private static final String DOCUMENTCONTENT_TEMPLATE_PREFIX = "templates/";

    /**
     * String used to protect value in HQL request.
     */
    private static final String HQL_PROTECT_STRING = "'";

    /**
     * String that replace {@link #HQL_PROTECT_STRING} in value strings.
     */
    private static final String HQL_I_PROTECT_STRING = "\\'";

    /**
     * Space prefix of class document.
     * 
     * @see #getClassSpace()
     */
    private final String classSpacePrefix;

    /**
     * Prefix of class document.
     * 
     * @see #getClassPrefix()
     */
    private final String classPrefix;

    /**
     * Space of class document.
     * 
     * @see #getClassSpace()
     */
    private final String classSpace;

    /**
     * Name of class document.
     * 
     * @see #getClassName()
     */
    private final String className;

    /**
     * Full name of class document.
     * 
     * @see #getClassFullName()
     */
    private final String classFullName;

    /**
     * Space of class sheet document.
     * 
     * @see #getClassSpace()
     */
    private final String classSheetSpace;

    /**
     * Name of class sheet document.
     * 
     * @see #getClassSheetName()
     */
    private final String classSheetName;

    /**
     * Full name of class sheet document.
     * 
     * @see #getClassSheetFullName()
     */
    private final String classSheetFullName;

    /**
     * Space of class template document.
     * 
     * @see #getClassSpace()
     */
    private final String classTemplateSpace;

    /**
     * Name of class template document.
     * 
     * @see #getClassTemplateName()
     */
    private final String classTemplateName;

    /**
     * Full name of class template document.
     * 
     * @see #getClassTemplateFullName()
     */
    private final String classTemplateFullName;

    /**
     * Default content of class template document.
     */
    private final String classSheetDefaultContent;

    /**
     * Default content of class sheet document.
     */
    private final String classTemplateDefaultContent;

    /**
     * Base class managed.
     */
    private BaseClass baseClass;

    /**
     * Store for any database name if documents used for manage this class has been checked.
     */
    private final Set databasesInitedMap = new HashSet();

    /**
     * Constructor for AbstractSuperClass.
     * 
     * @param prefix the prefix of class document.
     * @see #AbstractSuperClass(String, String)
     * @see #AbstractSuperClass(String, String, boolean)
     */
    protected AbstractSuperClass(String prefix)
    {
        this(XWIKI_CLASS_SPACE_PREFIX, prefix);
    }

    /**
     * Constructor for AbstractSuperClass.
     * 
     * @param spaceprefix the space prefix of class document.
     * @param prefix the prefix of class document.
     * @see #AbstractSuperClass(String)
     * @see #AbstractSuperClass(String, String, boolean)
     */
    protected AbstractSuperClass(String spaceprefix, String prefix)
    {
        this(spaceprefix, prefix, true);
    }

    /**
     * Constructor for AbstractSuperClass.
     * 
     * @param spaceprefix the space of class document.
     * @param prefix the prefix of class document.
     * @param dispatch Indicate if it had to use standard XWiki applications space names.
     * @see #AbstractSuperClass(String)
     * @see #AbstractSuperClass(String, String)
     */
    protected AbstractSuperClass(String spaceprefix, String prefix, boolean dispatch)
    {
        classSpacePrefix = spaceprefix;
        classPrefix = prefix;

        classSpace = dispatch ? classSpacePrefix + XWIKI_CLASS_SPACE_SUFFIX : classSpacePrefix;
        className = classPrefix + XWIKI_CLASS_SUFFIX;
        classFullName = classSpace + SuperDocument.SPACE_DOC_SEPARATOR + className;

        classSheetSpace =
            dispatch ? classSpacePrefix + XWIKI_CLASSSHEET_SPACE_SUFFIX : classSpacePrefix;
        classSheetName = classPrefix + XWIKI_CLASSSHEET_SUFFIX;
        classSheetFullName = classSheetSpace + SuperDocument.SPACE_DOC_SEPARATOR + classSheetName;

        classTemplateSpace =
            dispatch ? classSpacePrefix + XWIKI_CLASSTEMPLATE_SPACE_SUFFIX : classSpacePrefix;
        classTemplateName = classPrefix + XWIKI_CLASSTEMPLATE_SUFFIX;
        classTemplateFullName =
            classTemplateSpace + SuperDocument.SPACE_DOC_SEPARATOR + classTemplateName;

        classSheetDefaultContent =
            "## you can modify this page to customize the presentation of your object\n\n"
                + "1 Document $doc.name\n\n#set($class = $doc.getObject(\"" + classFullName
                + "\").xWikiClass)\n" + "\n" + "<dl>\n"
                + "  #foreach($prop in $class.properties)\n"
                + "    <dt> ${prop.prettyName} </dt>\n"
                + "    <dd>$doc.display($prop.getName())</dd>\n  #end\n" + "</dl>\n";

        classTemplateDefaultContent = "#includeForm(\"" + classSheetFullName + "\")\n";
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#getClassSpacePrefix()
     */
    public String getClassSpacePrefix()
    {
        return classSpacePrefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassSpace()
     */
    public String getClassSpace()
    {
        return classSpace;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassPrefix()
     */
    public String getClassPrefix()
    {
        return classPrefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassName()
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassFullName()
     */
    public String getClassFullName()
    {
        return classFullName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassTemplateName()
     */
    public String getClassTemplateSpace()
    {
        return classTemplateSpace;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassTemplateName()
     */
    public String getClassTemplateName()
    {
        return classTemplateName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassTemplateFullName()
     */
    public String getClassTemplateFullName()
    {
        return classTemplateFullName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassSheetName()
     */
    public String getClassSheetSpace()
    {
        return classSheetSpace;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassSheetName()
     */
    public String getClassSheetName()
    {
        return classSheetName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassSheetFullName()
     */
    public String getClassSheetFullName()
    {
        return classSheetFullName;
    }

    /**
     * Check if all necessary documents for manage this class in this context exists and update.
     * Create if not exists. Thread safe.
     * 
     * @param context the XWiki context.
     * @throws XWikiException error when saving documents.
     * @see #checkClassDocument(XWikiContext)
     */
    protected void check(XWikiContext context) throws XWikiException
    {
        synchronized (databasesInitedMap) {
            if (!this.databasesInitedMap.contains(context.getDatabase())) {
                checkClassDocument(context);
                checkClassSheetDocument(context);
                checkClassTemplateDocument(context);

                this.databasesInitedMap.add(context.getDatabase());
            }
        }
    }

    /**
     * Check if class document exists in this context and update. Create if not exists.
     * 
     * @param context the XWiki context.
     * @throws XWikiException error when saving document.
     */
    private void checkClassDocument(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(getClassFullName(), context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace(getClassSpace());
            doc.setName(getClassName());
            doc.setParent(DEFAULT_XWIKICLASS_PARENT);
            needsUpdate = true;
        }

        this.baseClass = doc.getxWikiClass();

        needsUpdate |= updateBaseClass(this.baseClass);

        if (doc.isNew() || needsUpdate) {
            xwiki.saveDocument(doc, context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassSheetDefaultContent()
     */
    public String getClassSheetDefaultContent()
    {
        return classSheetDefaultContent;
    }

    /**
     * Load an entire resource text file into {@link String}.
     * 
     * @param path the path to the resource file.
     * @return the entire content of the resource text file.
     */
    private String getResourceDocumentContent(String path)
    {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(path);

        if (in != null) {
            try {
                StringBuffer content = new StringBuffer(in.available());

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                for (String str = reader.readLine(); str != null; str = reader.readLine()) {
                    content.append(str);
                    content.append('\n');
                }

                return content.toString();
            } catch (IOException e) {
                // No resource file as been found or there is a problem when read it.
            }
        }

        return null;
    }

    /**
     * Check if class sheet document exists in this context and update. Create if not exists.
     * 
     * @param context the XWiki context.
     * @throws XWikiException error when saving document.
     */
    private void checkClassSheetDocument(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(getClassSheetFullName(), context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace(getClassSheetSpace());
            doc.setName(getClassSheetName());
            doc.setParent(getClassFullName());
            needsUpdate = true;
        }

        if (doc.isNew()) {
            String content =
                getResourceDocumentContent(DOCUMENTCONTENT_SHEET_PREFIX + getClassSheetFullName()
                    + DOCUMENTCONTENT_EXT);
            doc.setContent(content != null ? content : getClassSheetDefaultContent());
        }

        if (doc.isNew() || needsUpdate) {
            xwiki.saveDocument(doc, context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassTemplateDefaultContent()
     */
    public String getClassTemplateDefaultContent()
    {
        return classTemplateDefaultContent;
    }

    /**
     * Check if class template document exists in this context and update. Create if not exists.
     * 
     * @param context the XWiki context.
     * @throws XWikiException error when saving document.
     */
    private void checkClassTemplateDocument(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(getClassTemplateFullName(), context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace(getClassTemplateSpace());
            doc.setName(getClassTemplateName());
            needsUpdate = true;
        }

        if (doc.getObject(getClassFullName()) == null) {
            doc.createNewObject(getClassFullName(), context);

            needsUpdate = true;
        }

        if (doc.isNew()) {
            String content =
                getResourceDocumentContent(DOCUMENTCONTENT_TEMPLATE_PREFIX
                    + getClassTemplateFullName() + DOCUMENTCONTENT_EXT);
            doc.setContent(content != null ? content : getClassTemplateDefaultContent());

            doc.setParent(getClassFullName());
        }

        needsUpdate |= updateClassTemplateDocument(doc);

        if (doc.isNew() || needsUpdate) {
            xwiki.saveDocument(doc, context);
        }
    }

    /**
     * Initialize template document with default content.
     * 
     * @param doc the class template document that will be saved.
     * @return true if <code>doc</code> modified.
     */
    protected boolean updateClassTemplateDocument(XWikiDocument doc)
    {
        return false;
    }

    /**
     * Configure BaseClass.
     * 
     * @param baseClass the baseClass to configure.
     * @return true if <code>baseClass</code> modified.
     */
    protected boolean updateBaseClass(BaseClass baseClass)
    {
        boolean needUpdate = false;

        if (!baseClass.getName().equals(getClassFullName())) {
            baseClass.setName(getClassFullName());
            needUpdate = true;
        }

        return needUpdate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getBaseClass()
     */
    public BaseClass getBaseClass()
    {
        if (this.baseClass == null) {
            this.baseClass = new BaseClass();
            updateBaseClass(this.baseClass);
        }

        return this.baseClass;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassDocument(com.xpn.xwiki.XWikiContext)
     */
    public XWikiDocument getClassDocument(XWikiContext context) throws XWikiException
    {
        check(context);

        return context.getWiki().getDocument(getClassFullName(), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassSheetDocument(com.xpn.xwiki.XWikiContext)
     */
    public XWikiDocument getClassSheetDocument(XWikiContext context) throws XWikiException
    {
        check(context);

        return context.getWiki().getDocument(getClassSheetFullName(), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getClassTemplateDocument(com.xpn.xwiki.XWikiContext)
     */
    public XWikiDocument getClassTemplateDocument(XWikiContext context) throws XWikiException
    {
        check(context);

        return context.getWiki().getDocument(getClassTemplateFullName(), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#isInstance(com.xpn.xwiki.doc.XWikiDocument)
     */
    public boolean isInstance(XWikiDocument doc)
    {
        return doc.getObject(getClassFullName()) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getItemDocumentDefaultName(java.lang.String, XWikiContext)
     */
    public String getItemDocumentDefaultName(String itemName, XWikiContext context)
    {
        String name = context.getWiki().clearName(itemName, true, true, context);

        return getClassPrefix() + name.substring(0, 1).toUpperCase()
            + name.substring(1).toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getItemDocumentDefaultFullName(java.lang.String, XWikiContext)
     */
    public String getItemDocumentDefaultFullName(String itemName, XWikiContext context)
    {
        return getClassSpacePrefix() + SuperDocument.SPACE_DOC_SEPARATOR
            + getItemDocumentDefaultName(itemName, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#getItemDefaultName(java.lang.String)
     */
    public String getItemDefaultName(String docFullName)
    {
        return docFullName.substring(
            (getClassSpacePrefix() + SuperDocument.SPACE_DOC_SEPARATOR + getClassPrefix())
                .length()).toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#getItemDocument(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public XWikiDocument getItemDocument(String itemName, XWikiContext context)
        throws XWikiException
    {
        return context.getWiki().getDocument(getItemDocumentDefaultFullName(itemName, context),
            context);
    }

    /**
     * Find all XWikiDocument containing object of this XWiki class.
     * 
     * @param context the XWiki context.
     * @return a list of XWikiDocument containing object of this XWiki class.
     * @throws XWikiException error when searching for document in database.
     * @see #getClassFullName()
     */
    public List searchItemDocuments(XWikiContext context) throws XWikiException
    {
        return searchItemDocumentsByFields(null, null, context);
    }

    /**
     * Find XWikiDocument containing object of this XWiki class with provided full name. Difference
     * with {@link XWiki#getDocument(String, XWikiContext)} is that it will not return "new"
     * XWikiDocument.
     * 
     * @param docFullName the full name of the document.
     * @param context the XWiki context.
     * @return a list with just one XWikiDocument containing object of this XWiki class.
     * @throws XWikiException error when searching for document in database.
     */
    public List searchItemDocuments(String docFullName, XWikiContext context)
        throws XWikiException
    {
        return searchItemDocumentsByFields(docFullName, null, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#searchItemDocumentsByField(java.lang.String,
     *      java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public List searchItemDocumentsByField(String fieldName, String fieldValue, String fieldType,
        XWikiContext context) throws XWikiException
    {
        String[][] fieldDescriptors = new String[][] {{fieldName, fieldType, fieldValue}};

        return searchItemDocumentsByFields(null, fieldDescriptors, context);
    }

    /**
     * Protect a {@link String} value used in Hql request.
     * 
     * @param value the value to protect.
     * @return the protected version of <code>value</code>.
     */
    private String hqlProtectValue(String value)
    {
        return hqlProtectValue(value, false);
    }
    
    /**
     * Protect a {@link String} value used in Hql request.
     * 
     * @param value the value to protect.
     * @param toLower if true <code>toLower</code> is lowered.
     * @return the protected version of <code>value</code>.
     */
    private String hqlProtectValue(String value, boolean toLower)
    {
        String protectedValue = value.replace(HQL_PROTECT_STRING, HQL_I_PROTECT_STRING);

        if (toLower) {
            protectedValue = protectedValue.toLowerCase();
        }

        return HQL_PROTECT_STRING + protectedValue + HQL_PROTECT_STRING;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#searchItemDocumentsByFields(java.lang.String,
     *      java.lang.String[][], com.xpn.xwiki.XWikiContext)
     */
    public List searchItemDocumentsByFields(String docFullName, String[][] fieldDescriptors,
        XWikiContext context) throws XWikiException
    {
        check(context);

        StringBuffer from = new StringBuffer(", BaseObject as obj");

        StringBuffer where =
            new StringBuffer(" where doc.fullName=obj.name and obj.className="
                + hqlProtectValue(getClassFullName()));

        if (docFullName != null) {
            where.append(" and obj.name=" + hqlProtectValue(docFullName));
        } else {
            where.append(" and obj.name<>" + hqlProtectValue(getClassTemplateFullName()));
        }

        if (fieldDescriptors != null) {
            for (int i = 0; i < fieldDescriptors.length; ++i) {
                String fieldName = fieldDescriptors[i][0];
                String type = fieldDescriptors[i][1];
                String value = fieldDescriptors[i][2];

                if (type != null) {
                    String fieldPrefix = "field" + i;

                    from.append(", " + type + " as " + fieldPrefix);

                    String andSymbol = " and ";

                    where.append(andSymbol + "obj.id=" + fieldPrefix + ".id.id");

                    where.append(andSymbol + fieldPrefix + ".name="
                        + hqlProtectValue(fieldName));
                    where.append(andSymbol + "lower(" + fieldPrefix + ".value)="
                        + hqlProtectValue(value, true));
                } else {
                    where.append(" and lower(doc." + fieldName + ")="
                        + hqlProtectValue(value, true));
                }
            }
        }

        return context.getWiki().getStore().searchDocuments(from.append(where).toString(),
            context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#newSuperDocument(com.xpn.xwiki.doc.XWikiDocument,
     *      com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        return new DefaultSuperDocument(this, doc, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#newSuperDocument(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(String docFullName, XWikiContext context)
        throws XWikiException
    {
        return newSuperDocument(context.getWiki().getDocument(docFullName, context), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperClass#newSuperDocument(com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(XWikiContext context) throws XWikiException
    {
        return newSuperDocument(new XWikiDocument(), context);
    }
}
