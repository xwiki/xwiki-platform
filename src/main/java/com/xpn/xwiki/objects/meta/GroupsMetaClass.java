package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.GroupsClass;

public class GroupsMetaClass extends ListMetaClass {
    public GroupsMetaClass() {
        super();
        setPrettyName("Groups List Class");
        setName(GroupsClass.class.getName());

        BooleanClass uselist_class = new BooleanClass(this);
        uselist_class.setName("usesList");
        uselist_class.setPrettyName("Uses List");
        uselist_class.setDisplayType("yesno");
        safeput("usesList", uselist_class);

    }

    public BaseCollection newObject(XWikiContext context) {
        return new GroupsMetaClass();
    }
}
