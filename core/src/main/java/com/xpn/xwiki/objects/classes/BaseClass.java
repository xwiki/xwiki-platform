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
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.xhtml.select;
import org.apache.ecs.xhtml.option;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.SAXReader;

import java.io.StringReader;
import java.util.*;


public class BaseClass extends BaseCollection implements ClassInterface {
    private static final Log log = LogFactory.getLog(BaseClass.class);
    private String customMapping;
    private String customClass;
    private String defaultWeb;
    private String defaultViewSheet;
    private String defaultEditSheet;
    private String validationScript;
    private String nameField;

    // This insures natural ordering between properties
    public void addField(String name, PropertyInterface element) {
        Set properties = getPropertyList();
        if (!properties.contains(name)) {
            if (((BaseCollection)element).getNumber()==0)
                ((BaseCollection)element).setNumber(properties.size()+1);
        }
        super.addField(name, element);
    }

    public PropertyInterface get(String name) {
        return safeget(name);
    }

    public void put(String name, PropertyInterface property) {
        safeput(name, property);
    }

    public BaseProperty fromString(String value) {
        return null;  //To change body of implemented methods use Options | File Templates.
    }

    public BaseCollection newObject(XWikiContext context) throws XWikiException {
        BaseObject bobj = newCustomClassInstance(context);
        bobj.setClassName(getName());
        return bobj;
    }

    public BaseCollection fromMap(Map map, XWikiContext context) throws XWikiException {
        BaseCollection object = newObject(context);
        return fromMap(map, object);
    }

    public BaseCollection fromMap(Map map, BaseCollection object) {
        object.setClassName(getName());
        Iterator classit = getFieldList().iterator();
        while (classit.hasNext()) {
            PropertyClass property = (PropertyClass) classit.next();
            String name = property.getName();
            Object formvalues = map.get(name);
            if (formvalues!=null) {
                BaseProperty objprop;
                if (formvalues instanceof String[]) {
                    objprop = property.fromStringArray(((String[])formvalues));
                } else {
                    objprop = property.fromString(formvalues.toString());
                }
                if (objprop!=null) {
                    objprop.setObject(object);
                    object.safeput(name, objprop);
                }
            }
        }
        return object;
    }

    public BaseCollection fromValueMap(Map map, BaseCollection object) {
        object.setClassName(getName());
        Iterator classit = getFieldList().iterator();
        while (classit.hasNext()) {
            PropertyClass property = (PropertyClass) classit.next();
            String name = property.getName();
            Object formvalue = map.get(name);
            if (formvalue!=null) {
                BaseProperty objprop;
                objprop = property.fromValue(formvalue);
                if (objprop!=null) {
                    objprop.setObject(object);
                    object.safeput(name, objprop);
                }
            }
        }
        return object;
    }

    public Object clone() {
        BaseClass bclass = (BaseClass) super.clone();
        bclass.setCustomClass(getCustomClass());
        bclass.setCustomMapping(getCustomMapping());
        bclass.setDefaultWeb(getDefaultWeb());
        bclass.setDefaultViewSheet(getDefaultViewSheet());
        bclass.setDefaultEditSheet(getDefaultEditSheet());
        bclass.setNameField(getNameField());
        return bclass;
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;

        BaseClass bclass = (BaseClass) obj;

        if (!getCustomClass().equals(bclass.getCustomClass()))
            return false;

        if (!getCustomMapping().equals(bclass.getCustomMapping()))
            return false;

        if (!getDefaultViewSheet().equals(bclass.getDefaultViewSheet()))
            return false;

        if (!getDefaultEditSheet().equals(bclass.getDefaultEditSheet()))
            return false;

        if (!getDefaultWeb().equals(bclass.getDefaultWeb()))
            return false;

        if (!getNameField().equals(bclass.getNameField()))
            return false;

        return true;
    }

    public void merge(BaseClass bclass) {
    }

    public Element toXML(BaseClass bclass) {
        return toXML();
    }

    public Element toXML() {
        Element cel = new DOMElement("class");

        Element el = new DOMElement("name");
        el.addText((getName()==null) ? "" : getName());
        cel.add(el);

        el = new DOMElement("customClass");
        el.addText((getCustomClass()==null) ? "" : getCustomClass());
        cel.add(el);

        el = new DOMElement("customMapping");
        el.addText((getCustomMapping()==null) ? "" : getCustomMapping());
        cel.add(el);

        el = new DOMElement("defaultViewSheet");
        el.addText((getDefaultViewSheet()==null) ? "" : getDefaultViewSheet());
        cel.add(el);

        el = new DOMElement("defaultEditSheet");
        el.addText((getDefaultEditSheet()==null) ? "" : getDefaultEditSheet());
        cel.add(el);

        el = new DOMElement("defaultWeb");
        el.addText((getDefaultWeb()==null) ? "" : getDefaultWeb());
        cel.add(el);

        el = new DOMElement("nameField");
        el.addText((getNameField()==null) ? "" : getNameField());
        cel.add(el);

        Iterator it = getFieldList().iterator();
        while (it.hasNext()) {
            PropertyClass bprop = (PropertyClass)it.next();
            cel.add(bprop.toXML());
        }
        return cel;
    }

