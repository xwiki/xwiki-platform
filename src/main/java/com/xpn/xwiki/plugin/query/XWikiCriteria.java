package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.util.Util;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 7 sept. 2006
 * Time: 20:00:39
 * To change this template use File | Settings | File Templates.
 */
public class XWikiCriteria {
    protected HashMap params = new HashMap();

    public Object getParameter(String field) {
        return params.get(field);
    }

    public Map getParameters(String field) {
       return Util.getSubMap(params, field);
    }

    public void setParam(String field, Object value) {
        params.put(field, value);
    }

    public Set getClasses() {
        Set set = new HashSet();
        for(Iterator it=params.keySet().iterator();it.hasNext();) {
            String key = (String) it.next();
            String objname = key.substring(0, key.indexOf('_'));
            if ((!objname.equals("")&&(!objname.equals("doc"))))
             set.add(objname);
        }
        return set;
    }
}
