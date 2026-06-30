/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.objects;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.web.Utils;

/**
 * Hold information about the diff between objects or properties.
 *
 * @version $Id$
 */
public class ObjectDiff
{
    /**
     * Represents action for a property added.
     */
    public static final String ACTION_PROPERTYADDED = "added";

    /**
     * Represents action for a property modified.
     */
    public static final String ACTION_PROPERTYCHANGED = "changed";

    /**
     * Represents action for a property removed.
     */
    public static final String ACTION_PROPERTYREMOVED = "removed";

    /**
     * Represents action for an entire object added.
     */
    public static final String ACTION_OBJECTADDED = "object-added";

    /**
     * Represents action for an entire object removed.
     */
    public static final String ACTION_OBJECTREMOVED = "object-removed";

    private DocumentReferenceResolver<String> currentDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "current");

    private EntityReferenceSerializer<String> localEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.TYPE_STRING, "local");

    private DocumentReference xClassReference;

    private int number;

    private String guid;

    private String propName;

    private String propType;

    private Object prevValue;

    private Object newValue;

    private String action;

    private boolean sensitive;

    /**
     * Deprecated constructor.
     *
     * @param className the name of the class of the object the diff is performed for.
     * @param number the number of the object the diff is performed for.
     * @param action the actual action that has been done on the object between the versions involved in the diff.
     * @param propName the name of the property involved in the action.
     * @param prevValue the previous value of the property.
     * @param newValue the new value of the property.
     */
    @Deprecated
    public ObjectDiff(String className, int number, String action, String propName, Object prevValue, Object newValue)
    {
        this(className, number, "", action, propName, "", prevValue, newValue);
    }

    /**
     * Deprecated constructor.
     *
     * @param className the name of the class of the object the diff is performed for.
     * @param number the number of the object the diff is performed for.
     * @param guid the guid of the object the diff is performed for.
     * @param action the actual action that has been done on the object between the versions involved in the diff.
     * @param propName the name of the property involved in the action.
     * @param propType the type of the property involved in the action.
     * @param prevValue the previous value of the property.
     * @param newValue the new value of the property.
     */
    @Deprecated
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ObjectDiff(String className, int number, String guid, String action, String propName, String propType,
        Object prevValue, Object newValue)
    {
        this.setClassName(className);
        this.setNumber(number);
        this.setGuid(guid);
        this.setAction(action);
        this.setPropName(propName);
        this.setPropType(propType);
        this.setPrevValue(prevValue);
        this.setNewValue(newValue);
    }

    /**
     * Default constructor.
     *
     * @param xClassReference the reference of the class of the object the diff is performed for.
     * @param number the number of the object the diff is performed for.
     * @param guid the guid of the object the diff is performed for.
     * @param action the actual action that has been done on the object between the versions involved in the diff.
     * @param propName the name of the property involved in the action.
     * @param propType the type of the property involved in the action.
     * @param prevValue the previous value of the property.
     * @param newValue the new value of the property.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ObjectDiff(DocumentReference xClassReference, int number, String guid, String action, String propName,
        String propType, Object prevValue, Object newValue)
    {
        this(xClassReference, number, guid, action, propName, propType, prevValue, newValue, false);
    }

    /**
     * Default constructor.
     *
     * @param xClassReference the reference of the class of the object the diff is performed for.
     * @param number the number of the object the diff is performed for.
     * @param guid the guid of the object the diff is performed for.
     * @param action the actual action that has been done on the object between the versions involved in the diff.
     * @param propName the name of the property involved in the action.
     * @param propType the type of the property involved in the action.
     * @param prevValue the previous value of the property.
     * @param newValue the new value of the property.
     * @param sensitive {@code true} if the property is sensitive and should be obfuscated.
     *
     * @since 18.2.0RC1
     */
    @Unstable
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ObjectDiff(DocumentReference xClassReference, int number, String guid, String action, String propName,
        String propType, Object prevValue, Object newValue, boolean sensitive)
    {
        this.setXClassReference(xClassReference);
        this.setNumber(number);
        this.setGuid(guid);
        this.setAction(action);
        this.setPropName(propName);
        this.setPropType(propType);
        this.setPrevValue(prevValue);
        this.setNewValue(newValue);
        this.setSensitive(sensitive);
    }

    /**
     * @return the name of the class of the object the diff is performed for.
     */
    public String getClassName()
    {
        DocumentReference classReference = getXClassReference();

        return classReference != null ? this.localEntityReferenceSerializer.serialize(classReference) : null;
    }

    /**
     * @param className the name of the class of the object the diff is performed for.
     */
    public void setClassName(String className)
    {
        DocumentReference classReference =
            className != null ? this.currentDocumentReferenceResolver.resolve(className) : null;
        setXClassReference(classReference);
    }

    /**
     * @return the reference of the class of the object the diff is performed for.
     */
    public DocumentReference getXClassReference()
    {
        return this.xClassReference;
    }

    /**
     * @param xClassReference the reference of the class of the object the diff is performed for.
     */
    public void setXClassReference(DocumentReference xClassReference)
    {
        this.xClassReference = xClassReference;
    }

    /**
     * @return the number of the object the diff is performed for.
     */
    public int getNumber()
    {
        return this.number;
    }

    /**
     * @param number the number of the object the diff is performed for.
     */
    public void setNumber(int number)
    {
        this.number = number;
    }

    /**
     * @return the guid of the object the diff is performed for.
     */
    public String getGuid()
    {
        return this.guid;
    }

    /**
     * @param guid the guid of the object the diff is performed for.
     */
    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    /**
     * @return propName the name of the property involved in the action.
     */
    public String getPropName()
    {
        return this.propName;
    }

    /**
     * @param propName propName the name of the property involved in the action.
     */
    public void setPropName(String propName)
    {
        this.propName = propName;
    }

    /**
     * @return the type of the property involved in the action.
     */
    public String getPropType()
    {
        return this.propType;
    }

    /**
     * @param propType the type of the property involved in the action.
     */
    public void setPropType(String propType)
    {
        this.propType = propType;
    }

    /**
     * @return the previous value of the property.
     */
    public Object getPrevValue()
    {
        return this.prevValue;
    }

    /**
     * @param prevValue the previous value of the property.
     */
    public void setPrevValue(Object prevValue)
    {
        this.prevValue = prevValue;
    }

    /**
     * @return the new value of the property.
     */
    public Object getNewValue()
    {
        return this.newValue;
    }

    /**
     * @param newValue the new value of the property.
     */
    public void setNewValue(Object newValue)
    {
        this.newValue = newValue;
    }

    /**
     * @return the actual action that has been done on the object between the versions involved in the diff.
     */
    public String getAction()
    {
        return this.action;
    }

    /**
     * @param action the actual action that has been done on the object between the versions involved in the diff.
     */
    public void setAction(String action)
    {
        this.action = action;
    }

    /**
     * @return {@code true} if the property is sensitive and should be obfuscated.
     * @since 18.2.0RC1
     */
    @Unstable
    public boolean isSensitive()
    {
        return sensitive;
    }

    /**
     * @param sensitive {@code true} if the property is sensitive and should be obfuscated.
     * @since 18.2.0RC1
     */
    @Unstable
    public void setSensitive(boolean sensitive)
    {
        this.sensitive = sensitive;
    }

    @Override
    public String toString()
    {
        return getClassName()
            + "."
            + getPropName()
            + ": "
            + getPrevValue().toString()
            + " ⇨ "
            + getNewValue().toString();
    }
}
