package com.xpn.xwiki.objects;

import java.util.List;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.BaseCollection} class.
 * 
 * @version $Id: $
 */
public aspect BaseCollectionCompatibiityAspect
{
    /**
     * @deprecated use setStringListValue or setDBStringListProperty
     */
    @Deprecated
    public void BaseCollection.setListValue(String name, List value)
    {
        ListProperty property = (ListProperty) safeget(name);
        if (property == null)
            property = new StringListProperty();
        property.setValue(value);
        safeput(name, property);
    }
}
