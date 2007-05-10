/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ecs.xhtml.input;
import org.apache.velocity.VelocityContext;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.hibernate.mapping.Property;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.validation.XWikiValidationStatus;

public class PropertyClass extends BaseCollection implements PropertyClassInterface,
    PropertyInterface
{
    private BaseClass object;

    private int id;

    private PropertyMetaClass pMetaClass;

    public PropertyClass()
    {
    }

    public PropertyClass(String name, String prettyname, PropertyMetaClass xWikiClass)
    {
        super();
        setName(name);
        setPrettyName(prettyname);
        setxWikiClass(xWikiClass);
        setUnmodifiable(false);
    }

    public BaseClass getxWikiClass()
    {
        if (pMetaClass == null) {
            MetaClass metaClass = MetaClass.getMetaClass();
            pMetaClass = (PropertyMetaClass) metaClass.get(getClassType());
        }
        return pMetaClass;
    }

    public void setxWikiClass(BaseClass xWikiClass)
    {
        this.pMetaClass = (PropertyMetaClass) xWikiClass;
    }

    public BaseCollection getObject()
    {
        return object;
    }

    public void setObject(BaseCollection object)
    {
        this.object = (BaseClass) object;
    }

    public String getFieldFullName()
    {
        if (getObject() == null) {
            return getName();
        }
        return getObject().getName() + "_" + getName();
    }

    public int getId()
    {
        if (getObject() == null) {
            return id;
        }
        return getObject().getId();
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void checkField(String name) throws XWikiException
    {
        // Let's stop checking
        /*
         * if ((getxWikiClass(context).safeget(name)==null)&& (getxWikiClass(context).safeget("meta" +
         * name)==null)){ Object[] args = { name, getxWikiClass(context).getName() }; throw new
         * XWikiException( XWikiException.MODULE_XWIKI_CLASSES,
         * XWikiException.ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST, "Field {0} does not exist in
         * class {1}", null, args); }
         */
    }

    public String toString(BaseProperty property)
    {
        return property.toText();
    }

    public BaseProperty fromString(String value)
    {
        return null;
    }

    public BaseProperty newPropertyfromXML(Element ppcel)
    {
        String value = ppcel.getText();
        return fromString(value);
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
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

    public void displaySearch(StringBuffer buffer, String name, String prefix,
        XWikiCriteria criteria, XWikiContext context)
    {
        input input = new input();
        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(20);
        String fieldFullName = getFieldFullName();
        Object value = criteria.getParameter(fieldFullName);
        if (value != null) {
            input.setValue(value.toString());
        }
        buffer.append(input.toString());
    }

    public void displayView(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
        buffer.append(((BaseProperty) object.safeget(name)).toText());
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
        input input = new input();

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toFormString());
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        buffer.append(input.toString());
    }

    public String displayHidden(String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayHidden(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayHidden(String name, BaseCollection object, XWikiContext context)
    {
        return displayHidden(name, "", object, context);
    }

    public String displaySearch(String name, String prefix, XWikiCriteria criteria,
        XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displaySearch(buffer, name, prefix, criteria, context);
        return buffer.toString();
    }

    public String displaySearch(String name, XWikiCriteria criteria, XWikiContext context)
    {
        return displaySearch(name, "", criteria, context);
    }

    public String displayView(String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayView(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayView(String name, BaseCollection object, XWikiContext context)
    {
        return displayView(name, "", object, context);
    }

    public String displayEdit(String name, String prefix, BaseCollection object,
        XWikiContext context)
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

    public void displayCustom(StringBuffer buffer, String fieldName, String prefix, String type,
        BaseObject object, XWikiContext context) throws XWikiException
    {
        String content = getCustomDisplay();

        try {
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("name", fieldName);
            vcontext.put("prefix", prefix);
            vcontext.put("object", new com.xpn.xwiki.api.Object(object, context));
            vcontext.put("type", type);
            vcontext.put("context", new com.xpn.xwiki.api.Context(context));

            BaseProperty prop = (BaseProperty) object.safeget(fieldName);
            if (prop != null) {
                vcontext.put("value", prop.getValue());
            }

            content = context.getWiki().parseContent(content, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                XWikiException.ERROR_XWIKI_CLASSES_CANNOT_PREPARE_CUSTOM_DISPLAY,
                "Exception while preparing the custom display of " + fieldName,
                e,
                null);

        }
        buffer.append(content);
    }

    public BaseClass getxWikiClass(XWikiContext context)
    {
        return getxWikiClass();
    }

    public String getClassName()
    {
        BaseClass bclass = getxWikiClass();
        return (bclass == null) ? "" : bclass.getName();
    }

    // In property classes we need to store this info in the HashMap for fields
    // This way it is readable by the displayEdit/displayView functions..
    public String getName()
    {
        return getStringValue("name");
    }

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

    public String getPrettyName()
    {
        return getStringValue("prettyName");
    }

    public void setPrettyName(String prettyName)
    {
        setStringValue("prettyName", prettyName);
    }

    public String getTooltip()
    {
        return getLargeStringValue("tooltip");
    }

    public void setTooltip(String tooltip)
    {
        setLargeStringValue("tooltip", tooltip);
    }

    public String getTranslatedPrettyName(XWikiContext context)
    {
        String msgName = className + "_" + getName();
        if ((context == null) || (context.getWiki() == null)) {
            return getPrettyName();
        }

        String prettyName = context.getWiki().getMessage(msgName, context);
        if (prettyName.equals(msgName)) {
            return getPrettyName();
        }
        return prettyName;
    }

    public int getNumber()
    {
        return getIntValue("number");
    }

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

    public Object clone()
    {
        PropertyClass pclass = (PropertyClass) super.clone();
        pclass.setObject(getObject());
        pclass.setClassType(getClassType());
        return pclass;
    }

    public Element toXML(BaseClass bclass)
    {
        return toXML();
    }

    public Element toXML()
    {
        Element pel = new DOMElement(getName());
        Iterator it = getFieldList().iterator();
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
                    "Cannot find property class {0} in MetaClass object",
                    null,
                    args);
            }
            PropertyClass pclass = (PropertyClass) bclass.safeget(name);
            if (pclass != null) {
                BaseProperty bprop = pclass.newPropertyfromXML(ppcel);
                bprop.setObject(this);
                safeput(name, bprop);
            }
        }
    }

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

    public BaseProperty fromStringArray(String[] strings)
    {
        return fromString(strings[0]);
    }

    public boolean isValidColumnTypes(Property hibprop)
    {
        return true;
    }

    public BaseProperty fromValue(Object value)
    {
        BaseProperty property = newProperty();
        property.setValue(value);
        return property;
    }

    public BaseProperty newProperty()
    {
        return new BaseProperty();
    }

    public void makeQuery(Map map, String prefix, XWikiCriteria query, List criteriaList)
    {
    }

    public void fromSearchMap(XWikiQuery query, Map map)
    {
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

        String value =
            ((property == null) || (property.getValue() == null)) ? "" : property.getValue()
                .toString();
        try {
            if (context.getUtil().match(regexp, value)) {
                return true;
            }
            XWikiValidationStatus.addErrorToContext((getObject() == null) ? "" : getObject()
                .getClassName(), getName(), getTranslatedPrettyName(context),
                getValidationMessage(), context);
            return false;
        } catch (Exception e) {
            XWikiValidationStatus.addExceptionToContext(getObject().getClassName(), getName(), e,
                context);
            return false;
        }
    }

    public void flushCache()
    {
    }
}
