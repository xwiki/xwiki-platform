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
package com.xpn.xwiki.gwt.api.client.dialog;

import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.DOM;

public class ChoiceDialog extends Dialog {
    FlowPanel buttonPanel = new FlowPanel();
    FlowPanel helpPanel = new FlowPanel();
    SimplePanel helpHeader = new SimplePanel();
    SimplePanel helpContent = new SimplePanel();
    boolean autoSelect;

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     */
    public ChoiceDialog(XWikiGWTApp app, String name, int buttonModes, boolean autoSelect) {
        this(app, name, buttonModes, autoSelect, null);
    }

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     * @param nextCallback Callback when dialog is finished
     */
    public ChoiceDialog(XWikiGWTApp app, String name, int buttonModes, boolean autoSelect, AsyncCallback nextCallback) {
        super(app, name, buttonModes, nextCallback);
        this.autoSelect = autoSelect;

        FlowPanel main = new FlowPanel();
        main.addStyleName(getCSSName("main"));

        HTMLPanel invitationPanel = new HTMLPanel(app.getTranslation(getDialogTranslationName() + ".invitation"));
        invitationPanel.addStyleName(getCssPrefix() + "-invitation");
        main.add(invitationPanel);

        buttonPanel.addStyleName(getCSSName("buttons"));
        main.add(buttonPanel);

        helpHeader.addStyleName(getCSSName("help", "head"));
        helpContent.addStyleName(getCSSName("help", "content"));
        helpPanel.addStyleName(getCSSName("help"));
        main.add(helpPanel);

        main.add(getActionsPanel());
        add(main);
    }

    public void addChoice(String name) {
            ChoiceInfo choiceinfo = new ChoiceInfo(app, getDialogTranslationName(), name);
            addChoice(choiceinfo);
    }

    public void addChoice(ChoiceInfo choice) {
            ChoiceDialog.ChoiceButton button = new ChoiceDialog.ChoiceButton(choice, new ClickListener() {
                @Override
                public void onClick(Widget widget) {
                    onChoiceClick((ChoiceDialog.ChoiceButton)widget);
                }
            });
            buttonPanel.add(button);
    }

    public void onChoiceClick(ChoiceDialog.ChoiceButton choiceButton) {
            setCurrentResult(choiceButton.getChoiceInfo());
            if (autoSelect) {
              endDialog();
            } else {
                setActive(choiceButton);
                choiceButton.showDescription();
            }
    }

    private class ChoiceButton extends Button {
        ChoiceInfo choiceInfo;

        public ChoiceButton(ChoiceInfo choiceInfo, ClickListener callback){
            super(choiceInfo.getTitle(), callback);
            this.choiceInfo = choiceInfo;
            addStyleName(getCSSName("button", choiceInfo.getCSSName()));
            sinkEvents(Event.ONMOUSEOVER);
        }

        public ChoiceInfo getChoiceInfo() {
            return choiceInfo;
        }

        @Override
        public void onBrowserEvent(Event event) {
            switch (DOM.eventGetType(event)) {
                case Event.ONMOUSEOVER:
                    if (autoSelect) {
                     setActive(this);
                     showDescription();
                    }
                    break;
            }

            super.onBrowserEvent(event);
        }

        public void showDescription(){
            helpPanel.clear();
            HTMLPanel textPanel = new HTMLPanel(choiceInfo.getTitle());
            textPanel.setStyleName(getCSSName("help", "content-text"));
            helpPanel.add(textPanel);
            String imageURL = choiceInfo.getImageURL();
            Image image = new Image();
            image.setStyleName(getCSSName("help", "content-image"));
            image.setTitle(choiceInfo.getTitle());
            if ((imageURL!=null)&&(!imageURL.equals(""))) {
                image.setUrl(imageURL);
            }
            helpPanel.add(image);
            HTMLPanel descPanel = new HTMLPanel(choiceInfo.getDescription());
            descPanel.setStyleName(getCSSName("help", "content-description"));
            helpPanel.add(descPanel);
        }
    }

    private void setActive(Button button) {
        for (int i=0;i< buttonPanel.getWidgetCount();i++) {
            Widget widget = buttonPanel.getWidget(i);
            if (widget instanceof ChoiceDialog.ChoiceButton) {
                ChoiceDialog.ChoiceButton choiceButton = (ChoiceDialog.ChoiceButton) widget;
                widget.removeStyleName(getCSSName("button", choiceButton.getChoiceInfo().getCSSName() + "-active"));
            }
        }
        ((ChoiceDialog.ChoiceButton)button).addStyleName(getCSSName("button", ((ChoiceDialog.ChoiceButton)button).getChoiceInfo().getCSSName() + "-active"));
    }
}
