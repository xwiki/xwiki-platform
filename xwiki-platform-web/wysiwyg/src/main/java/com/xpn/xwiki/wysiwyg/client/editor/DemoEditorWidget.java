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
package com.xpn.xwiki.wysiwyg.client.editor;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.diff.Chunk;
import com.xpn.xwiki.wysiwyg.client.diff.Delta;
import com.xpn.xwiki.wysiwyg.client.diff.Diff;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.diff.ToString;

public class DemoEditorWidget extends Composite
{
    private Wysiwyg wysiwyg;

    private RichTextEditor editor1;

    private RichTextEditor editor2;

    private TextArea wikiTextArea;

    private TextArea convertedTextArea;

    private TextArea convertedTextArea2;

    public DemoEditorWidget(Wysiwyg wysiwyg)
    {
        this.wysiwyg = wysiwyg;

        FlowPanel panel = new FlowPanel();

        panel.setWidth("100%");
        editor1 = new RichTextEditor();
        editor1.setWidth("730px");
        editor1.setHeight("220px");
        panel.add(editor1);

        editor2 = new RichTextEditor();
        editor2.setWidth("730px");
        editor2.setHeight("220px");
        panel.add(editor2);

        convertedTextArea = new TextArea();
        convertedTextArea.setWidth("400px");
        convertedTextArea.setHeight("220px");
        panel.add(convertedTextArea);

        convertedTextArea2 = new TextArea();
        convertedTextArea2.setWidth("400px");
        convertedTextArea2.setHeight("220px");
        panel.add(convertedTextArea2);

        wikiTextArea = new TextArea();
        wikiTextArea.setWidth("400px");
        wikiTextArea.setHeight("220px");
        panel.add(wikiTextArea);

        // get the transformed HTML Content
        convertedTextArea.setText(editor1.getTextArea().getHTML());
        convertedTextArea2.setText(editor2.getTextArea().getHTML());

        initWidget(panel);
    }

    public void refreshData()
    {
        boolean debug = wysiwyg.getParam("1", "0").equals("1");
        String newDataBox1 = editor1.getTextArea().getHTML();
        if (debug || true)
            Window.alert("New Data box 1 is " + newDataBox1);
        String oldDataBox1 = convertedTextArea.getText();
        if (debug)
            Window.alert("Old data box 1 is " + oldDataBox1);

        String newDataBox2 = editor2.getTextArea().getHTML();
        if (debug)
            Window.alert("New Data box 2 is " + newDataBox2);
        String oldDataBox2 = convertedTextArea2.getText();
        if (debug)
            Window.alert("Old data box 2 is " + oldDataBox2);

        Revision rev1 = null;
        Revision rev2 = null;
        boolean updateBox2 = true;
        try {
            rev1 = Diff.diff(ToString.stringToArray(oldDataBox1), ToString.stringToArray(newDataBox1));
            rev2 = Diff.diff(ToString.stringToArray(oldDataBox2), ToString.stringToArray(newDataBox2));
            if (rev1 != null) {
                if ((rev2 != null) && (rev2.size() > 0))
                    relocateRevision(rev1, rev2);
                if (debug)
                    Window.alert(rev1.toRCSString());
                for (int i = 0; i < rev1.size(); i++) {
                    Delta delta = rev1.getDelta(i);
                    Chunk orig = delta.getOriginal();
                    Chunk revised = delta.getRevised();

                    if (debug)
                        Window.alert("Orig chunk: " + orig.toString());
                    if (debug)
                        Window.alert("Revised chunk: " + revised.toString());
                }

                // Trying to apply change to right box
                String rta2Html = editor2.getTextArea().getHTML();
                if (debug)
                    Window.alert("Text to patch: " + rta2Html);
                String newRta2Html = ToString.arrayToString(rev1.patch(ToString.stringToArray(rta2Html)));
                editor2.getTextArea().setHTML(newRta2Html);
                if (debug)
                    Window.alert("Data is second html box is " + editor2.getTextArea().getHTML());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Window.alert("Failed to apply the patch in box 2. Erasing box 2.");
            updateBox2 = false;
        }

        boolean updateBox1 = true;
        try {
            if ((rev1 != null) && (rev1.size() > 0))
                relocateRevision(rev2, rev1);
            if (rev2 != null) {
                if (debug)
                    Window.alert(rev2.toRCSString());
                for (int i = 0; i < rev2.size(); i++) {
                    Delta delta = rev2.getDelta(i);
                    Chunk orig = delta.getOriginal();
                    Chunk revised = delta.getRevised();

                    if (debug)
                        Window.alert("Orig chunk: " + orig.toString());
                    if (debug)
                        Window.alert("Revised chunk: " + revised.toString());
                }

                // Trying to apply change to right box
                String rtaHtml = editor1.getTextArea().getHTML();
                if (debug)
                    Window.alert("Text to patch: " + rtaHtml);
                String newRtaHtml = ToString.arrayToString(rev2.patch(ToString.stringToArray(rtaHtml)));
                editor1.getTextArea().setHTML(newRtaHtml);
                if (debug)
                    Window.alert("Data is second html box is " + editor1.getTextArea().getHTML());
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (updateBox2 == false)
                Window.alert("Failed to apply the patch in box 1.");
            else
                Window.alert("Failed to apply the patch in box 1. Erasing box 1.");
            updateBox1 = false;
        }

        if (updateBox2 == false) {
            editor2.getTextArea().setHTML(editor1.getTextArea().getHTML());
        } else if (updateBox1 == false) {
            editor1.getTextArea().setHTML(editor2.getTextArea().getHTML());
        }

        convertedTextArea.setText(editor1.getTextArea().getHTML());
        convertedTextArea2.setText(editor1.getTextArea().getHTML());

        WysiwygService.Singleton.getInstance().fromHTML(editor1.getTextArea().getHTML(), "xwiki/2.0",
            new XWikiAsyncCallback(wysiwyg)
            {
                public void onSuccess(Object result)
                {
                    super.onSuccess(result);
                    wikiTextArea.setText((String) result);
                }
            });
    }

    /**
     * This will relocate the patches on rev2 based on changes in rev1
     * 
     * @param rev2
     * @param rev1
     */
    private void relocateRevision(Revision rev2, Revision rev1)
    {
        for (int i = 0; i < rev1.size(); i++) {
            Delta delta = rev1.getDelta(i);
            Chunk orig = delta.getOriginal();
            Chunk revised = delta.getRevised();
            if (orig.size() != revised.size()) {
                int position = orig.anchor();
                int deltaSize = revised.size() - orig.size();
                for (int j = 0; j < rev2.size(); j++) {
                    Delta delta2 = rev2.getDelta(i);
                    Chunk orig2 = delta2.getOriginal();
                    Chunk revised2 = delta2.getRevised();
                    if (orig2.anchor() >= position) {
                        orig2.moveAnchor(deltaSize);
                        revised2.moveAnchor(deltaSize);
                    }
                }
            }
        }
    }
}
