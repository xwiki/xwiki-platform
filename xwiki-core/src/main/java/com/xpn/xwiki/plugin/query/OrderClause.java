package com.xpn.xwiki.plugin.query;

public class OrderClause extends Object {
    public final static int ASC = 1;
    public final static int DESC = 2;

    private String property;
    private int order;

    public OrderClause(String property, int ascdesc) {
        setProperty(property);
        setOrder(ascdesc);
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
