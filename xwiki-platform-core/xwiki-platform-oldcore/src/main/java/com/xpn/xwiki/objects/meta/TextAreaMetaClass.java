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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Defines the meta properties of a text area XClass property.
 *
 * @version $Id$
 */
@Component
@Named("TextArea")
@Singleton
public class TextAreaMetaClass extends StringMetaClass
{
    /**
     * Default constructor. Initializes the default meta properties of a Text Area XClass property.
     */
    public TextAreaMetaClass()
    {
        setPrettyName("TextArea");
        setName(getClass().getAnnotation(Named.class).value());

        NumberClass rowsClass = new NumberClass(this);
        rowsClass.setName("rows");
        rowsClass.setPrettyName("Rows");
        rowsClass.setSize(5);
        rowsClass.setNumberType("integer");
        safeput(rowsClass.getName(), rowsClass);

        StaticListClass editorClass = new StaticListClass(this);
        editorClass.setName("editor");
        editorClass.setPrettyName("Editor");
        editorClass.setValues("---|Text|PureText|Wysiwyg");
        editorClass.setRelationalStorage(false);
        editorClass.setDisplayType("select");
        editorClass.setMultiSelect(false);
        editorClass.setSize(1);
        safeput(editorClass.getName(), editorClass);

        StaticListClass contentTypeClass = new StaticListClass(this);
        contentTypeClass.setName("contenttype");
        contentTypeClass.setPrettyName("Content");
        contentTypeClass.setValues("FullyRenderedText|VelocityCode|PureText");
        contentTypeClass.setRelationalStorage(false);
        contentTypeClass.setDisplayType(editorClass.getDisplayType());
        contentTypeClass.setMultiSelect(false);
        contentTypeClass.setSize(1);
        safeput(contentTypeClass.getName(), contentTypeClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new TextAreaClass();
    }
}
