package com.xpn.xwiki.objects.classes;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 19 oct. 2006
 * Time: 15:23:56
 * To change this template use File | Settings | File Templates.
 */
public class ListItem {
    private String id = "";
    private String value = "";
    private String parent = "";

    public ListItem(String id) {
        this.setId(id);
        this.setValue(id);
    }

    public ListItem(String id, String value) {
        this(id);
        this.setValue(value);
    }
    public ListItem(String id, String value, String parent) {
        this(id, value);
        this.setParent(parent);
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
}
