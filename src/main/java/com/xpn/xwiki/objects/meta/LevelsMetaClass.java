package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.LevelsClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.XWikiContext;

public class LevelsMetaClass extends ListMetaClass {
    public LevelsMetaClass() {
        super();
        setPrettyName("Level Class");
        setName(LevelsClass.class.getName());
    }

    public BaseCollection newObject(XWikiContext context) {
        return new LevelsMetaClass();
    }
}
