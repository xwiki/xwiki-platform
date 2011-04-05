package com.xpn.xwiki.objects.classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.dom4j.Element;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.XWikiRequest;

public class LevelsClass extends ListClass
{
    public LevelsClass(PropertyMetaClass wclass)
    {
        super("levelslist", "Levels List", wclass);
        setSize(6);
    }

    public LevelsClass()
    {
        this(null);
    }

    @Override
    public List<String> getList(XWikiContext context)
    {
        List<String> list;
        try {
            list = context.getWiki().getRightService().listAllLevels(context);
        } catch (XWikiException e) {
            // TODO add log exception
            list = new ArrayList<String>();
        }

        XWikiRequest req = context.getRequest();
        if (("editrights".equals(req.get("xpage")) || "rights".equals(req.get("editor")))
            && (!"1".equals(req.get("global")))) {
            list.remove("admin");
            list.remove("programming");
            list.remove("delete");
            list.remove("register");
        }
        return list;
    }

    @Override
    public Map<String, ListItem> getMap(XWikiContext context)
    {
        return new HashMap<String, ListItem>();
    }

    @Override
    public BaseProperty newProperty()
    {
        return new StringProperty();
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty prop = newProperty();
        prop.setValue(value);
        return prop;
    }

    @Override
    public BaseProperty fromStringArray(String[] strings)
    {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < strings.length; i++) {
            if (!strings[i].trim().equals("")) {
                list.add(strings[i]);
            }
        }
        BaseProperty prop = newProperty();
        prop.setValue(StringUtils.join(list, ","));
        return prop;
    }

    public String getText(String value, XWikiContext context)
    {
        return value;
    }

    public static List<String> getListFromString(String value)
    {
        return getListFromString(value, ",", false);
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        select select = new select(prefix + name, 1);
        select.setMultiple(isMultiSelect());
        select.setSize(getSize());
        select.setName(prefix + name);
        select.setID(prefix + name);
        select.setDisabled(isDisabled());

        List<String> list = getList(context);
        List<String> selectlist;

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop == null) {
            selectlist = new ArrayList<String>();
        } else {
            selectlist = getListFromString((String) prop.getValue());
        }

        // Add options from Set
        for (String value : list) {
            String display = getText(value, context);
            option option = new option(display, value);
            option.addElement(display);
            // If we don't have this option in the list then add it
            if (!list.contains(value)) {
                list.add(value);
            }
            if (selectlist.contains(value)) {
                option.setSelected(true);
            }
            select.addElement(option);
        }

        buffer.append(select.toString());
        input in = new input();
        in.setType("hidden");
        in.setName(prefix + name);
        in.setDisabled(isDisabled());
        buffer.append(in.toString());
    }

    @Override
    public BaseProperty newPropertyfromXML(Element ppcel)
    {
        String value = ppcel.getText();
        return fromString(value);
    }
}
