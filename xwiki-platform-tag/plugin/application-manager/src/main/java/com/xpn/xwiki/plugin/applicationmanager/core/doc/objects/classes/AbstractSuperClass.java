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

import java.util.HashSet;
import java.util.List;

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
 * </p>
 * 
 * @see SuperClass
 * @todo See http://jira.xwiki.org/jira/browse/XWIKI-1571. When that issue is applied in XWiki Core
 *       and when this plugin moves to the version of XWiki Core where it was applied then remove
 *       this class.
 */
public abstract class AbstractSuperClass implements SuperClass
{
    /**
     * Space prefix of class document.
     * 
     * @see #getClassSpace()
     */
    private final String CLASS_SPACE_PREFIX;

    /**
     * Prefix of class document.
     * 
     * @see #getClassPrefix()
     */
    private final String CLASS_PREFIX;

    /**
     * Space of class document.
     * 
     * @see #getClassSpace()
     */
    private final String CLASS_SPACE;

    /**
     * Name of class document.
     * 
     * @see #getClassName()
     */
    private final String CLASS_NAME;

    /**
     * Full name of class document.
     * 
     * @see #getClassFullName()
     */
    private final String CLASS_FULLNAME;

    /**
     * Space of class sheet document.
     * 
     * @see #getClassSpace()
     */
    private final String CLASSSHEET_SPACE;

    /**
     * Name of class sheet document.
     * 
     * @see #getClassSheetName()
     */
    private final String CLASSSHEET_NAME;

    /**
     * Full name of class sheet document.
     * 
     * @see #getClassSheetFullName()
     */
    private final String CLASSSHEET_FULLNAME;

    /**
     * Space of class template document.
     * 
     * @see #getClassSpace()
     */
    private final String CLASSTEMPLATE_SPACE;

    /**
     * Name of class template document.
     * 
     * @see #getClassTemplateName()
     */
    private final String CLASSTEMPLATE_NAME;

    /**
     * Full name of class template document.
     * 
     * @see #getClassTemplateFullName()
     */
    private final String CLASSTEMPLATE_FULLNAME;

    /**
     * Default content of class template document.
     */
    private final String classSheetDefaultContent;

    /**
     * Default content of class sheet document.
     */
    private final String classTemplateDefaultContent;

