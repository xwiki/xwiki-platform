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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractSuperClass}.
 * 
 * @version $Id: $
 */
public class TestAbstractSuperClassTest extends MockObjectTestCase
{
    private XWikiContext context;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Map documents = new HashMap();

    protected void setUp() throws XWikiException
    {
        this.context = new XWikiContext();
        this.xwiki = new XWiki(new XWikiConfig(), this.context);
        
        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {this.xwiki, this.context});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    
                    if (documents.containsKey(shallowDoc.getFullName())) {
                        return documents.get(shallowDoc.getFullName());
                    } else {
                        return shallowDoc;
                    }
                }
            });
        this.mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    documents.put(document.getFullName(), document);
                    
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(
            returnValue(Collections.EMPTY_LIST));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class,
            XWikiContext.class}, new Object[] {this.xwiki, this.context});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(
            returnValue(null));
        this.mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(
            returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore
            .proxy());
    }

    /////////////////////////////////////////////////////////////////////////////////////////:
    // Tests
    
    private static final String CLASS_SPACE_PREFIX = "Space";
    private static final String CLASS_PREFIX = "Prefix";
    
    private static final String CLASS_NAME = CLASS_PREFIX + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass
        .XWIKI_CLASS_SUFFIX;
    private static final String CLASSSHEET_NAME = CLASS_PREFIX + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass
        .XWIKI_CLASSSHEET_SUFFIX;
    private static final String CLASSTEMPLATE_NAME = CLASS_PREFIX + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass
        .XWIKI_CLASSTEMPLATE_SUFFIX;
    
    private static final String DISPATCH_CLASS_SPACE = CLASS_SPACE_PREFIX + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass
        .XWIKI_CLASS_SPACE_SUFFIX;
    private static final String DISPATCH_CLASS_FULLNAME = DISPATCH_CLASS_SPACE + "." + CLASS_NAME;
    private static final String DISPATCH_CLASSSHEET_SPACE = CLASS_SPACE_PREFIX + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass
        .XWIKI_CLASSSHEET_SPACE_SUFFIX;
    private static final String DISPATCH_CLASSSHEET_FULLNAME = DISPATCH_CLASSSHEET_SPACE + "." + CLASSSHEET_NAME;
    private static final String DISPATCH_CLASSTEMPLATE_SPACE = CLASS_SPACE_PREFIX + com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass
        .XWIKI_CLASSTEMPLATE_SPACE_SUFFIX;
    private static final String DISPATCH_CLASSTEMPLATE_FULLNAME = DISPATCH_CLASSTEMPLATE_SPACE + "." + CLASSTEMPLATE_NAME;
    
    private static final String NODISPATCH_CLASS_SPACE = CLASS_SPACE_PREFIX;
    private static final String NODISPATCH_CLASS_FULLNAME = NODISPATCH_CLASS_SPACE + "." + CLASS_NAME;
    private static final String NODISPATCH_CLASSSHEET_SPACE = CLASS_SPACE_PREFIX;
    private static final String NODISPATCH_CLASSSHEET_FULLNAME = NODISPATCH_CLASSSHEET_SPACE + "." + CLASSSHEET_NAME;
    private static final String NODISPATCH_CLASSTEMPLATE_SPACE = CLASS_SPACE_PREFIX;
    private static final String NODISPATCH_CLASSTEMPLATE_FULLNAME = NODISPATCH_CLASSTEMPLATE_SPACE + "." + CLASSTEMPLATE_NAME;
    
    private static final String DEFAULT_ITEM_NAME = "item";
    private static final String DEFAULT_ITEMDOCUMENT_NAME = CLASS_PREFIX + "Item";
    private static final String DISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME = CLASS_SPACE_PREFIX + "." + DEFAULT_ITEMDOCUMENT_NAME;
    private static final String NODISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME = CLASS_SPACE_PREFIX + "." + DEFAULT_ITEMDOCUMENT_NAME;
        
    /**
     * Name of field <code>string</code>.
     */
    public static final String FIELD_string = "string";
    /**
     * Pretty name of field <code>string</code>.
     */
    public static final String FIELDPN_string = "String";

    /**
     * Name of field <code>stringlist</code>.
     */
    public static final String FIELD_stringlist = "stringlist";
    /**
     * Pretty name of field <code>stringlist</code>.
     */
    public static final String FIELDPN_stringlist = "String List";
    
    static abstract public class SuperClass extends AbstractSuperClass
    {
         /**
         * Default constructor for XWikiApplicationClass.
         */
        protected SuperClass(String spaceprefix, String prefix, boolean dispatch)
        {
            super(spaceprefix, prefix, dispatch);
        }

        protected boolean updateBaseClass(BaseClass baseClass)
        {
            boolean needsUpdate = super.updateBaseClass(baseClass);

            needsUpdate |= baseClass.addTextField(FIELD_string, FIELDPN_string, 30);
            needsUpdate |= baseClass.addTextField(FIELD_stringlist, FIELDPN_stringlist, 80);

            return needsUpdate;
        }
    }
    
    static public class DispatchSuperClass extends SuperClass
    {
        /**
         * Unique instance of XWikiApplicationClass;
         */
        private static DispatchSuperClass instance = null;

        /**
         * Return unique instance of XWikiApplicationClass and update documents for this context.
         * 
         * @param context Context.
         * @return XWikiApplicationClass Instance of XWikiApplicationClass.
         * @throws XWikiException
         */
        public static DispatchSuperClass getInstance(XWikiContext context) throws XWikiException
        {
            //if (instance == null)
                instance = new DispatchSuperClass();

            instance.check(context);

            return instance;
        }

        /**
         * Default constructor for XWikiApplicationClass.
         */
        private DispatchSuperClass()
        {
            super(CLASS_SPACE_PREFIX, CLASS_PREFIX, true);
        }
    }
    
    static public class NoDispatchSuperClass extends SuperClass
    {
        /**
         * Unique instance of XWikiApplicationClass;
         */
        private static NoDispatchSuperClass instance = null;

        /**
         * Return unique instance of XWikiApplicationClass and update documents for this context.
         * 
         * @param context Context.
         * @return XWikiApplicationClass Instance of XWikiApplicationClass.
         * @throws XWikiException
         */
        public static NoDispatchSuperClass getInstance(XWikiContext context) throws XWikiException
        {
            //if (instance == null)
                instance = new NoDispatchSuperClass();

            instance.check(context);

            return instance;
        }

        /**
         * Default constructor for XWikiApplicationClass.
         */
        private NoDispatchSuperClass()
        {
            super(CLASS_SPACE_PREFIX, CLASS_PREFIX, false);
        }
    }
    
    public void testInitSuperClassDispatch() throws XWikiException
    {
        documents.clear();
        
        /////
        
        DispatchSuperClass sclass = DispatchSuperClass.getInstance(context);
        
        assertEquals(CLASS_SPACE_PREFIX, sclass.getClassSpacePrefix());
        assertEquals(CLASS_PREFIX, sclass.getClassPrefix());
        
        assertEquals(CLASS_NAME, sclass.getClassName());
        assertEquals(CLASSSHEET_NAME, sclass.getClassSheetName());
        assertEquals(CLASSTEMPLATE_NAME, sclass.getClassTemplateName());
        
        assertEquals(DISPATCH_CLASS_SPACE, sclass.getClassSpace());
        assertEquals(DISPATCH_CLASS_FULLNAME, sclass.getClassFullName());
        assertEquals(DISPATCH_CLASSSHEET_SPACE, sclass.getClassSheetSpace());
        assertEquals(DISPATCH_CLASSSHEET_FULLNAME, sclass.getClassSheetFullName());
        assertEquals(DISPATCH_CLASSTEMPLATE_SPACE, sclass.getClassTemplateSpace());
        assertEquals(DISPATCH_CLASSTEMPLATE_FULLNAME, sclass.getClassTemplateFullName());
    }
    
    public void testInitSuperClassNoDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        NoDispatchSuperClass sclass = NoDispatchSuperClass.getInstance(context);
        
        assertEquals(CLASS_SPACE_PREFIX, sclass.getClassSpacePrefix());
        assertEquals(CLASS_PREFIX, sclass.getClassPrefix());
        
        assertEquals(CLASS_NAME, sclass.getClassName());
        assertEquals(CLASSSHEET_NAME, sclass.getClassSheetName());
        assertEquals(CLASSTEMPLATE_NAME, sclass.getClassTemplateName());
        
        assertEquals(NODISPATCH_CLASS_SPACE, sclass.getClassSpace());
        assertEquals(NODISPATCH_CLASS_FULLNAME, sclass.getClassFullName());
        assertEquals(NODISPATCH_CLASSSHEET_SPACE, sclass.getClassSheetSpace());
        assertEquals(NODISPATCH_CLASSSHEET_FULLNAME, sclass.getClassSheetFullName());
        assertEquals(NODISPATCH_CLASSTEMPLATE_SPACE, sclass.getClassTemplateSpace());
        assertEquals(NODISPATCH_CLASSTEMPLATE_FULLNAME, sclass.getClassTemplateFullName());
    }
    
    private void ptestCkeck(SuperClass sclass) throws XWikiException
    {        
        XWikiDocument doc = xwiki.getDocument(sclass.getClassFullName(), context);
        
        assertFalse(doc.isNew());
        
        BaseClass baseclass = doc.getxWikiClass();
        
        assertEquals(sclass.getClassFullName(), baseclass.getName());
        
        PropertyInterface prop = baseclass.getField(FIELD_string);
        
        assertNotNull(prop);
        
        prop = baseclass.getField(FIELD_stringlist);
        
        assertNotNull(prop);
        
        /////
        
        XWikiDocument docSheet = xwiki.getDocument(sclass.getClassSheetFullName(), context);
        
        assertFalse(docSheet.isNew());
        
        /////
        
        XWikiDocument docTemplate = xwiki.getDocument(sclass.getClassTemplateFullName(), context);
        
        assertFalse(docTemplate.isNew());
        
        BaseObject baseobject = docTemplate.getObject(sclass.getClassFullName());
        
        assertNotNull(baseobject);
    }
    
    public void testCkeckDispatch() throws XWikiException
    {
        documents.clear();
        
        /////
        
        ptestCkeck(NoDispatchSuperClass.getInstance(context));
    }
    
    public void testCkeckNoDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        ptestCkeck(NoDispatchSuperClass.getInstance(context));
    }
    
    private void ptestGetClassDocument(SuperClass sclass) throws XWikiException
    {
        XWikiDocument doc = xwiki.getDocument(sclass.getClassFullName(), context);
        XWikiDocument docFromClass = sclass.getClassDocument(context);
    
        assertFalse(docFromClass.isNew());
        assertEquals(doc.getFullName(), docFromClass.getFullName());
    }
    
    public void testGetClassDocumentDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        ptestGetClassDocument(DispatchSuperClass.getInstance(context));
    }
    
    public void testGetClassDocumentNoDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        ptestGetClassDocument(NoDispatchSuperClass.getInstance(context));
    }
    
    private void ptestGetClassSheetDocument(SuperClass sclass) throws XWikiException
    {
        XWikiDocument doc = xwiki.getDocument(sclass.getClassSheetFullName(), context);
        XWikiDocument docFromClass = sclass.getClassSheetDocument(context);
        
        assertFalse(docFromClass.isNew());
        assertEquals(doc.getFullName(), docFromClass.getFullName());
    }
    
    public void testGetClassSheetDocumentDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        ptestGetClassSheetDocument(DispatchSuperClass.getInstance(context));
    }
    
    public void testGetClassSheetDocumentNoDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        ptestGetClassSheetDocument(NoDispatchSuperClass.getInstance(context));
    }
    
    private void ptestGetClassTemplateDocument(SuperClass sclass) throws XWikiException
    {
        XWikiDocument doc = xwiki.getDocument(sclass.getClassTemplateFullName(), context);
        XWikiDocument docFromClass = sclass.getClassTemplateDocument(context);
        
        assertFalse(docFromClass.isNew());
        assertEquals(doc.getFullName(), docFromClass.getFullName());
    }
    
    public void testGetClassTemplateDocumentDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        ptestGetClassTemplateDocument(DispatchSuperClass.getInstance(context));
    }
    
    public void testGetClassTemplateDocumentNoDispatch() throws XWikiException
    {
        documents.clear();

        /////
        
        ptestGetClassTemplateDocument(NoDispatchSuperClass.getInstance(context));
    }

    public void testGetItemDefaultNameDisptach() throws XWikiException
    {
        assertEquals(DEFAULT_ITEM_NAME, DispatchSuperClass.getInstance(context).getItemDefaultName(DISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME, context));
    }
    
    public void testGetItemDefaultNameNoDispatch() throws XWikiException
    {
        assertEquals(DEFAULT_ITEM_NAME, NoDispatchSuperClass.getInstance(context).getItemDefaultName(NODISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME, context));
    }
    
    public void testGetItemDocumentDefaultNameDispatch() throws XWikiException
    {
        assertEquals(DEFAULT_ITEMDOCUMENT_NAME, DispatchSuperClass.getInstance(context).getItemDocumentDefaultName(DEFAULT_ITEM_NAME, context));
        assertEquals(DEFAULT_ITEMDOCUMENT_NAME, DispatchSuperClass.getInstance(context).getItemDocumentDefaultName(DEFAULT_ITEM_NAME+" ", context));
        assertEquals(DEFAULT_ITEMDOCUMENT_NAME, DispatchSuperClass.getInstance(context).getItemDocumentDefaultName(DEFAULT_ITEM_NAME+"\t", context));
        assertEquals(DEFAULT_ITEMDOCUMENT_NAME, DispatchSuperClass.getInstance(context).getItemDocumentDefaultName(DEFAULT_ITEM_NAME+".", context));
    }
    
    public void testGetItemDocumentDefaultNameNoDispatch() throws XWikiException
    {
        assertEquals(DEFAULT_ITEMDOCUMENT_NAME, NoDispatchSuperClass.getInstance(context).getItemDocumentDefaultName(DEFAULT_ITEM_NAME, context));
    }
    
    public void getItemDocumentDefaultFullNameDispatch() throws XWikiException
    {
        assertEquals(DISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME, DispatchSuperClass.getInstance(context).getItemDocumentDefaultFullName(DEFAULT_ITEM_NAME, context));
    }
    
    public void getItemDocumentDefaultFullNameNoDispatch() throws XWikiException
    {
        assertEquals(NODISPATCH_DEFAULT_ITEMDOCUMENT_FULLNAME, NoDispatchSuperClass.getInstance(context).getItemDocumentDefaultFullName(DEFAULT_ITEM_NAME, context));
    }
}