    public void fromXML(Element cel) throws XWikiException {
        try {
            int j = 1;
            setName(cel.element("name").getText());
            Element cclel = cel.element("customClass");
            if (cclel!=null) {
                setCustomClass(cclel.getText());
                j++;
            }
            Element cmapel = cel.element("customMapping");
            if (cmapel!=null) {
                setCustomMapping(cmapel.getText());
                j++;
            }
            Element cdvsel = cel.element("defaultViewSheet");
            if (cdvsel!=null) {
                setDefaultViewSheet(cdvsel.getText());
                j++;
            }
            Element cdesel = cel.element("defaultEditSheet");
            if (cdesel!=null) {
                setDefaultViewSheet(cdesel.getText());
                j++;
            }
            Element cdwel = cel.element("defaultWeb");
            if (cdwel!=null) {
                setDefaultWeb(cdwel.getText());
                j++;
            }
            Element cnfel = cel.element("nameField");
            if (cnfel!=null) {
                setNameField(cnfel.getText());
                j++;
            }

            List list = cel.elements();
            for (int i=j;i<list.size();i++) {
                Element pcel = (Element) list.get(i);
                String name = pcel.getName();
                String classType = pcel.element("classType").getText();
                PropertyClass property = (PropertyClass) Class.forName(classType).newInstance();
                property.setName(name);
                property.setObject(this);
                property.fromXML(pcel);
                safeput(name, property);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES, XWikiException.ERROR_XWIKI_CLASSES_PROPERTY_CLASS_INSTANCIATION, "Error instanciating property class", e, null);
        }
    }

    public void fromXML(String xml) throws XWikiException {
        SAXReader reader = new SAXReader();
        Document domdoc;

        if ((xml==null)||(xml.trim().equals("")))
         return;

        xml = xml.replaceAll("<>", "<unknown>");
        xml = xml.replaceAll("</>", "</unknown>");

        try {
            StringReader in = new StringReader(xml);
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            Object[] args = { xml };
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING, "Error parsing xml {0}", e, args);
        }