    public String getClassSpacePrefix()
    {
        return CLASS_SPACE_PREFIX;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassSpace()
     */
    public String getClassSpace()
    {
        return CLASS_SPACE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassPrefix()
     */
    public String getClassPrefix()
    {
        return CLASS_PREFIX;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassName()
     */
    public String getClassName()
    {
        return CLASS_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassFullName()
     */
    public String getClassFullName()
    {
        return CLASS_FULLNAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassTemplateName()
     */
    public String getClassTemplateSpace()
    {
        return CLASSTEMPLATE_SPACE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassTemplateName()
     */
    public String getClassTemplateName()
    {
        return CLASSTEMPLATE_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassTemplateFullName()
     */
    public String getClassTemplateFullName()
    {
        return CLASSTEMPLATE_FULLNAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassSheetName()
     */
    public String getClassSheetSpace()
    {
        return CLASSSHEET_SPACE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassSheetName()
     */
    public String getClassSheetName()
    {
        return CLASSSHEET_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassSheetFullName()
     */
    public String getClassSheetFullName()
    {
        return CLASSSHEET_FULLNAME;
    }

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
        CLASS_SPACE_PREFIX = spaceprefix;
        CLASS_PREFIX = prefix;

        CLASS_SPACE =
            dispatch ? CLASS_SPACE_PREFIX + XWIKI_CLASS_SPACE_SUFFIX : CLASS_SPACE_PREFIX;
        CLASS_NAME = CLASS_PREFIX + XWIKI_CLASS_SUFFIX;
        CLASS_FULLNAME = CLASS_SPACE + "." + CLASS_NAME;

        CLASSSHEET_SPACE =
            dispatch ? CLASS_SPACE_PREFIX + XWIKI_CLASSSHEET_SPACE_SUFFIX : CLASS_SPACE_PREFIX;
        CLASSSHEET_NAME = CLASS_PREFIX + XWIKI_CLASSSHEET_SUFFIX;
        CLASSSHEET_FULLNAME = CLASSSHEET_SPACE + "." + CLASSSHEET_NAME;

        CLASSTEMPLATE_SPACE =
            dispatch ? CLASS_SPACE_PREFIX + XWIKI_CLASSTEMPLATE_SPACE_SUFFIX : CLASS_SPACE_PREFIX;
        CLASSTEMPLATE_NAME = CLASS_PREFIX + XWIKI_CLASSTEMPLATE_SUFFIX;
        CLASSTEMPLATE_FULLNAME = CLASSTEMPLATE_SPACE + "." + CLASSTEMPLATE_NAME;

        classSheetDefaultContent =
            "## you can modify this page to customize the presentation of your object\n" + "\n"
                + "1 Document $doc.name\n" + "\n" + "#set($class = $doc.getObject(\""
                + CLASS_FULLNAME + "\").xWikiClass)\n" + "\n" + "<dl>\n"
                + "  #foreach($prop in $class.properties)\n"
                + "    <dt> ${prop.prettyName} </dt>\n"
                + "    <dd>$doc.display($prop.getName())</dd>\n" + "  #end\n" + "</dl>\n";

        classTemplateDefaultContent = "#includeForm(\"" + CLASSSHEET_FULLNAME + "\")\n";
    }

    /**
     * Base class managed.
     */
    private BaseClass baseClass = null;

    /**
     * Store for any database name if documents used for manage this class has been checked;
     */
    private final HashSet databasesInitedMap = new HashSet();

    /**
     * Check if all necessary documents for manage this class in this context exists and update.
     * Create if not exists. Thread safe.
     * 
     * @param context the XWiki context.
     * @throws XWikiException
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
     * @throws XWikiException
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
            needsUpdate = true;
        }

        this.baseClass = doc.getxWikiClass();

        needsUpdate |= updateBaseClass(this.baseClass);

        if (doc.isNew() || needsUpdate)
            xwiki.saveDocument(doc, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassSheetDefaultContent()
     */
    public String getClassSheetDefaultContent()
    {
        return classSheetDefaultContent;
    }

    /**
     * Check if class sheet document exists in this context and update. Create if not exists.
     * 
     * @param context the XWiki context.
     * @throws XWikiException
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
            needsUpdate = true;
        }

        if (doc.isNew()) {
            doc.setContent(getClassSheetDefaultContent());
        }

        if (doc.isNew() || needsUpdate)
            xwiki.saveDocument(doc, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassTemplateDefaultContent()
     */
    public String getClassTemplateDefaultContent()
    {
        return classTemplateDefaultContent;
    }

    /**
     * Check if class template document exists in this context and update. Create if not exists.
     * 
     * @param context the XWiki context.
     * @throws XWikiException
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
            doc.setContent(getClassTemplateDefaultContent());
        }

        if (doc.isNew() || needsUpdate)
            xwiki.saveDocument(doc, context);
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
     * @see com.xpn.xwiki.util.SuperClass#getBaseClass()
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
     * @see com.xpn.xwiki.util.SuperClass#getClassDocument(com.xpn.xwiki.XWikiContext)
     */
    public XWikiDocument getClassDocument(XWikiContext context) throws XWikiException
    {
        check(context);

        return context.getWiki().getDocument(getClassFullName(), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassSheetDocument(com.xpn.xwiki.XWikiContext)
     */
    public XWikiDocument getClassSheetDocument(XWikiContext context) throws XWikiException
    {
        check(context);

        return context.getWiki().getDocument(getClassSheetFullName(), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getClassTemplateDocument(com.xpn.xwiki.XWikiContext)
     */
    public XWikiDocument getClassTemplateDocument(XWikiContext context) throws XWikiException
    {
        check(context);

        return context.getWiki().getDocument(getClassTemplateFullName(), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#isInstanceOf(com.xpn.xwiki.doc.XWikiDocument,
     *      com.xpn.xwiki.XWikiContext)
     */
    public boolean isInstance(XWikiDocument doc, XWikiContext context)
    {
        return doc.getObject(getClassFullName()) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getItemDocumentName(java.lang.String)
     */
    public String getItemDocumentDefaultName(String itemName, XWikiContext context)
    {
        itemName = context.getWiki().clearName(itemName, true, true, context);

        return getClassPrefix() + itemName.substring(0, 1).toUpperCase()
            + itemName.substring(1).toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getItemDocumentFullName(java.lang.String)
     */
    public String getItemDocumentDefaultFullName(String itemName, XWikiContext context)
    {
        return getClassSpacePrefix() + "." + getItemDocumentDefaultName(itemName, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass#getItemDefaultName(java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public String getItemDefaultName(String docFullName, XWikiContext context)
    {
        return docFullName.substring((getClassSpacePrefix() + "." + getClassPrefix()).length())
            .toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#getItemDocument(java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
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
     * @throws XWikiException
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
     * @throws XWikiException
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
        return searchItemDocumentsByFields(null, new String[][] {{fieldType, fieldName,
        fieldValue}}, context);
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

        String from = ", BaseObject as obj";

        String where =
            " where doc.fullName=obj.name" + " and obj.className='" + getClassFullName() + "'";

        if (docFullName != null)
            where += " and obj.name='" + docFullName + "'";
        else
            where += " and obj.name<>'" + getClassTemplateFullName() + "'";

        if (fieldDescriptors != null)
            for (int i = 0; i < fieldDescriptors.length; ++i) {
                from += ", " + fieldDescriptors[i][0] + " as field" + i;

                where +=
                    " and obj.id=field" + i + ".id.id" + " and field" + i + ".id.name='"
                        + fieldDescriptors[i][1] + "'" + " and field" + i + ".id.value='"
                        + fieldDescriptors[i][2] + "'";
            }

        return context.getWiki().getStore().searchDocuments(from + where, context);
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
     * @see com.xpn.xwiki.util.SuperClass#newSuperDocument(java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(String docFullName, XWikiContext context)
        throws XWikiException
    {
        return newSuperDocument(context.getWiki().getDocument(docFullName, context), context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.SuperClass#newSuperDocument(com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(XWikiContext context) throws XWikiException
    {
        return newSuperDocument(new XWikiDocument(), context);
    }
}
