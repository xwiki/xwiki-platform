package com.xpn.xwiki.watch.client.ui.wizard;

import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.ui.dialog.MessageDialog;
import com.xpn.xwiki.watch.client.ui.dialog.WatchDialog;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;

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

public class PressReviewMailDialog extends WatchDialog {
    protected TextBox mailSubjectTextBox;
    protected TextBox mailToTextBox;
    protected TextArea mailContentTextArea;
    
    public PressReviewMailDialog(Watch watch, String dialogName, int buttonModes) {
        super(watch, dialogName, buttonModes);
        FlowPanel main = new FlowPanel();
        main.addStyleName(getCSSName("main"));

        HTMLPanel invitationPanel = new HTMLPanel(app.getTranslation(getDialogTranslationName() + ".invitation"));
        invitationPanel.addStyleName(getCssPrefix() + "-invitation");
        main.add(invitationPanel);
        main.add(getParametersPanel());
        main.add(getActionsPanel());
        add(main);

    }

    protected Widget getParametersPanel() {
        FlowPanel paramsPanel = new FlowPanel();
        Label mailSubjectLabel = new Label();
        mailSubjectLabel.setStyleName("mailsubject-label");
        mailSubjectLabel.setText(app.getTranslation(getDialogTranslationName() + ".mailsubject"));
        paramsPanel.add(mailSubjectLabel);

        mailSubjectTextBox = new TextBox();
        mailSubjectTextBox.setVisibleLength(60);
        mailSubjectTextBox.setName("mailsubject");
        mailSubjectTextBox.setStyleName(getCSSName("mailsubject"));
        mailSubjectTextBox.setText(app.getTranslation(getDialogTranslationName() + ".mailsubjectdefault"));
        paramsPanel.add(mailSubjectTextBox);

        Label mailToLabel = new Label();
        mailToLabel.setStyleName("mailto-label");
        mailToLabel.setText(app.getTranslation(getDialogTranslationName() + ".mailto"));
        paramsPanel.add(mailToLabel);

        mailToTextBox = new TextBox();
        mailToTextBox.setVisibleLength(60);
        mailToTextBox.setName("mailto");
        mailToTextBox.setStyleName(getCSSName("mailto"));
        paramsPanel.add(mailToTextBox);

        Label mailContentLabel = new Label();
        mailContentLabel.setStyleName("mailcontent-label");
        mailContentLabel.setText(app.getTranslation(getDialogTranslationName() + ".mailcontent"));
        paramsPanel.add(mailContentLabel);

        mailContentTextArea = new TextArea();
        mailContentTextArea.setVisibleLines(5);
        mailContentTextArea.setName("mailcontent");
        mailContentTextArea.setStyleName(getCSSName("mailcontent"));
        mailContentTextArea.setText(app.getTranslation(getDialogTranslationName() + ".mailcontentdefault"));
        paramsPanel.add(mailContentTextArea);
        return paramsPanel;
    }

    protected void endDialog() {
        Window.alert(app.getTranslation(getDialogTranslationName() + ".notimplemented"));
    }

}
