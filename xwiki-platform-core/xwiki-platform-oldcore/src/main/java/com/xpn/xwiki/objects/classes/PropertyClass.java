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
package com.xpn.xwiki.objects.classes;

import java.util.Iterator;
import java.util.List;

import org.apache.ecs.xhtml.input;
import org.apache.velocity.VelocityContext;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.hibernate.mapping.Property;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.Utils;

/**
 * Represents an XClass property and contains property definitions (eg "relational storage", "display type",
 * "separator", "multi select", etc). Each property definition is of type {@link BaseProperty}.
 * 
 * @version $Id$
 */
public class PropertyClass extends BaseCollection<ClassPropertyReference> implements PropertyClassInterface,
    PropertyInterface, Comparable<PropertyClass>
{
    private BaseClass xclass;

    private long id;

    private PropertyMetaClass pMetaClass;

    public PropertyClass()
    {
    }

    public PropertyClass(String name, String prettyname, PropertyMetaClass xWikiClass)
    {
        setName(name);
        setPrettyName(prettyname);
        setxWikiClass(xWikiClass);
        setUnmodifiable(false);
        setDisabled(false);
    }

    @Override
    protected ClassPropertyReference createReference()
    {
        return new ClassPropertyReference(getName(), this.xclass.getReference());
    }

    @Override
    public BaseClass getXClass(XWikiContext context)
    {
        return getxWikiClass();
    }
    
    public BaseClass getxWikiClass()
    {
        if (this.pMetaClass == null) {
            MetaClass metaClass = MetaClass.getMetaClass();
            this.pMetaClass = (PropertyMetaClass) metaClass.get(getClassType());
        }
        return this.pMetaClass;
    }

    public void setxWikiClass(BaseClass xWikiClass)
    {
        this.pMetaClass = (PropertyMetaClass) xWikiClass;
    }

    @Override
    public BaseCollection getObject()
    {
        return this.xclass;
    }

    @Override
    public void setObject(BaseCollection object)
    {
        this.xclass = (BaseClass) object;
    }

    public String getFieldFullName()
    {
        if (getObject() == null) {
            return getName();
        }
        return getObject().getName() + "_" + getName();
    }

    @Override
    public long getId()
    {
        if (getObject() == null) {
            return this.id;
        }
        return getObject().getId();
    }

    @Override
    public void setId(long id)
    {
        this.id = id;
    }

    @Override
    public String toString(BaseProperty property)
    {
        return property.toText();
    }

    @Override
    public BaseProperty fromString(String value)
    {
        return null;
    }

    public BaseProperty newPropertyfromXML(Element ppcel)
    {
        String value = ppcel.getText();
        return fromString(value);
    }

    @Override
    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        input input = new input();
        PropertyInterface prop = object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toFormString());
        }

        input.setType("hidden");
        input.setName(prefix + name);
        input.setID(prefix + name);
        buffer.append(input.toString());
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            buffer.append(prop.toText());
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        input input = new input();

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toFormString());
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setDisabled(isDisabled());
        buffer.append(input.toString());
    }

    public String displayHidden(String name, String prefix, BaseCollection object, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayHidden(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayHidden(String name, BaseCollection object, XWikiContext context)
    {
        return displayHidden(name, "", object, context);
    }

    public String displayView(String name, String prefix, BaseCollection object, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayView(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayView(String name, BaseCollection object, XWikiContext context)
    {
        return displayView(name, "", object, context);
    }

    public String displayEdit(String name, String prefix, BaseCollection object, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayEdit(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayEdit(String name, BaseCollection object, XWikiContext context)
    {
        return displayEdit(name, "", object, context);
    }

    public boolean isCustomDisplayed(XWikiContext context)
    {
        String disp = getCustomDisplay();
        return disp != null && disp.length() > 0;
    }

    public void displayCustom(StringBuffer buffer, String fieldName, String prefix, String type, BaseObject object,
        XWikiContext context) throws XWikiException
    {
        String content = getCustomDisplay();

        try {
            VelocityContext vcontext = Utils.getComponent(VelocityManager.class).getVelocityContext();
            vcontext.put("name", fieldName);
            vcontext.put("prefix", prefix);
            vcontext.put("object", new com.xpn.xwiki.api.Object(object, context));
            vcontext.put("type", type);
            vcontext.put("context", new com.xpn.xwiki.api.Context(context));

            BaseProperty prop = (BaseProperty) object.safeget(fieldName);
            if (prop != null) {
                vcontext.put("value", prop.getValue());
            }

            String classSyntax =
                context.getWiki().getDocument(getObject().getDocumentReference(), context).getSyntax().toIdString();
            content = context.getDoc().getRenderedContent(content, classSyntax, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                XWikiException.ERROR_XWIKI_CLASSES_CANNOT_PREPARE_CUSTOM_DISPLAY,
                "Exception while preparing the custom display of " + fieldName, e, null);

        }
        buffer.append(content);
    }

    @Override
    public String getClassName()
    {
        BaseClass bclass = getxWikiClass();
        return (bclass == null) ? "" : bclass.getName();
    }

    // In property classes we need to store this info in the HashMap for fields
    // This way it is readable by the displayEdit/displayView functions..
    @Override
    public String getName()
    {
        return getStringValue("name");
    }

    @Override
    public void setName(String name)
    {
        setStringValue("name", name);
    }

    public String getCustomDisplay()
    {
        return getStringValue("customDisplay");
    }

    public void setCustomDisplay(String value)
    {
        setLargeStringValue("customDisplay", value);
    }

    @Override
    public String getPrettyName()
    {
        return getStringValue("prettyName");
    }

    public String getPrettyName(XWikiContext context)
    {
        return getTranslatedPrettyName(context);
    }

    public String getTranslatedPrettyName(XWikiContext context)
    {
        String msgName = getFieldFullName();
        if ((context == null) || (context.getWiki() == null)) {
            return getPrettyName();
        }

        String prettyName = context.getMessageTool().get(msgName);
        if (prettyName.equals(msgName)) {
            return getPrettyName();
        }
        return prettyName;
    }

    @Override
    public void setPrettyName(String prettyName)
    {
        setStringValue("prettyName", prettyName);
    }

    public String getTooltip()
    {
        return getLargeStringValue("tooltip");
    }

    /**
     * Gets international tooltip
     * 
     * @param context
     * @return
     */
    public String getTooltip(XWikiContext context)
    {
        String tooltipName = getFieldFullName() + "_tooltip";
        String tooltip = context.getMessageTool().get(tooltipName);
        if (tooltipName.equals(tooltip)) {
            tooltipName = getLargeStringValue("tooltip");
            if ((tooltipName != null) && (!tooltipName.trim().equals(""))) {
                tooltip = context.getMessageTool().get(tooltipName);
            }
        }
        return tooltip;
    }

    public void setTooltip(String tooltip)
    {
        setLargeStringValue("tooltip", tooltip);
    }

    @Override
    public int getNumber()
    {
        return getIntValue("number");
    }

    @Override
    public void setNumber(int number)
    {
        setIntValue("number", number);
    }

    public String getClassType()
    {
        return getClass().getName();
    }

    public void setClassType(String type)
    {
    }

    @Override
    public PropertyClass clone()
    {
        PropertyClass pclass = (PropertyClass) super.clone();
        pclass.setObject(getObject());
        pclass.setClassType(getClassType());
        return pclass;
    }

    @Override
    public Element toXML(BaseClass bclass)
    {
        return toXML();
    }

    @Override
    public Element toXML()
    {
        Element pel = new DOMElement(getName());

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator it = getSortedIterator();
        while (it.hasNext()) {
            BaseProperty bprop = (BaseProperty) it.next();
            pel.add(bprop.toXML());
        }
        Element el = new DOMElement("classType");
        el.addText(getClassType());
        pel.add(el);
        return pel;
    }

    public void fromXML(Element pcel) throws XWikiException
    {
        List list = pcel.elements();
        BaseClass bclass = getxWikiClass();

        for (int i = 0; i < list.size(); i++) {
            Element ppcel = (Element) list.get(i);
            String name = ppcel.getName();
            if (bclass == null) {
                Object[] args = {getClass().getName()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                    XWikiException.ERROR_XWIKI_CLASSES_PROPERTY_CLASS_IN_METACLASS,
                    "Cannot find property class {0} in MetaClass object", null, args);
            }
            PropertyClass pclass = (PropertyClass) bclass.safeget(name);
            if (pclass != null) {
                BaseProperty bprop = pclass.newPropertyfromXML(ppcel);
                bprop.setObject(this);
                safeput(name, bprop);
            }
        }
    }

    @Override
    public String toFormString()
    {
        return toString();
    }

    public void initLazyCollections()
    {
    }

    public boolean isUnmodifiable()
    {
        return (getIntValue("unmodifiable") == 1);
    }

    public void setUnmodifiable(boolean unmodifiable)
    {
        if (unmodifiable) {
            setIntValue("unmodifiable", 1);
        } else {
            setIntValue("unmodifiable", 0);
        }
    }

    /**
     * See if this property is disabled or not. A disabled property should not be editable, but existing object values
     * are still kept in the database.
     * 
     * @return {@code true} if this property is disabled and should not be used, {@code false} otherwise
     * @see #setDisabled(boolean)
     * @since 2.4M2
     */
    public boolean isDisabled()
    {
        return (getIntValue("disabled", 0) == 1);
    }

    /**
     * Disable or re-enable this property. A disabled property should not be editable, but existing object values are
     * still kept in the database.
     * 
     * @param disabled whether the property is disabled or not
     * @see #isDisabled()
     * @since 2.4M2
     */
    public void setDisabled(boolean disabled)
    {
        if (disabled) {
            setIntValue("disabled", 1);
        } else {
            setIntValue("disabled", 0);
        }
    }

    public BaseProperty fromStringArray(String[] strings)
    {
        return fromString(strings[0]);
    }

    public boolean isValidColumnTypes(Property hibprop)
    {
        return true;
    }

    @Override
    public BaseProperty fromValue(Object value)
    {
        BaseProperty property = newProperty();
        property.setValue(value);
        return property;
    }

    @Override
    public BaseProperty newProperty()
    {
        return new BaseProperty();
    }

    public void setValidationRegExp(String validationRegExp)
    {
        setStringValue("validationRegExp", validationRegExp);
    }

    public String getValidationRegExp()
    {
        return getStringValue("validationRegExp");
    }

    public String getValidationMessage()
    {
        return getStringValue("validationMessage");
    }

    public void setValidationMessage(String validationMessage)
    {
        setStringValue("validationMessage", validationMessage);
    }

    public boolean validateProperty(BaseProperty property, XWikiContext context)
    {
        String regexp = getValidationRegExp();
        if ((regexp == null) || (regexp.trim().equals(""))) {
            return true;
        }

        String value = ((property == null) || (property.getValue() == null)) ? "" : property.getValue().toString();
        try {
            if (context.getUtil().match(regexp, value)) {
                return true;
            }
            XWikiValidationStatus.addErrorToContext((getObject() == null) ? "" : getObject().getName(), getName(),
                getTranslatedPrettyName(context), getValidationMessage(), context);

            return false;
        } catch (Exception e) {
            XWikiValidationStatus.addExceptionToContext((getObject() == null) ? "" : getObject().getName(), getName(),
                e, context);

            return false;
        }
    }

    @Override
    public void flushCache()
    {
    }

    /**
     * Compares two property definitions based on their index number.
     * 
     * @param other the other property definition to be compared with
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @see #getNumber()
     * @since 2.4M2
     */
    @Override
    public int compareTo(PropertyClass other)
    {
        int result = this.getNumber() - other.getNumber();

        // This should never happen, but just to remove the randomness in case it does happen, also compare their names.
        if (result == 0) {
            result = this.getName().compareTo(other.getName());
        }

        return result;
    }

    protected String getFullQueryPropertyName()
    {
        return "obj." + getName();
    }
}
