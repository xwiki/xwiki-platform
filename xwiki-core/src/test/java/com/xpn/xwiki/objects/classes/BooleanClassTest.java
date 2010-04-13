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
 *
 */
package com.xpn.xwiki.objects.classes;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jmock.Mock;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Unit tests for the {@link BooleanClass} class.
 * 
 * @version $Id$
 */
public class BooleanClassTest extends AbstractBridgedXWikiComponentTestCase
{
    /** Test localization. */
    public void testLocalization()
    {
        // Setup
        ResourceBundle bundle = ResourceBundle.getBundle("ApplicationResources", new Locale("en"));
        Mock mockXWiki = mock(XWiki.class);
        mockXWiki.stubs().method("getDefaultLanguage").will(returnValue("en"));
        mockXWiki.stubs().method("getLanguagePreference").will(returnValue("en"));
        mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Fake.Translations"));
        mockXWiki.stubs().method("Param").will(returnValue(null));
        XWikiDocument translationDoc = new XWikiDocument();
        translationDoc.setContent("Some.Class_prop_0=Nay\nSome.Class_prop_1=Aye\nSome.Class_prop_2=Dunno\n");
        translationDoc.setLanguage("en");
        translationDoc.setDefaultLanguage("en");
        translationDoc.setNew(false);
        mockXWiki.stubs().method("getDocument").will(returnValue(translationDoc));

        this.getContext().setWiki((XWiki) mockXWiki.proxy());
        this.getContext().put("msg", new XWikiMessageTool(bundle, this.getContext()));

        // Create a Boolean meta-property and an associated object with a property instance
        BooleanClass metaProperty = new BooleanClass();
        BaseClass cls = new BaseClass();
        BaseObject obj = new BaseObject();
        IntegerProperty prop = new IntegerProperty();
        prop.setValue(0);
        obj.safeput("prop", prop);
        cls.setName("Some.Class");
        metaProperty.setObject(cls);

        StringBuffer out = new StringBuffer();

        // Test the default translations, should be the default "yesno" display type
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("No", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("Yes", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("---", out.toString());

        // Test translations when display type is "active"
        metaProperty.setDisplayType("active");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("Inactive", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("Active", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("---", out.toString());

        // Test translations when display type is the non-existing "blacktive"
        metaProperty.setDisplayType("blacktive");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("0", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("1", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("---", out.toString());

        // Test translations with the full classname_prop_value format
        metaProperty.setName("prop");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("Nay", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("Aye", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.getContext());
        assertEquals("Dunno", out.toString());
    }
}
