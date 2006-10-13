package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.XWikiRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LevelsClass extends ListClass {

    public LevelsClass(PropertyMetaClass wclass) {
        super("levelslist", "Levels List", wclass);
        setSize(6);
    }

    public LevelsClass() {
        this(null);
    }

    public List getList(XWikiContext context) {
        List list;
        try {
            list = context.getWiki().getRightService().listAllLevels(context);
        } catch (XWikiException e) {
            // TODO add log exception
            list = new ArrayList();

        }

        XWikiRequest req = context.getRequest();
        if (("editrights".equals(req.get("xpage")))
		&&(!"1".equals(req.get("global")))) {
            list.remove("admin");
            list.remove("programming");
            list.remove("delete");
            list.remove("register");
        }

        return list;
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
        return value;
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

        List list = getList(context);
        List selectlist;

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop == null) {
            selectlist = new ArrayList();
        } else {
            selectlist = getListFromString((String) prop.getValue());
        }

        // Add options from Set
        for (Iterator it = list.iterator(); it.hasNext();) {
            String value = it.next().toString();
            option option = new option(value, value);
            option.addElement(getText(value, context));
            // If we don't have this option in the list then add it
            if (!list.contains(value))
                    list.add(value);
            if (selectlist.contains(value))
                option.setSelected(true);
            select.addElement(option);
        }

        buffer.append(select.toString());
        input in = new input();
        in.setType("hidden");
        in.setName(prefix + name);
        in.setID(prefix + name);
        buffer.append(in.toString());
    }
}
