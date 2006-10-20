package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.classes.DBTreeListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.XWikiContext;

public class DBTreeListMetaClass extends DBListMetaClass {

    public DBTreeListMetaClass() {
        super();
        setPrettyName("Database Tree List Class");
        setName(DBTreeListClass.class.getName());

        StringClass parentfield_class = new StringClass(this);
        parentfield_class.setName("parentField");
        parentfield_class.setPrettyName("Parent Field Name");
        parentfield_class.setSize(20);
        safeput("parentField", parentfield_class);                
    }

    public BaseCollection newObject(XWikiContext context) {
        return new DBTreeListClass();
    }
}
