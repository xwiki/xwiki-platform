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
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.objects.classes.PropertyClassProvider;
import com.xpn.xwiki.internal.objects.meta.PropertyMetaClassInterface;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.web.Utils;

/**
 * Default implementation of {@link PropertyMetaClassInterface}. Provides the default set of meta properties all XClass
 * properties have. Can be extended by adding new meta properties with
 * {@link #safeput(String, com.xpn.xwiki.objects.PropertyInterface)}.
 * <p>
 * NOTE: We implement {@link PropertyClassProvider} in order to be able to use existing meta classes (that extend this
 * one) as providers while keeping backward compatibility. When defining new property types you should not extend this
 * class but rather create a new {@link PropertyClassProvider} that creates an instance of this class and adds new meta
 * properties using {@link #safeput(String, com.xpn.xwiki.objects.PropertyInterface)}.
 *
 * @version $Id$
 */
public class PropertyMetaClass extends BaseClass implements PropertyMetaClassInterface, PropertyClassProvider
{
    /**
     * Default constructor. Initializes the meta properties that are common to all XClass property types.
     */
    public PropertyMetaClass()
    {
        // NOTE: All XClass property types have an additional read-only meta property called 'classType' that is added
        // automatically when exporting the XClass property to XML. See PropertyClass#toXML().

        StringClass nameClass = new StringClass(this);
        nameClass.setName("name");
        nameClass.setPrettyName("Name");
        nameClass.setUnmodifiable(true);
        nameClass.setSize(40);
        safeput(nameClass.getName(), nameClass);

        BooleanClass disabledClass = new BooleanClass(this);
        disabledClass.setName("disabled");
        disabledClass.setPrettyName("Disabled");
        disabledClass.setDisplayType("yesno");
        disabledClass.setDisplayFormType("checkbox");
        safeput(disabledClass.getName(), disabledClass);

        addPresentationMetaProperties();

        BooleanClass unmodifiableClass = new BooleanClass(this);
        unmodifiableClass.setName("unmodifiable");
        unmodifiableClass.setPrettyName("Unmodifiable");
        unmodifiableClass.setDisplayType(disabledClass.getDisplayType());
        safeput(unmodifiableClass.getName(), unmodifiableClass);

        NumberClass numberClass = new NumberClass(this);
        numberClass.setName("number");
        numberClass.setPrettyName("Number");
        numberClass.setNumberType("integer");
        safeput(numberClass.getName(), numberClass);

        addValidationMetaProperties();
    }

    /**
     * Adds generic meta properties that control how an XClass property is displayed.
     */
    private void addPresentationMetaProperties()
    {
        StringClass prettyNameClass = new StringClass(this);
        prettyNameClass.setName("prettyName");
        prettyNameClass.setPrettyName("Pretty Name");
        prettyNameClass.setSize(40);
        safeput(prettyNameClass.getName(), prettyNameClass);

        StringClass hintClass = new StringClass(this);
        hintClass.setName("hint");
        hintClass.setPrettyName("Hint");
        hintClass.setSize(40);
        safeput(hintClass.getName(), hintClass);

        TextAreaClass toolTipClass = new TextAreaClass(this);
        toolTipClass.setName("tooltip");
        toolTipClass.setPrettyName("Tooltip");
        toolTipClass.setSize(60);
        toolTipClass.setRows(5);
        safeput(toolTipClass.getName(), toolTipClass);

        TextAreaClass customDisplayClass = new TextAreaClass(this);
        customDisplayClass.setName("customDisplay");
        customDisplayClass.setPrettyName("Custom Display");
        customDisplayClass.setEditor("Text");
        customDisplayClass.setRows(5);
        customDisplayClass.setSize(80);
        safeput(customDisplayClass.getName(), customDisplayClass);
    }

    /**
     * Adds the meta properties used for validation the XClass property value.
     */
    private void addValidationMetaProperties()
    {
        StringClass validationRegExpClass = new StringClass(this);
        validationRegExpClass.setName("validationRegExp");
        validationRegExpClass.setPrettyName("Validation Regular Expression");
        validationRegExpClass.setSize(40);
        safeput(validationRegExpClass.getName(), validationRegExpClass);

        StringClass validationMessageClass = new StringClass(this);
        validationMessageClass.setName("validationMessage");
        validationMessageClass.setPrettyName("Validation Message");
        validationMessageClass.setSize(80);
        safeput(validationMessageClass.getName(), validationMessageClass);
    }

    @Override
    public BaseCollection getObject()
    {
        return null;
    }

    @Override
    public void setObject(BaseCollection object)
    {
    }

    @Override
    public String toFormString()
    {
        return null;
    }

    @Override
    public PropertyMetaClass clone()
    {
        return (PropertyMetaClass) super.clone();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is deprecated. Use directly the {@link PropertyClassProvider} if you need a new XClass property
     * instance.
     * </p>
     */
    @Override
    public BaseCollection newObject(XWikiContext context) throws XWikiException
    {
        PropertyClassInterface instance = null;
        try {
            // Try to use the corresponding XClass property provider to create the new property instance.
            PropertyClassProvider provider = Utils.getComponent(PropertyClassProvider.class, getName());
            instance = provider.getInstance();
        } catch (Exception e) {
            // Fail silently.
        }
        return instance != null && instance instanceof BaseCollection ? (BaseCollection) instance : super
            .newObject(context);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        // Needs to be implemented in derived classes. We didn't make this method abstract to preserve backwards
        // compatibility.
        return null;
    }

    @Override
    public PropertyMetaClassInterface getDefinition()
    {
        return this;
    }
}
