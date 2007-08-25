package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.UsersClass;

public class UsersMetaClass extends ListMetaClass {
    public UsersMetaClass() {
        super();
        setPrettyName("Users List Class");
        setName(UsersClass.class.getName());

        BooleanClass uselist_class = new BooleanClass(this);
        uselist_class.setName("usesList");
        uselist_class.setPrettyName("Uses List");
        uselist_class.setDisplayType("yesno");
        safeput("usesList", uselist_class);
    }

    public BaseCollection newObject(XWikiContext context) {
        return new UsersClass();
    }
}
