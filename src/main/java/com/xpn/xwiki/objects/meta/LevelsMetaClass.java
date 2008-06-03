package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.LevelsClass;

public class LevelsMetaClass extends ListMetaClass
{
    public LevelsMetaClass()
    {
        super();
        setPrettyName("Access Right Levels");
        setName(LevelsClass.class.getName());
    }

    public BaseCollection newObject(XWikiContext context)
    {
        return new LevelsClass();
    }
}
