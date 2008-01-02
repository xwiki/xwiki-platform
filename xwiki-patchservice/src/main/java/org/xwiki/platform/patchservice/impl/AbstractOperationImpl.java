package org.xwiki.platform.patchservice.impl;

import java.io.InputStream;
import java.util.Map;

import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

public abstract class AbstractOperationImpl implements RWOperation
{
    public static final String NODE_NAME = "operation";

    public static final String TYPE_ATTRIBUTE_NAME = "type";

    public static final String TEXT_NODE_NAME = "text";

    public static final String OBJECT_NODE_NAME = "object";

    public static final String ATTACHMENT_NODE_NAME = "attachment";

    public static final String PROPERTY_NODE_NAME = "property";

    private String type;

    /**
     * {@inheritDoc}
     */
    public boolean addObject(String objectClass)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean createType(String propertyType, Map properties)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteAttachment(String name)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteFromProperty(String property, String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteObject(String objectClass, int index)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteType(String propertyName)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean insert(String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean insertInProperty(String property, String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean modifyType(String propertyName, Map properties)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setAttachment(InputStream is)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setObjectProperty(String objectClass, String index, String propertyName,
        String value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setProperty(String property, String value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public String getType()
    {
        return this.type;
    }
}
