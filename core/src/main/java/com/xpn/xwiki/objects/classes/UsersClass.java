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
import org.dom4j.Element;

import java.util.*;

public class UsersClass extends ListClass {

    public UsersClass(PropertyMetaClass wclass) {
        super("userslist", "Users List", wclass);
        setSize(6);
        setUsesList(true);
    }

    public UsersClass() {
        this(null);
    }

    public List getList(XWikiContext context) {
        List list;
        try {
            list = context.getWiki().getGroupService(context).listMemberForGroup(null, context);
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
        return context.getWiki().getUserName(value, null, false, context);
    }

    public static List getListFromString(String value) {
        List list = new ArrayList();
        if (value == null)
            return list;

        String val = StringUtils.replace(value, "\\,", "%SEP%");
        String[] result = StringUtils.split(value, ", ");
        for (int i = 0; i < result.length; i++)
            list.add(StringUtils.replace(result[i], "%SEP%", ","));
        return list;
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        select select = new select(prefix + name, 1);
        select.setMultiple(isMultiSelect());
        select.setSize(getSize());
        select.setID(prefix + name);
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

        // Add options from Set

        for (Iterator it=selectlist.iterator();it.hasNext();) {
            String value = it.next().toString();
            if (!list.contains(value))
                list.add(value);
        }

        // Make sure we have guest
        list.remove("XWiki.XWikiGuest");
        list.add(0,"XWiki.XWikiGuest");

        // Add options from Set
        for (Iterator it=list.iterator();it.hasNext();) {
            String value = it.next().toString();
            String display = getText(value, context);
            option option = new option(display, value);
            option.addElement(display);
            if (selectlist.contains(value))
                option.setSelected(true);
            select.addElement(option);
        }

        buffer.append(select.toString());

        if (!isUsesList()) {
            input in = new input();
            in.setName(prefix+"newuser");
            in.setSize(15);
            buffer.append("<br />");
            buffer.append(in.toString());

            button button = new button();
            button.setTagText("Add");

            button.setOnClick("addUser(this.form,'" + prefix + "'); return false;");
            buffer.append(button.toString());
        }

        input in = new input();
        in.setType("hidden");
        in.setName(prefix + name);
        buffer.append(in.toString());
    }

    public BaseProperty newPropertyfromXML(Element ppcel) {
        String value = ppcel.getText();
        return fromString(value);
    }
}
