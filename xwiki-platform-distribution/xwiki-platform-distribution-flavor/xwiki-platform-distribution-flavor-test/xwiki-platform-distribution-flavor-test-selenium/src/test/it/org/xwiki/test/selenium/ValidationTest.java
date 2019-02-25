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
package org.xwiki.test.selenium;

import org.junit.Test;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

/**
 * Verify the data validation feature of XWiki.
 * 
 * @version $Id$
 */
public class ValidationTest extends AbstractXWikiTestCase
{
    private static final String SYNTAX = "xwiki/2.1";

    @Test
    public void testSaveActionValidatesWhenXValidateIsPresent()
    {
        deletePage("Main", "ValidatedClass");
        open("Main", "ValidatedClass", "edit", "editor=class");
        setFieldValue("propname", "prop");
        setFieldValue("proptype", "String");
        clickButtonAndContinue("//input[@name='action_propadd']");
        setFieldValue("prop_validationRegExp", "/^[0-4][0-2]$/");
        setFieldValue("prop_validationMessage", "invalid value for prop");
        clickEditSaveAndView();
        createPage("Main", "ValidatedObject",
            "{{velocity}}value: $doc.display('prop')\n\n#foreach($e in $xcontext.validationStatus.errors)$e #end{{/velocity}}", SYNTAX);
        open("Main", "ValidatedObject", "edit", "editor=object");
        new ObjectEditPage().addObject("Main.ValidatedClass");
        setFieldValue("Main.ValidatedClass_0_prop", "22");
        clickEditSaveAndView();
        open("Main", "ValidatedObject", "save", "xvalidate=1");
        assertTextPresent("value: 22");
        open("Main", "ValidatedObject", "edit", "editor=object");
        setFieldValue("Main.ValidatedClass_0_prop", "44");
        clickEditSaveAndView();
        open("Main", "ValidatedObject", "save", "xvalidate=1");
        assertTextNotPresent("value: 44");
        assertTextPresent("invalid value for prop");
        open("Main", "ValidatedObject", "save", "xvalidate=1&Main.ValidatedClass_0_prop=11");
        assertTextPresent("value: 11");
    }
}
