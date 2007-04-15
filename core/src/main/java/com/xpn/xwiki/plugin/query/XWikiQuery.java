package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiRequest;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 7 sept. 2006
 * Time: 20:00:39
 * To change this template use File | Settings | File Templates.
 */
public class XWikiQuery extends XWikiCriteria {
    protected boolean distinct = false;
    protected List displayProperties = new ArrayList();
    protected List addProperties = new ArrayList();
    protected List groupbyProperties = new ArrayList();
    protected List orderProperties = new ArrayList();

    public XWikiQuery() {
        super();
    }
    
    public XWikiQuery(XWikiRequest request, String className, XWikiContext context) throws XWikiException {
        super();
        String[] columns = request.getParameterValues(className + "_" + "searchcolumns");
        setDisplayProperties(columns);
        String[] order = request.getParameterValues(className + "_" + "searchorder");
        setOrderProperties(order);
        BaseClass bclass = context.getWiki().getDocument(className, context).getxWikiClass();
        Set properties = bclass.getPropertyList();
        Iterator propid = properties.iterator();
        while (propid.hasNext()) {
            String propname = (String) propid.next();
            Map map = Util.getObject(request, className + "_" + propname);
            ((PropertyClass)(bclass.get(propname))).fromSearchMap(this, map);
        }
    }

    private void setOrderProperties(String[] order) {
        orderProperties.clear();
        if (order!=null) {
         for (int i=0;i<order.length;i++) {
            OrderClause oclause = new OrderClause(order[i], OrderClause.ASC);
            orderProperties.add(oclause);
         }
        }
    }

    public void reset() {
        displayProperties = new ArrayList();
        addProperties = new ArrayList();
        groupbyProperties = new ArrayList();
        orderProperties = new ArrayList();
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDisplayProperties(List properties) {
         displayProperties = properties;
    }

    public void setDisplayProperties(String[] properties) {
        if (properties!=null)
         displayProperties = Arrays.asList(properties);
    }

    public List getDisplayProperties() {
        return displayProperties;
    }

    public void setDisplayProperty(String property) {
        displayProperties = new ArrayList();
        displayProperties.add(property);
    }

    public void addProperty(String property) {
        addProperties.add(property);
    }

    public void addGroupByProperty(String property) {
        groupbyProperties.add(property);
    }

    public void addOrderProperty(String property) {
        orderProperties.add(new OrderClause(property, OrderClause.DESC));
    }

    public void addOrderProperty(String property, String order) {
        addOrderProperty(property, order.equals("desc") ? OrderClause.DESC : OrderClause.ASC);
    }

    public void addOrderProperty(String property, int order) {
        orderProperties.add(new OrderClause(property, order));
    }

    public List getOrderProperties() {
        return orderProperties;
    }
}
