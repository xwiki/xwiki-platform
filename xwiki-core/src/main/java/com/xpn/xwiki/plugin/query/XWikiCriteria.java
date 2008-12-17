package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.util.Util;

import java.util.*;

public class XWikiCriteria {
    protected Map<String, Object> params = new HashMap<String, Object>();

    public Object getParameter(String field)
    {
        return params.get(field);
    }

    public Map<String, Object> getParameters(String field)
    {
        return Util.getSubMap(params, field);
    }

    public void setParam(String field, Object value)
    {
        params.put(field, value);
    }

    public Set<String> getClasses()
    {
        Set<String> set = new HashSet<String>();
        for(Iterator<String> it=params.keySet().iterator();it.hasNext();) {
            String key = (String) it.next();
            String objname = key.substring(0, key.indexOf('_'));
            if ((!objname.equals("")&&(!objname.equals("doc"))))
                set.add(objname);
        }
        return set;
    }
}
