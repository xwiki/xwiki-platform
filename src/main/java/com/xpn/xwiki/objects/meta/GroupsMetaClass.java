package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.XWikiContext;

public class GroupsMetaClass extends ListMetaClass {
    public GroupsMetaClass() {
        super();
        setPrettyName("Groups List Class");
        setName(GroupsClass.class.getName());

    }

    public BaseCollection newObject(XWikiContext context) {
        return new GroupsMetaClass();
    }
}
