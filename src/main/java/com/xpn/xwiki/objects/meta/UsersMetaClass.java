package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.UsersClass;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.XWikiContext;

public class UsersMetaClass extends ListMetaClass {
    public UsersMetaClass() {
        super();
        setPrettyName("Users List Class");
        setName(UsersClass.class.getName());

    }

    public BaseCollection newObject(XWikiContext context) {
        return new UsersMetaClass();
    }
}
