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
package org.xwiki.ckeditor.test.ui;

import org.junit.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.wysiwyg.RichTextAreaElement;

import static org.junit.Assert.*;

/**
 * Functional tests for the Save plugin.
 * 
 * @version $Id$
 * @since 1.13
 */
public class SaveTest extends AbstractCKEditorTest
{
    @Test
    public void save()
    {
        WYSIWYGEditPage editPage = WYSIWYGEditPage.gotoPage(getTestClassName(), getTestMethodName());
        CKEditor editor = new CKEditor().waitToLoad();
        RichTextAreaElement textArea = editor.getRichTextArea();
        textArea.clear();
        textArea.sendKeys("xyz");
        editPage.clickSaveAndView().edit();
        editPage.waitUntilPageJSIsLoaded();
        assertEquals("<p>xyz</p>", editor.waitToLoad().getRichTextArea().getContent());
    }
}
