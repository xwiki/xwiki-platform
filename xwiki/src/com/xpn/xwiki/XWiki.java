/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 13:52:39
 */

package com.xpn.xwiki;

import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.objects.meta.MetaClass;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.ecs.html.TextArea;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.ecs.Filter;

public class XWiki {

    private XWikiConfig config;
    private XWikiStoreInterface store;
    private XWikiRenderingEngine renderingEngine;
    private MetaClass metaclass = MetaClass.getMetaClass();

    public XWiki(String path) throws XWikiException {
      config = new XWikiConfig(path);
      String storeclass = Param("xwiki.store.class","com.xpn.xwiki.store.XWikiRCSFileStore");
      try {
         Class[] classes = new Class[1];
         classes[0] = this.getClass();
         Object[] args = new Object[1] ;
         args[0] = this;
         store = (XWikiStoreInterface)Class.forName(storeclass).getConstructor(classes).newInstance(args);
        }
        catch (InvocationTargetException e)
        {
         Object[] args = { storeclass };
         throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                  XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR,
                                  "Cannot load store class {0}",e.getTargetException(), args);
        } catch (Exception e) {
          Object[] args = { storeclass };
             throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                      XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR,
                                      "Cannot load store class {0}",e, args);
        }
        renderingEngine = new XWikiRenderingEngine(this);
    }

    public XWikiConfig getConfig() {
        return config;
    }

    public String Param(String key) {
        return getConfig().getProperty(key);
    }

    public String Param(String key, String default_value) {
        return getConfig().getProperty(key, default_value);
    }

    public XWikiStoreInterface getStore() {
        return store;
    }

    public void saveDocument(XWikiDocInterface doc) throws XWikiException {
        store.saveXWikiDoc(doc);
    }

    public XWikiDocInterface getDocument(XWikiDocInterface doc) throws XWikiException {
        try {
            store.loadXWikiDoc(doc);
        }  catch (XWikiException e) {
            // TODO: log error for document that does not exist.
        }
        return doc;
    }

    public XWikiDocInterface getDocument(String web, String name) throws XWikiException {
        XWikiSimpleDoc doc = new XWikiSimpleDoc(web, name);
        return getDocument(doc);
    }

    public XWikiDocInterface getDocument(String fullname) throws XWikiException {
        int i1 = fullname.lastIndexOf(".");
        String web = fullname.substring(0,i1);
        String name = fullname.substring(i1+1);
        if (name.equals(""))
         name = "WebHome";
        return getDocument(web,name);
    }

    public XWikiDocInterface getDocumentFromPath(String path) throws XWikiException {
        int i1 = path.indexOf("/",1);
        int i2 = path.lastIndexOf("/");
        String web = path.substring(i1+1,i2);
        String name = path.substring(i2+1);
        if (name.equals(""))
         name = "WebHome";
        return getDocument(web,name);
    }

    public String getBase() {
        return Param("xwiki.base","/xwiki/bin/");
    }

    public XWikiRenderingEngine getRenderingEngine() {
        return renderingEngine;
    }

    public void setRenderingEngine(XWikiRenderingEngine renderingEngine) {
        this.renderingEngine = renderingEngine;
    }

    public MetaClass getMetaclass() {
        return metaclass;
    }

    public void setMetaclass(MetaClass metaclass) {
        this.metaclass = metaclass;
    }

    public String getTextArea(String content) {
        Filter filter = new CharacterFilter();
        String scontent = filter.process(content);

        TextArea textarea = new TextArea();
        textarea.setFilter(filter);
        textarea.setRows(20);
        textarea.setCols(80);
        textarea.setName("content");
        textarea.addElement(scontent);
        return textarea.toString();
    }

    public String[] getClassList() throws XWikiException {
      List list = store.getClassList();
      String[] array = new String[list.size()];
      for (int i=0;i<list.size();i++)
         array[i] = (String)list.get(i);
      return array;
    }
}