        Element docel = domdoc.getRootElement();
        if (docel!=null) {
            fromXML(docel);
        }
    }

    public boolean addTextField(String fieldName, String fieldPrettyName, int size) {
        if (get(fieldName)==null) {
            StringClass text_class = new StringClass();
            text_class.setName(fieldName);
            text_class.setPrettyName(fieldPrettyName);
            text_class.setSize(size);
            text_class.setObject(this);
            put(fieldName, text_class);
            return true;
        }
        return false;
    }

    public boolean addPasswordField(String fieldName, String fieldPrettyName, int size) {
        if (get(fieldName)==null) {
            PasswordClass text_class = new PasswordClass();
            text_class.setName(fieldName);
            text_class.setPrettyName(fieldPrettyName);
            text_class.setSize(size);
            text_class.setObject(this);
            put(fieldName, text_class);
            return true;
        }
        return false;
    }

    public boolean addBooleanField(String fieldName, String fieldPrettyName, String displayType) {
        if (get(fieldName)==null) {
            BooleanClass boolean_class = new BooleanClass();
            boolean_class.setName(fieldName);
            boolean_class.setPrettyName(fieldPrettyName);
            boolean_class.setDisplayType(displayType);
            boolean_class.setObject(this);
            put(fieldName, boolean_class);
            return true;
        }
        return false;
    }

    public boolean addUsersField(String fieldName, String fieldPrettyName) {
        return addUsersField(fieldName, fieldPrettyName, 5);
    }

    public boolean addUsersField(String fieldName, String fieldPrettyName,int size) {
        if (get(fieldName) == null) {
            UsersClass users_class = new UsersClass();
            users_class.setName(fieldName);
            users_class.setPrettyName(fieldPrettyName);
            users_class.setSize(size);
            users_class.setMultiSelect(true);
            users_class.setObject(this);
            put(fieldName, users_class);
            return true;
        }
        return false ;
    }

    public boolean addLevelsField(String fieldName, String fieldPrettyName) {
        return addLevelsField(fieldName, fieldPrettyName, 3);
    }

    public boolean addLevelsField(String fieldName, String fieldPrettyName,int size) {
        if (get(fieldName) == null) {
            LevelsClass levels_class = new LevelsClass();
            levels_class.setName(fieldName);
            levels_class.setPrettyName(fieldPrettyName);
            levels_class.setSize(size);
            levels_class.setMultiSelect(true);
            levels_class.setObject(this);
            put(fieldName, levels_class);
            return true;
        }
        return false ;
    }

    public boolean addGroupsField(String fieldName, String fieldPrettyName) {
        return addGroupsField(fieldName, fieldPrettyName, 5);
    }

    public boolean addGroupsField(String fieldName, String fieldPrettyName,int size) {
        if (get(fieldName) == null) {
            GroupsClass groups_class = new GroupsClass();
            groups_class.setName(fieldName);
            groups_class.setPrettyName(fieldPrettyName);
            groups_class.setSize(size);
            groups_class.setMultiSelect(true);
            groups_class.setObject(this);
            put(fieldName, groups_class);
            return true;
        }
        return false ;
    }

    public boolean addTemplateField(String fieldName, String fieldPrettyName) {
        return addTextAreaField(fieldName, fieldPrettyName, 80, 15);
    }
    public boolean addTextAreaField(String fieldName, String fieldPrettyName, int cols, int rows) {
        if (get(fieldName)==null) {
            TextAreaClass template_class = new TextAreaClass();
            template_class.setName(fieldName);
            template_class.setPrettyName(fieldPrettyName);
            template_class.setSize(cols);
            template_class.setRows(rows);
            template_class.setObject(this);
            put(fieldName, template_class);
            return true;
        }
        return false;
    }

    public boolean addStaticListField(String fieldName, String fieldPrettyName, String values) {
        return addStaticListField(fieldName, fieldPrettyName, 1, false, values);
    }

    public boolean addStaticListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect, String values) {
        if (get(fieldName)==null) {
            StaticListClass list_class = new StaticListClass();
            list_class.setName(fieldName);
            list_class.setPrettyName(fieldPrettyName);
            list_class.setSize(size);
            list_class.setMultiSelect(multiSelect);
            list_class.setValues(values);
            list_class.setObject(this);
            put(fieldName, list_class);
            return true;
        }
        return false;
    }

    public boolean addNumberField(String fieldName, String fieldPrettyName, int size, String type) {
        if (get(fieldName)==null) {
            NumberClass number_class = new NumberClass();
            number_class.setName(fieldName);
            number_class.setPrettyName(fieldPrettyName);
            number_class.setSize(size);
            number_class.setNumberType(type);
            number_class.setObject(this);
            put(fieldName, number_class);
            return true;
        }
        return false;
    }

    public boolean addDateField(String fieldName, String fieldPrettyName) {
        return addDateField(fieldName, fieldPrettyName, null, 1);
    }

    public boolean addDateField(String fieldName, String fieldPrettyName, String dformat) {
        return addDateField(fieldName, fieldPrettyName, dformat, 1);
    }

    public boolean addDateField(String fieldName, String fieldPrettyName, String dformat, int emptyIsToday) {
        if (get(fieldName)==null) {
            DateClass date_class = new DateClass();
            date_class.setName(fieldName);
            date_class.setPrettyName(fieldPrettyName);
            if (dformat!=null)
                date_class.setDateFormat(dformat);
            date_class.setObject(this);
            date_class.setEmptyIsToday(emptyIsToday);
            put(fieldName, date_class);
            return true;
        }
        return false;
    }

    public boolean addDBListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect, String sql) {
        if (get(fieldName)==null) {
            DBListClass list_class = new DBListClass();
            list_class.setName(fieldName);
            list_class.setPrettyName(fieldPrettyName);
            list_class.setSize(size);
            list_class.setMultiSelect(multiSelect);
            list_class.setSql(sql);
            list_class.setObject(this);
            put(fieldName, list_class);
            return true;
        }
        return false;
    }

    public void setCustomMapping(String customMapping) {
        this.customMapping = customMapping;
    }

    public String getCustomMapping() {
        if ("XWiki.XWikiPreferences".equals(getName()))
          return "internal";
        if (customMapping==null)
          return "";
        return customMapping;
    }

    public boolean hasCustomMapping() {
        String cMapping = getCustomMapping();
        return (cMapping!=null)&&(!"".equals(cMapping));
    }

    public boolean hasExternalCustomMapping() {
        String cMapping = getCustomMapping();
        return (cMapping!=null)&&(!"".equals(cMapping))
                &&(!"internal".equals(cMapping));
    }

    public boolean hasInternalCustomMapping() {
        return "internal".equals(customMapping);
    }

    public boolean isCustomMappingValid(XWikiContext context) throws XWikiException {
        return isCustomMappingValid(getCustomMapping(), context);
    }

    public boolean isCustomMappingValid(String custommapping1, XWikiContext context) throws XWikiException {
        if ((custommapping1!=null)&&(custommapping1.trim().length()>0))
          return context.getWiki().getStore().isCustomMappingValid(this, custommapping1, context);
        else
          return true;
    }

    public List getCustomMappingPropertyList(XWikiContext context) {
        String custommapping1 = getCustomMapping();
        if ((custommapping1!=null)&&(custommapping1.trim().length()>0))
          return context.getWiki().getStore().getCustomMappingPropertyList(this);
        else
          return new ArrayList();
    }

    public void setCustomClass(String customClass) {
        this.customClass = customClass;
    }

    public String getCustomClass() {
        if (customClass==null)
         return "";
        return customClass;
    }

    public BaseObject newCustomClassInstance(XWikiContext context) throws XWikiException {
        String customClass = getCustomClass();
        try {
            if ((customClass==null)||(customClass.equals("")))
             return new BaseObject();
            else
             return (BaseObject) Class.forName(getCustomClass()).newInstance();
        } catch (Exception e) {
            Object[] args = {customClass};
            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                    XWikiException.ERROR_XWIKI_CLASSES_CUSTOMCLASSINVOCATIONERROR,
                    "Cannot instanciate custom class {0}", e, args);
        }
    }

    public static BaseObject newCustomClassInstance(String className, XWikiContext context) throws XWikiException { {
        BaseClass bclass = context.getWiki().getClass(className, context);
        BaseObject object = (bclass==null) ? new BaseObject() : bclass.newCustomClassInstance(context);
        return object;
    }
    }

    public String getDefaultWeb() {
        if (defaultWeb==null)
          return "";
        return defaultWeb;
    }

    public void setDefaultWeb(String defaultWeb) {
        this.defaultWeb = defaultWeb;
    }

    public String getDefaultViewSheet() {
        if (defaultViewSheet==null)
          return "";
        return defaultViewSheet;
    }

    public void setDefaultViewSheet(String defaultViewSheet) {
        this.defaultViewSheet = defaultViewSheet;
    }

    public String getDefaultEditSheet() {
        if (defaultEditSheet==null)
          return "";
        return defaultEditSheet;
    }

    public void setDefaultEditSheet(String defaultEditSheet) {
        this.defaultEditSheet = defaultEditSheet;
    }

    public String getNameField() {
        if (nameField==null)
          return "";
        return nameField;
    }

    public void setNameField(String nameField) {
        this.nameField = nameField;
    }

    public String makeQuery(XWikiCriteria query) {
        List criteriaList = new ArrayList();
        Iterator classit = getFieldList().iterator();
        while (classit.hasNext()) {
            PropertyClass property = (PropertyClass) classit.next();
            String name = property.getName();
            Map map = query.getParameters(getName() + "_" + name);
            if (map.size()>0) {
              property.makeQuery(map, "", query, criteriaList);
            }
        }
        return StringUtils.join(criteriaList.toArray(), " and ");
    }

    public String displaySearchColumns(String prefix, XWikiQuery query, XWikiContext context) {
        select select = new select(prefix + "searchcolumns", 5);
        select.setMultiple(true);
        select.setName(prefix + "searchcolumns");
        select.setID(prefix + "searchcolumns");

        List list = Arrays.asList(getPropertyNames());
        Map prettynamesmap = new HashMap();
        for (int i=0;i<list.size();i++) {
            String propname = (String) list.get(i);
            list.set(i, prefix + propname);
            prettynamesmap.put(prefix + propname, ((PropertyClass)get(propname)).getPrettyName());
        }

        List selectlist = query.getDisplayProperties();

        // Add options from Set
        for (Iterator it=list.iterator();it.hasNext();) {
            String value = it.next().toString();
            String displayValue = (String) prettynamesmap.get(value);
            option option = new option(displayValue, displayValue);
            option.addElement(displayValue);
            option.setValue(value);
            if (selectlist.contains(value))
                option.setSelected(true);
            select.addElement(option);
        }

        return select.toString();
    }

    public void setValidationScript(String validationScript) {
        this.validationScript = validationScript;
    }

    public String getValidationScript() {
        return validationScript;
    }

}
