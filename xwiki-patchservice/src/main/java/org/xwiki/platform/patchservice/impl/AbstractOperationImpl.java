package org.xwiki.platform.patchservice.impl;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public abstract class AbstractOperationImpl implements RWOperation
{
    public static final String NODE_NAME = "operation";
    public static final String TYPE_ATTRIBUTE_NAME = "type";
    public static final String TEXT_NODE_NAME = "text";
    public static final String OBJECT_NODE_NAME = "object";
    public static final String ATTACHMENT_NODE_NAME = "attachment";
    public static final String PROPERTY_NODE_NAME = "property";

    private Date time;

    private String type;

    public boolean addObject(String objectClass)
    {
        return false;
    }

    public boolean createType(String propertyType, Map properties)
    {
        return false;
    }

    public boolean delete(String text, int position)
    {
        return false;
    }

    public boolean deleteAttachment(String name)
    {
        return false;
    }

    public boolean deleteFromProperty(String property, String text, int position)
    {
        return false;
    }

    public boolean deleteObject(String objectClass, int index)
    {
        return false;
    }

    public boolean deleteType(String propertyName)
    {
        return false;
    }

    public boolean insert(String text, int position)
    {
        return false;
    }

    public boolean insertInProperty(String property, String text, int position)
    {
        return false;
    }

    public boolean modifyType(String propertyName, Map properties)
    {
        return false;
    }

    public boolean setAttachment(InputStream is)
    {
        return false;
    }

    public boolean setObjectProperty(String objectClass, String index, String propertyName,
        String value)
    {
        return false;
    }

    public boolean setProperty(String property, String value)
    {
        return false;
    }

    public void setTime(Date date)
    {
        this.time = date;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Date getTime()
    {
        return this.time;
    }

    public String getType()
    {
        return this.type;
    }

    public abstract void apply(XWikiDocument doc) throws XWikiException;

    public abstract void fromXml(Element e) throws XWikiException;

    public abstract Element toXml(Document doc) throws XWikiException;
}
