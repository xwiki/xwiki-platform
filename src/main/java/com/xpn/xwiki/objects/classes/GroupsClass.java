package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import org.apache.commons.lang.StringUtils;
import org.apache.ecs.xhtml.button;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;

import java.util.*;

public class GroupsClass extends ListClass {

    public GroupsClass(PropertyMetaClass wclass) {
        super("groupslist", "Groups List", wclass);
        setSize(6);
        setUsesList(true);
    }

    public GroupsClass() {
        this(null);
    }

    public List getList(XWikiContext context) {
        List list;
        try {
            list = context.getWiki().getGroupService(context).listAllGroups(context);
        } catch (XWikiException e) {
            // TODO add log exception
            list = new ArrayList();

        }

        return list;
    }

    public Map getMap(XWikiContext context) {
        return new HashMap();
    }

    public boolean isUsesList() {
        return (getIntValue("usesList")==1);
    }

    public void setUsesList(boolean usesList) {
        setIntValue("usesList",usesList ? 1 : 0);
    }

    public BaseProperty newProperty() {
        return new StringProperty();
    }

    public BaseProperty fromString(String value) {
        BaseProperty prop = newProperty();
        prop.setValue(value);
        return prop;
    }

    public BaseProperty fromStringArray(String[] strings) {
        List list = new ArrayList();
        for (int i = 0; i < strings.length; i++)
            list.add(strings[i]);
        BaseProperty prop = newProperty();
        prop.setValue(StringUtils.join(list.toArray(), ","));
        return prop;
    }

    public String getText(String value, XWikiContext context) {
        if (value.indexOf(":")!=-1)
         return value;
        else
         return value.substring(value.lastIndexOf(".")+1);
    }

    public static List getListFromString(String value) {
        List list = new ArrayList();
        if (value == null)
            return list;

        value = StringUtils.replace(value, "\\,", "%SEP%");
        String[] result = StringUtils.split(value, ", ");
        for (int i = 0; i < result.length; i++)
            list.add(StringUtils.replace(result[i], "%SEP%", ","));
        return list;
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        select select = new select(prefix + name, 1);
        select.setMultiple(isMultiSelect());
        select.setSize(getSize());

        List list;
        if (isUsesList())
            list = getList(context);
        else
            list = new ArrayList();

        List selectlist;

        BaseProperty prop =  (BaseProperty)object.safeget(name);
        if (prop==null) {
            selectlist = new ArrayList();
        } else {
            selectlist = getListFromString((String)prop.getValue());
        }

        list.remove("XWiki.XWikiAllGroup");
        list.add(0,"XWiki.XWikiAllGroup");
        if ((context.getWiki().isVirtual()&&(!context.getDatabase().equals("xwiki")))) {
            list.remove("xwiki:XWiki.XWikiAllGroup");
            list.add(1,"xwiki:XWiki.XWikiAllGroup");
        }

        // Add options from Set

        for (Iterator it=selectlist.iterator();it.hasNext();) {
            String value = it.next().toString();
            if (!list.contains(value))
                list.add(value);
        }
        for (Iterator it=list.iterator();it.hasNext();) {
            String value = it.next().toString();
            option option = new option(value, value);
            option.addElement(getText(value, context));
            if (selectlist.contains(value))
                option.setSelected(true);
            select.addElement(option);
        }

        buffer.append(select.toString());

         if (!isUsesList()) {
            input in = new input();
            in.setName(prefix + "newgroup");
            in.setID(prefix + "newgroup");
            in.setSize(15);
            buffer.append("<br />");
            buffer.append(in.toString());

            button button = new button();
            button.setTagText("Add");

            button.setOnClick("addGroup(this.form,'" + prefix + "'); return false;");
            buffer.append(button.toString());
        }

        input in = new input();
        in.setType("hidden");
        in.setName(prefix + name);
        buffer.append(in.toString());        
    }
}
