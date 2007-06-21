package com.xpn.xwiki.watch.client.ui.dialog;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
import com.xpn.xwiki.gwt.api.client.dialog.Dialog;
import com.xpn.xwiki.watch.client.Watch;

/**
 * Copyright 2006,XpertNet SARL,and individual contributors as indicated
 * by the contributors.txt.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 *
 * @author ldubost
 */

public class CommentAddDialog extends Dialog {
    protected TextArea commentTextArea;

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     */
    public CommentAddDialog(XWikiGWTApp app, String name, int buttonModes) {
        super(app, name, buttonModes);

        FlowPanel main = new FlowPanel();
        main.addStyleName(getCSSName("main"));

        HTMLPanel invitationPanel = new HTMLPanel(app.getTranslation(getDialogTranslationName() + ".invitation"));
        invitationPanel.addStyleName(getCssPrefix() + "-invitation");
        main.add(invitationPanel);
        main.add(getParametersPanel());
        main.add(getActionsPanel());
        add(main);
    }

    protected boolean updateData() {
        String comment = commentTextArea.getText();

        if (comment.equals("")) {
            Window.alert(app.getTranslation(getDialogTranslationName() + ".nocomment"));
            return false;
        }

        setCurrentResult(comment);
        return true;
    }

    protected Widget getParametersPanel() {
        FlowPanel paramsPanel = new FlowPanel();
        Label commentLabel = new Label();
        commentLabel.setStyleName("comment-label");
        commentLabel.setText(app.getTranslation(getDialogTranslationName() + ".comment"));
        paramsPanel.add(commentLabel);
        commentTextArea = new TextArea();
        commentTextArea.setPixelSize(400, 300);
        commentTextArea.setName("comment");
        commentTextArea.setStyleName(getCSSName("comment"));
        paramsPanel.add(commentTextArea);
        return paramsPanel;
    }

    protected void endDialog() {
        if (updateData()) {
            getNextCallback().onSuccess(getCurrentResult());
        }
    }
}
