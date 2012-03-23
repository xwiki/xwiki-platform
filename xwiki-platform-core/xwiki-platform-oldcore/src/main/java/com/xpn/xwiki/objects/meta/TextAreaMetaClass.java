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
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

public class TextAreaMetaClass extends StringMetaClass
{
    public TextAreaMetaClass()
    {
        super();
        // setType("textareametaclass");
        setPrettyName("TextArea");
        setName(TextAreaClass.class.getName());

        NumberClass rows_class = new NumberClass(this);
        rows_class.setName("rows");
        rows_class.setPrettyName("Rows");
        rows_class.setSize(5);
        rows_class.setNumberType("integer");
        safeput("rows", rows_class);

        StaticListClass editor_class = new StaticListClass(this);
        editor_class.setName("editor");
        editor_class.setPrettyName("Editor");
        editor_class.setValues("---|Text|PureText|Wysiwyg");
        editor_class.setRelationalStorage(false);
        editor_class.setDisplayType("select");
        editor_class.setMultiSelect(false);
        editor_class.setSize(1);
        safeput("editor", editor_class);

        StaticListClass contenttype_class = new StaticListClass(this);
        contenttype_class.setName("contenttype");
        contenttype_class.setPrettyName("Content");
        contenttype_class.setValues("FullyRenderedText|VelocityCode|PureText");
        contenttype_class.setRelationalStorage(false);
        contenttype_class.setDisplayType("select");
        contenttype_class.setMultiSelect(false);
        contenttype_class.setSize(1);
        safeput("contenttype", contenttype_class);
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new TextAreaClass();
    }
}
