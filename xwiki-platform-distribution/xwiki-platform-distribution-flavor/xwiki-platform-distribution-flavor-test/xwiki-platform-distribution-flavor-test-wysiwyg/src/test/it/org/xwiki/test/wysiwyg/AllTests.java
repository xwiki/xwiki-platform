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
package org.xwiki.test.wysiwyg;

import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.ui.PageObjectSuite;
import org.xwiki.test.ui.PersistentTestContext;

/**
 * Runs all functional tests found in the classpath.
 * 
 * @version $Id$
 */
@RunWith(PageObjectSuite.class)
public class AllTests extends org.xwiki.test.selenium.AllTests
{
    @PageObjectSuite.PostStart
    @Override
    public void postStart(PersistentTestContext context) throws Exception
    {
        super.postStart(context);

        displayHiddenDocumentsForAdmin(context);
        enableAllEditingFeatures(context);

        new SolrTestUtils(context.getUtil()).waitEmpyQueue();
    }

    private void displayHiddenDocumentsForAdmin(PersistentTestContext context)
    {
        context.getUtil().loginAsAdmin();
        context.getUtil().gotoPage("XWiki", "Admin", "save", "XWiki.XWikiUsers_0_displayHiddenDocuments=1");
    }

    private void enableAllEditingFeatures(PersistentTestContext context)
    {
        Map<String, String> params = new HashMap<>();
        params.put("XWiki.WysiwygEditorConfigClass_0_plugins",
            "submit readonly line separator embed text valign list "
            + "indent history format symbol link image " + "table macro import color justify font");
        params.put("XWiki.WysiwygEditorConfigClass_0_toolBar",
            "bold italic underline strikethrough teletype | subscript superscript | "
            + "justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent | "
            + "undo redo | format | fontname fontsize forecolor backcolor | hr removeformat symbol | "
            + " paste | macro:velocity");
        context.getUtil().gotoPage("XWiki", "WysiwygEditorConfig", "save", params);
    }
}
