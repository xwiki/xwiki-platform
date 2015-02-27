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
package org.xwiki.wysiwyg.test.ui;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import org.openqa.selenium.By;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.FormElement;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.wysiwyg.test.po.WYSIWYGEditPage;

/**
 * Base class for WYSIWYG Editor tests.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
public abstract class AbstractWYSIWYGEditorTest extends AbstractTest
{
    // Note: We do not use the @Rule annotation since we need this rule to be executed before the configure() method
    // below which runs **before** any @Rule (since it's tagged with @BeforeClass). Thus we trigger the authentication
    // manually.
    public static SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    /**
     * The edited page.
     */
    protected WYSIWYGEditPage editPage;

    @BeforeClass
    public static void configure()
    {
        authenticationRule.authenticate();
        enableAllEditingFeatures();
    }

    @Before
    public void setUp() throws Exception
    {
        editPage = WYSIWYGEditPage.gotoPage(getTestClassName(), getTestMethodName()).waitUntilPageIsLoaded();
    }

    /**
     * Enables all editing features so they are accessible for testing.
     */
    private static void enableAllEditingFeatures()
    {
        Map<String, String> config = new HashMap<String, String>();
        config.put("plugins", "submit readonly line separator embed text valign list "
            + "indent history format symbol link image " + "table macro import color justify font");
        config.put("toolBar", "bold italic underline strikethrough teletype | subscript superscript | "
            + "justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent | "
            + "undo redo | format | fontname fontsize forecolor backcolor | hr removeformat symbol | "
            + " paste | macro:velocity");
        updateConfiguration(config);
    }

    /**
     * Updates the WYSIWYG editor configuration based on the given configuration object. The key in the configuration is
     * the name of a {@code XWiki.WysiwygEditorConfigClass} property and the value is the new value for that property.
     * 
     * @param config the configuration object
     */
    private static void updateConfiguration(Map<String, String> config)
    {
        ObjectEditPage oep = ObjectEditPage.gotoPage("XWiki", "WysiwygEditorConfig");
        FormElement form = oep.getObjectsOfClass("XWiki.WysiwygEditorConfigClass").get(0);
        boolean save = false;
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String propertyId = "XWiki.WysiwygEditorConfigClass_0_" + entry.getKey();
            if (!entry.getValue().equals(form.getFieldValue(By.id(propertyId)))) {
                form.setFieldValue(By.id(propertyId), entry.getValue());
                save = true;
            }
        }
        if (save) {
            oep.clickSaveAndContinue();
        }
    }
}
