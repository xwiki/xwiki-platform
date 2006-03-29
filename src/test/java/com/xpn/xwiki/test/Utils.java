/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author vmassol
 * @author sdumitriu
 * @author thomas
 */

package com.xpn.xwiki.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.jrcs.diff.Diff;
import org.apache.commons.jrcs.diff.Revision;
import org.apache.commons.jrcs.util.ToString;
import org.apache.ecs.Filter;
import org.apache.ecs.filter.CharacterFilter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseElement;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.store.XWikiStoreInterface;



public class Utils {
    public static String name = "WebHome";
    public static String name2 = "Globals";
    public static String web = "Main";
    public static String content1 = "Hello 1\n<Hello> 2\nHello 3\n";
    public static String content3 = "Hello 1\nIntermediary line\nHello 2\nHello 3\n";
    public static String author = "XWiki.LudovicDubost";
    public static String author2 = "XWiki.JulesVerne";
    public static String parent = "Main.WebHome";
    public static String version = "1.1";
    public static String version2 = "1.2";
    public static String language = "";
    public static String defaultLanguage = "en";


    public static String rcspath = "./rcs";
    public static String rcsattachmentpath = "./rcsattachments";
    public static String filename = "test1.sxw";
    public static String filename2 = "test1.doc";
    public static String afilename = "text1.sxw";

    public static void setStandardData() {
        name = "WebHome";
        name2 = "Globals";
        web = "Main";
        content1 = "Hello 1\nHello 2\nHello 3\n";
        content3 = "Hello 1\nIntermediary line\nHello 2\nHello 3\n";
        author = "XWiki.LudovicDubost";
        author2 = "XWiki.JulesVerne";
        parent = "Main.WebHome";
        version = "1.1";
        version2 = "1.2";
        afilename = filename;
    }


    public static void setStandardAccentData() {
        name = "WebHome";
        name2 = "Globals";
        web = "Main";
        content1 = "Hello 1\nTexte avec des accents: �����\nHello 3\n";
        content3 = "Hello 1\nAutre texte ����\nHello 2\nHello 3\n";
        //try {
        // content1 = content1.getBytes("UTF-8").toString();
        // content3 = content3.getBytes("UTF-8").toString();
        //} catch (Exception e) {}
        author = "XWiki.LudovicDubost";
        author2 = "XWiki.JulesVerne";
        parent = "Main.WebHome";
        version = "1.1";
        version2 = "1.2";
        afilename = filename;
    }

    public static void setStandardIsoData() {
        name = "Ao�t2002";
        name2 = "Globals";
        web = "Main";
        content1 = "Hello 1\nAo�t\nHello 3\n";
        content3 = "Hello 1\nIntermediary line\nHello 2\nHello 3\n";
        author = "XWiki.Hel�neDupont";
        author2 = "XWiki.JulesVerne";
        parent = "Main.WebHome";
        version = "1.1";
        version2 = "1.2";
        afilename = "ao�t.txt";
    }

    public static void setMediumData() {

        setStandardData();

        while (content1.length()<1000)
            content1 += content1;


        while (author.length()<120)
            author += author;
        while (content3.length()<1000)
            content3 += content3;
    }

    public static String getData(File file) throws IOException {
        StringBuffer content = new StringBuffer();
        BufferedReader fr = new BufferedReader(new FileReader(file));
        String line;
        while ((line = fr.readLine())!=null) {
            content.append(line);
            content.append("\n");
        }
        fr.close();
        return content.toString();
    }

    public static byte[] getDataAsBytes(File file) throws IOException {
        byte[] result = new byte[(int)file.length()];
        FileInputStream fileis = new FileInputStream(file);
        fileis.read(result);
        fileis.close();
        return result;
    }

    public static void setBigData() throws IOException {
        setStandardData();
        while (author.length()<120)
            author += author;

        File file1 = new File(rcspath + "/" + web + "/" + name2 + ".txt.1");
        File file3 = new File(rcspath + "/" + web + "/" + name2 + ".txt.2");
        content1 = getData(file1);
        content3 = getData(file3);
    }


