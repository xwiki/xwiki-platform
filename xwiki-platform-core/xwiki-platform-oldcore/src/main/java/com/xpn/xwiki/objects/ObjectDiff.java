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

import com.xpn.xwiki.web.Utils;

public class ObjectDiff
{
    public static final String ACTION_PROPERTYADDED = "added";

    public static final String ACTION_PROPERTYCHANGED = "changed";

    public static final String ACTION_PROPERTYREMOVED = "removed";

    public static final String ACTION_OBJECTADDED = "object-added";

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

    @Deprecated
    public ObjectDiff(String className, int number, String action, String propName, Object prevValue, Object newValue)
    {
        this(className, number, "", action, propName, "", prevValue, newValue);
    }

    @Deprecated
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

    public ObjectDiff(DocumentReference xClassReference, int number, String guid, String action, String propName,
        String propType, Object prevValue, Object newValue)
    {
        this.setXClassReference(xClassReference);
        this.setNumber(number);
        this.setGuid(guid);
        this.setAction(action);
        this.setPropName(propName);
        this.setPropType(propType);
        this.setPrevValue(prevValue);
        this.setNewValue(newValue);
    }

    public String getClassName()
    {
        DocumentReference xClassReference = getXClassReference();

        return xClassReference != null ? this.localEntityReferenceSerializer.serialize(getXClassReference()) : null;
    }

    public void setClassName(String className)
    {
        DocumentReference xClassReference =
            className != null ? this.currentDocumentReferenceResolver.resolve(className) : null;
        setXClassReference(xClassReference);
    }

    public DocumentReference getXClassReference()
    {
        return this.xClassReference;
    }

    public void setXClassReference(DocumentReference xClassReference)
    {
        this.xClassReference = xClassReference;
    }

    public int getNumber()
    {
        return this.number;
    }

    public void setNumber(int number)
    {
        this.number = number;
    }

    public String getGuid()
    {
        return this.guid;
    }

    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    public String getPropName()
    {
        return this.propName;
    }

    public void setPropName(String propName)
    {
        this.propName = propName;
    }

    public String getPropType()
    {
        return this.propType;
    }

    public void setPropType(String propType)
    {
        this.propType = propType;
    }

    public Object getPrevValue()
    {
        return this.prevValue;
    }

    public void setPrevValue(Object prevValue)
    {
        this.prevValue = prevValue;
    }

    public Object getNewValue()
    {
        return this.newValue;
    }

    public void setNewValue(Object newValue)
    {
        this.newValue = newValue;
    }

    public String getAction()
    {
        return this.action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        buffer.append(getClassName());
        buffer.append(".");
        buffer.append(getPropName());
        buffer.append(": ");
        buffer.append(getPrevValue().toString());
        buffer.append(" &gt; ");
        buffer.append(getNewValue().toString());

        return buffer.toString();
    }
}
