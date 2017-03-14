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
package org.xwiki.ckeditor.test.po;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.editor.wysiwyg.RichTextAreaElement;

/**
 * Models a CKEditor instance.
 * 
 * @version $Id$
 * @since 1.13
 */
@Unstable
public class CKEditor extends BaseElement
{
    /**
     * The editor field name.
     */
    private String name;

    /**
     * Creates a new instance that can be used to interact with the first CKEditor detected on the page. Use this
     * constructor only when the page has a single CKEditor instance.
     */
    public CKEditor()
    {
    }

    /**
     * Create a new instance that can be used to interact with the specified CKEditor instance.
     * 
     * @param name the editor field name
     */
    public CKEditor(String name)
    {
        this.name = name;
    }

    /**
     * Waits for CKEditor to load.
     */
    public CKEditor waitToLoad()
    {
        XWikiWebDriver driver = getDriver();
        driver.manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);
        StringBuilder script = new StringBuilder();
        script.append("var name = arguments[0];\n");
        script.append("var callback = arguments[1];\n");
        script.append("require(['deferred!ckeditor'], function(ckeditorPromise) {\n");
        script.append("  ckeditorPromise.done(function(ckeditor) {\n");
        script.append("    var listener = function(event) {\n");
        script.append("      if (!name || name === event.editor.name) {\n");
        script.append("        ckeditor.removeListener('instanceReady', listener);\n");
        script.append("        callback(event.editor.name);\n");
        script.append("      }\n");
        script.append("    };\n");
        script.append("    ckeditor.on('instanceReady', listener);\n");
        script.append("    if (name) {\n");
        script.append("      var instance = ckeditor.instances[name];\n");
        script.append("      instance && instance.status === 'ready' && listener({editor: instance});\n");
        script.append("    } else {\n");
        script.append("      for (var key in ckeditor.instances) {\n");
        script.append("        if (ckeditor.instances.hasOwnProperty(key)) {\n");
        script.append("          var instance = ckeditor.instances[key];\n");
        script.append("          instance && instance.status === 'ready' && listener({editor: instance});\n");
        script.append("        }\n");
        script.append("      }\n");
        script.append("    }\n");
        script.append("  });\n");
        script.append("});\n");
        this.name = (String) driver.executeAsyncScript(script.toString(), this.name);
        return this;
    }

    /**
     * @return the rich text area
     */
    public RichTextAreaElement getRichTextArea()
    {
        // The in-line frame element is renewed while editing so we can't cache it.
        return new org.xwiki.ckeditor.test.po.internal.RichTextAreaElement((WebElement) getDriver().executeScript(
            "return CKEDITOR.instances[arguments[0]].ui.contentsElement.find('iframe').getItem(0).$;", this.name));
    }
}