    public static XWikiDocument createDoc(XWikiStoreInterface store, String web, String name, XWikiContext context) throws XWikiException {
        return createDoc(store,web, name, content1, null, null, null, context);
    }

    public static XWikiDocument createDoc(XWikiStoreInterface store, String web, String name, String content, BaseObject bobject, BaseClass bclass, XWikiContext context) throws XWikiException {
        return createDoc(store,web, name, content, null, null, null, context);
    }

    public static XWikiDocument createDoc(XWikiStoreInterface store, String web, String name,
                                 BaseObject bobject, BaseClass bclass, XWikiContext context) throws XWikiException {
        return createDoc(store,web, name, content1, bobject, bclass, null, context);

    }

    public static XWikiDocument createDoc(XWikiStoreInterface store, String web, String name,
                                 String content, BaseObject bobject, BaseClass bclass,
                                 Map bobjects, XWikiContext context) throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument(web, name);
        String fullname = doc1.getFullName();
        doc1.setContent(content);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.web + "." + Utils.name);
        doc1.setDefaultLanguage(Utils.defaultLanguage);

        if (bobjects!=null)
            doc1.setxWikiObjects(bobjects);

        if (bobject!=null) {
            bobject.setName(fullname);
            doc1.setObject(fullname, 0, bobject);
        }

        if (bclass!=null) {
            doc1.setxWikiClass(bclass);
        }

        store.saveXWikiDoc(doc1, context);
        return doc1;
    }

    public static XWikiDocument createDoc(XWikiStoreInterface store, String web, String name,
                                     BaseObject bobject, BaseClass bclass,
                                     Map bobjects, XWikiContext context) throws XWikiException {
              return createDoc(store, web, name, content1, bobject, bclass, bobjects, context);
        }

    public static BaseObject prepareObject(XWikiDocument doc) throws XWikiException {
        return prepareObject(doc, "Test.TestObject");
    }

    public static BaseClass prepareClass(XWikiDocument doc, String name) throws XWikiException {
        BaseClass wclass = doc.getxWikiClass();
        if (wclass==null)
          wclass =  new BaseClass();
        wclass.setName(name);
        doc.setxWikiClass(wclass);

        StringClass first_name_class = new StringClass();
        first_name_class.setName("first_name");
        first_name_class.setPrettyName("First Name");
        first_name_class.setSize(80);
        first_name_class.setObject(wclass);
        StringClass last_name_class = new StringClass();
        last_name_class.setName("last_name");
        last_name_class.setPrettyName("Last Name");
        last_name_class.setSize(80);
        last_name_class.setObject(wclass);
        NumberClass age_class = new NumberClass();
        age_class.setName("age");
        age_class.setPrettyName("Age");
        age_class.setSize(5);
        age_class.setNumberType("integer");
        age_class.setObject(wclass);
        PasswordClass passwd_class = new PasswordClass();
        passwd_class.setName("password");
        passwd_class.setPrettyName("Password");
        passwd_class.setSize(10);
        passwd_class.setObject(wclass);
        TextAreaClass comment_class = new TextAreaClass();
        comment_class.setName("comment");
        comment_class.setPrettyName("Comment");
        comment_class.setSize(80);
        comment_class.setRows(10);
        comment_class.setObject(wclass);

        wclass.put("first_name", first_name_class);
        wclass.put("last_name", last_name_class);
        wclass.put("age", age_class);
        wclass.put("password", passwd_class);
        wclass.put("comment", comment_class);
        return wclass;
    }

    public static BaseObject prepareObject(XWikiDocument doc, String name) throws XWikiException {
        BaseClass wclass = prepareClass(doc, name);
        BaseObject object = new BaseObject();
        doc.setObject(wclass.getName(), 0, object);

        object.setClassName(wclass.getName());
        if ((doc.getName()==null)||"".equals(doc.getName()))
         object.setName(name);
        else
         object.setName(doc.getFullName());
        object.put("first_name", ((PropertyClass)wclass.get("first_name")).fromString("Ludovic"));
        object.put("last_name", ((PropertyClass)wclass.get("last_name")).fromString("Von Dubost"));
        object.put("age", ((PropertyClass)wclass.get("age")).fromString("33"));
        object.put("password", ((PropertyClass)wclass.get("password")).fromString("sesame"));
        object.put("comment",((PropertyClass)wclass.get("comment")).fromString("Hello1\nHello2\nHello3\n"));
        return object;
    }

    public static BaseObject prepareAdvancedObject(XWikiDocument doc) throws XWikiException {
        return prepareAdvancedObject(doc, "Test.TestObject");
    }

    public static BaseClass prepareAdvancedClass(XWikiDocument doc, String name) throws XWikiException {
        BaseClass wclass = doc.getxWikiClass();
        BooleanClass boolean_class = new BooleanClass();
        boolean_class.setName("driver");
        boolean_class.setPrettyName("Driver License ?");
        boolean_class.setDisplayType("truefalse");
        boolean_class.setObject(wclass);
        wclass.put("driver", boolean_class);

        StaticListClass slist_class = new StaticListClass();
        slist_class.setName("category");
        slist_class.setPrettyName("Category");
        slist_class.setValues("1|2|3");
        slist_class.setObject(wclass);
        wclass.put("category", slist_class);


        StaticListClass slist_class2 = new StaticListClass();
        slist_class2.setName("category2");
        slist_class2.setPrettyName("Category2");
        slist_class2.setMultiSelect(true);
        slist_class2.setValues("1|2|3");
        slist_class2.setObject(wclass);
        wclass.put("category2", slist_class2);

        StaticListClass slist_class3 = new StaticListClass();
        slist_class3.setName("category3");
        slist_class3.setPrettyName("Category3");
        slist_class3.setValues("1|2|3");
        slist_class3.setObject(wclass);
        slist_class3.setRelationalStorage(true);
        slist_class3.setMultiSelect(true);
        wclass.put("category3", slist_class3);

        DBListClass dblist_class = new DBListClass();
        dblist_class.setName("dblist");
        dblist_class.setPrettyName("dblist");
        dblist_class.setSql("select distinct doc.name from XWikiDocument as doc");
        dblist_class.setObject(wclass);
        wclass.put("dblist", dblist_class);
        return wclass;
    }

    public static BaseObject prepareAdvancedObject(XWikiDocument doc, String name) throws XWikiException {
        BaseObject object = prepareObject(doc, name);
        BaseClass wclass = prepareAdvancedClass(doc, name);

        object.put("driver", ((PropertyClass)wclass.get("driver")).fromString("1"));
        object.put("category", ((PropertyClass)wclass.get("category")).fromString("1"));
        object.put("category2", ((PropertyClass)wclass.get("category2")).fromString("1|2"));
        object.put("category3", ((PropertyClass)wclass.get("category3")).fromString("1|2"));
        object.put("dblist", ((PropertyClass)wclass.get("dblist")).fromString("XWikiUsers"));
        return object;
    }


    public static IntegerProperty prepareIntegerProperty() {
        BaseObject object = new BaseObject();
        object.setName("Main.TestObject1");
        IntegerProperty prop = new IntegerProperty();
        prop.setObject(object);
        prop.setPrettyName("Age");
        prop.setName("age");
        prop.setValue(new Integer(34));
        return prop;
    }

    public static StringProperty prepareStringProperty() {
        BaseObject object = new BaseObject();
        object.setName("Main.TestObject1");
        StringProperty prop = new StringProperty();
        prop.setObject(object);
        prop.setName("name");
        prop.setPrettyName("Name");
        prop.setValue(new String("Dubost"));
        return prop;
    }

    public static StringListProperty prepareStringListProperty() {
        BaseObject object = new BaseObject();
        object.setName("Main.TestObject1");
        StringListProperty prop = new StringListProperty();
        prop.setObject(object);
        prop.setName("category");
        prop.setPrettyName("Category");
        List list = new ArrayList();
        list.add("1");
        prop.setValue(list);
        return prop;
    }

    public static DBStringListProperty prepareDBStringListProperty() {
        BaseObject object = new BaseObject();
        object.setName("Main.TestObject1");
        DBStringListProperty prop = new DBStringListProperty();
        prop.setObject(object);
        prop.setName("category");
        prop.setPrettyName("Category");
        List list = new ArrayList();
        list.add("1");
        prop.setValue(list);
        return prop;
    }

    public static boolean testEqualsNoName(BaseElement o1, BaseElement o2) {
        String name = o2.getName();
        try {
            o2.setName(o1.getName());
            return o2.equals(o1);
        } finally {
            o2.setName(name);
        }
    }

    public static void assertEquals(String msg, BaseElement o1, BaseElement o2, boolean bInclName) {
        AssertionFailedError e1;
        try {
            if (!bInclName) {
                if (testEqualsNoName(o1, o2))
                    return;
            }

            TestCase.assertEquals(msg, o1, o2);
            return;
        } catch (AssertionFailedError e) {
            e1 = e;
        }
        try {
            String so1 = o1.toString();
            String so2 = o2.toString();
            Revision rev = Diff.diff(ToString.stringToArray(so1), ToString.stringToArray(so2));
            if ((!bInclName)&&(rev.size()==1)) {
                if (rev.getDelta(0).getOriginal().toString().matches("<name>.*</name>"))
                    return;
            }
            String srev = rev.toString();
            throw new AssertionFailedError(msg + ":\n" + srev);
        } catch (Exception e2) {}
        throw e1;
    }

    public static void assertEquals(XWikiDocument doc1, XWikiDocument doc2) {
      assertEquals(doc1, doc2, true, true, false);
    }

    public static void assertEquals(XWikiDocument doc1, XWikiDocument doc2, boolean bInclName, boolean bInclVersion, boolean bInclTime) {
        if (bInclName) {
        TestCase.assertEquals("Name is different", doc1.getName(), doc2.getName());
        TestCase.assertEquals("Web is different", doc1.getWeb(), doc2.getWeb());
        }
        TestCase.assertEquals("Author is different", doc1.getAuthor(), doc2.getAuthor());
        if (bInclTime) {
            TestCase.assertEquals("Date is different", doc1.getDate().getTime(), doc2.getDate().getTime());
        }
        TestCase.assertEquals("Format is different", doc1.getFormat(), doc2.getFormat());
        if (bInclVersion) {
            TestCase.assertEquals("Version is different", doc1.getVersion(), doc2.getVersion());
        }
        TestCase.assertEquals("Content is different", doc1.getContent(), doc2.getContent());
        assertEquals("xWikiClass is different", doc1.getxWikiClass(), doc2.getxWikiClass(), bInclName);

        Set list1 = doc1.getxWikiObjects().keySet();
        Set list2 = doc2.getxWikiObjects().keySet();
        TestCase.assertEquals("Object list is different", list1, list2);

        for (Iterator it = list1.iterator();it.hasNext();) {
            String name = (String) it.next();
            Vector v1 = doc1.getObjects(name);
            Vector v2 = doc2.getObjects(name);
            TestCase.assertEquals("Object number for " + name + " is different", v1.size(), v2.size());

            for (int i=0;i<v1.size();i++) {
                assertEquals("Object " + i + " for " + name + " is different\n", (BaseElement)v1.get(i), (BaseElement)v2.get(i), bInclName);
            }
        }
    }

    public static void assertProperty(BaseCollection object1, BaseCollection object2, String propname) {
        BaseElement prop1 = (BaseElement)object1.safeget(propname);
        BaseElement prop2 = (BaseElement)object2.safeget(propname);
        TestCase.assertEquals("Property " + propname + " is different", prop1, prop2);
    }


    public static String formEncode(String value) {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String svalue = filter.process(value);
        return svalue;
    }


    public static void setStringValue(String docName, String propname, String propvalue, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = xwiki.getDocument(docName, context);
        BaseClass bclass = doc.getxWikiClass();
        if (bclass==null) {
            bclass = new BaseClass();
            bclass.setName(docName);
        }
        if (bclass.getField(propname)==null) {
            StringClass propclass = new StringClass();
            propclass.setName(propname);
            propclass.setPrettyName(propname);
            propclass.setSize(80);
            propclass.setObject(bclass);
            bclass.addField(propname, propclass);
            doc.setxWikiClass(bclass);
        }

        BaseObject bobject = doc.getxWikiObject();
        if (bobject==null) {
          bobject = new BaseObject();
          doc.addObject(docName, bobject);
        }
        bobject.setName(docName);
        bobject.setClassName(bclass.getName());
        bobject.setStringValue(propname, propvalue);
        xwiki.saveDocument(doc, context);
    }

    public static void setLargeStringValue(String docName, String propname, String propvalue, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = xwiki.getDocument(docName, context);
        BaseClass bclass = doc.getxWikiClass();
        if (bclass==null) {
            bclass = new BaseClass();
            bclass.setName(docName);
        }
        if (bclass.getField(propname)==null) {
            TextAreaClass propclass = new TextAreaClass();
            propclass.setName(propname);
            propclass.setPrettyName(propname);
            propclass.setSize(80);
            propclass.setRows(20);
            propclass.setObject(bclass);
            bclass.addField(propname, propclass);
            doc.setxWikiClass(bclass);
        }

        BaseObject bobject = doc.getxWikiObject();
        if (bobject==null) {
          bobject = new BaseObject();
          doc.addObject(docName, bobject);
        }
        bobject.setName(docName);
        bobject.setClassName(bclass.getName());
        bobject.setLargeStringValue(propname, propvalue);
        xwiki.saveDocument(doc, context);
    }


    public static void setStringValue(String docName, String className, String propname, String propvalue, XWikiContext context) throws XWikiException {
        if (docName.equals(className)) {
            setStringValue(docName, propname, propvalue, context);
            return;
        }

        XWiki xwiki = context.getWiki();
        XWikiDocument classdoc = xwiki.getDocument(className, context);
        BaseClass bclass = classdoc.getxWikiClass();
        if (bclass==null) {
            bclass = new BaseClass();
            bclass.setName(className);
        }
        if (bclass.getField(propname)==null) {
            StringClass propclass = new StringClass();
            propclass.setName(propname);
            propclass.setPrettyName(propname);
            propclass.setSize(80);
            propclass.setObject(bclass);
            bclass.addField(propname, propclass);
            classdoc.setxWikiClass(bclass);
            xwiki.saveDocument(classdoc, context);
        }
        XWikiDocument doc = xwiki.getDocument(docName, context);
        BaseObject bobject = doc.getObject(className, 0);
        if (bobject==null) {
          bobject = new BaseObject();
          doc.addObject(className, bobject);
        }
        bobject.setName(docName);
        bobject.setClassName(bclass.getName());
        bobject.setStringValue(propname, propvalue);
        xwiki.saveDocument(doc, context);
    }

    public static void setIntValue(String docName, String propname, int propvalue, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = xwiki.getDocument(docName, context);
        BaseClass bclass = doc.getxWikiClass();
        if (bclass==null) {
            bclass = new BaseClass();
            bclass.setName(docName);
        }
        if (bclass.getField(propname)==null) {
            NumberClass propclass = new NumberClass();
            propclass.setName(propname);
            propclass.setNumberType("integer");
            propclass.setPrettyName(propname);
            propclass.setSize(5);
            propclass.setObject(bclass);
            bclass.addField(propname, propclass);
            doc.setxWikiClass(bclass);
        }
        BaseObject bobject = doc.getxWikiObject();
        if (bobject==null) {
          bobject = new BaseObject();
          doc.addObject(docName, bobject);
        }
        bobject.setName(docName);
        bobject.setClassName(bclass.getName());
        bobject.setIntValue(propname, propvalue);
        xwiki.saveDocument(doc, context);
    }

    public static void setIntValue(String docName, String className, String propname, int propvalue, XWikiContext context) throws XWikiException {
        if (docName.equals(className)) {
            setIntValue(docName, propname, propvalue, context);
            return;
        }

        XWiki xwiki = context.getWiki();
        XWikiDocument classdoc = xwiki.getDocument(className, context);
        BaseClass bclass = classdoc.getxWikiClass();
        if (bclass==null) {
                    bclass = new BaseClass();
                    bclass.setName(className);
        }
        if (bclass.getField(propname)==null) {
          NumberClass propclass = new NumberClass();
          propclass.setName(propname);
          propclass.setNumberType("integer");
          propclass.setPrettyName(propname);
          propclass.setSize(5);
          propclass.setObject(bclass);
          bclass.addField(propname, propclass);
          classdoc.setxWikiClass(bclass);
          xwiki.saveDocument(classdoc, context);
        }

        XWikiDocument doc = xwiki.getDocument(docName, context);
        BaseObject bobject = doc.getObject(className, 0);
        if (bobject==null) {
          bobject = new BaseObject();
          doc.addObject(className, bobject);
        }
        bobject.setName(docName);
        bobject.setClassName(bclass.getName());
        bobject.setIntValue(propname, propvalue);
        xwiki.saveDocument(doc, context);
    }


    public static void updateRight(XWiki xwiki, XWikiContext context, String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        XWikiDocument doc = xwiki.getDocument(fullname, context);
        BaseObject bobj = new BaseObject();
        bobj.setName(fullname);
        BaseClass bclass;
        if (global)
            bclass = xwiki.getGlobalRightsClass(context);
        else
            bclass = xwiki.getRightsClass(context);
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("users", user);
        bobj.setStringValue("groups", group);
        bobj.setStringValue("levels", level);
        if (allow)
            bobj.setIntValue("allow", 1);
        else
            bobj.setIntValue("allow", 0);
        doc.setObject(bclass.getName(), 0, bobj);
        xwiki.saveDocument(doc, context);
    }

    public static void addMember(XWiki xwiki, XWikiContext context, String fullname, String group) throws XWikiException {
        XWikiDocument doc = xwiki.getDocument(group, context);
        BaseClass bclass = xwiki.getGroupClass(context);
        BaseObject bobj = new BaseObject();
        bobj.setName(group);
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("member", fullname);
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("1 AdminGroup");
        xwiki.saveDocument(doc, context);        
    }

    public static void addManyMembers(XWiki xwiki, XWikiContext context, String fullname, String group, int nb) throws XWikiException {
        XWikiDocument doc = xwiki.getDocument(group, context);
        BaseClass bclass = xwiki.getGroupClass(context);

        for (int i=0;i<nb;i++) {
        String name = fullname + i;
        if (i==0)
         name = fullname;
        BaseObject bobj = new BaseObject();
        bobj.setName(group);
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("member", name);
        doc.addObject(bclass.getName(), bobj);
        }
        doc.setContent("1 AdminGroup");
        xwiki.saveDocument(doc, context);
    }

    public static XWikiDocument createDoc(String docname, String content, XWikiStoreInterface store, XWikiContext context) throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument();
        doc1.setFullName(docname, context);
        doc1.setContent(content);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1, context);
        return doc1;
    }

    public static void rmdirs(File dir) {
        File[] filelist = dir.listFiles();
        if (filelist!=null) {
            for (int i=0;i<filelist.length;i++) {
                File file = filelist[i];
                if (file.isDirectory())
                    rmdirs(file);
                else
                    filelist[i].delete();
            }
        }
        dir.delete();
    }

}
