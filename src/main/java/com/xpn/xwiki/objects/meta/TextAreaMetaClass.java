/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 24 janv. 2004
 * Time: 10:29:55
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.XWikiContext;

public class TextAreaMetaClass extends StringMetaClass {

public TextAreaMetaClass() {
    super();
    // setType("textareametaclass");
    setPrettyName("TextArea Class");
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
    editor_class.setValues("---|Text|Text2|Wysiwyg");
    editor_class.setRelationalStorage(false);
    editor_class.setDisplayType("select");
    editor_class.setMultiSelect(false);
    editor_class.setSize(1);
    safeput("editor", editor_class);
  }


    public BaseCollection newObject(XWikiContext context) {
          return new TextAreaClass();
    }
}
