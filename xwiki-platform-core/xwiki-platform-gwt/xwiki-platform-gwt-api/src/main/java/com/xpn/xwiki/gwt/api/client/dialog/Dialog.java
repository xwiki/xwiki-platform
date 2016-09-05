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
import com.xpn.xwiki.gwt.api.client.wizard.Wizard;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;

public class Dialog extends DefaultDialog {
    public static int BUTTON_CANCEL = 1;
    public static int BUTTON_PREVIOUS = 2;
    public static int BUTTON_NEXT = 4;

    protected XWikiGWTApp app;
    protected AsyncCallback nextCallback;

    private String name;
    private String dialogTranslationName;
    private String cssPrefix;

    private int buttonModes;
    protected Object currentResult;
    protected Wizard wizard;
    protected String cancelText = "cancel";
    protected String previousText = "back";
    protected String nextText = "next";

    /**
     * Dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     */
    public Dialog(XWikiGWTApp app, String name, int buttonModes) {
        this(app, name, buttonModes, null);
    }

    /**
     * Dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     * @param nextCallback Callback when dialog is finished
     */
    public Dialog(XWikiGWTApp app, String name, int buttonModes, AsyncCallback nextCallback) {
        super(false, true);
        this.app = app;
        this.nextCallback = nextCallback;
        this.name = name;
        this.cssPrefix = name;
        this.dialogTranslationName = name;
        this.buttonModes = buttonModes;
        addStyleName(getCSSName(null));
        setText(app.getTranslation(dialogTranslationName + ".caption"));
    }

    public void setAsyncCallback(AsyncCallback nextCallback) {
        this.nextCallback = nextCallback;
    }

    public void setWizard(Wizard wizard) {
        this.wizard = wizard;
    }

    public Wizard getWizard() {
        return wizard;
    }

    public String getCSSName(String name) {
        return app.getCSSPrefix() + "-" + cssPrefix + ((name==null) ? "" : ("-" + name));
    }

    public String getCSSName(String module, String name) {
        return app.getCSSPrefix() + "-" + cssPrefix + "-" + module + "-" + name;
    }

    public Object getCurrentResult() {
        return currentResult;
    }

    public void setCurrentResult(Object result) {
        currentResult = result;
    }

    protected Panel getActionsPanel() {
        FlowPanel actionsPanel = new FlowPanel();
        actionsPanel.addStyleName(getCSSName("actions"));

        if ((buttonModes & BUTTON_PREVIOUS) == BUTTON_PREVIOUS) {
            String previousName = getPreviousText();
            ClickListener cancelListener = new ClickListener(){
                @Override
                public void onClick(Widget sender){
                    cancelDialogAsBack();
                }
            };

            Button cancel = new Button(app.getTranslation("button." + previousName));
            cancel.addClickListener(cancelListener);
            cancel.addStyleName(getCSSName(previousName));
            cancel.addStyleName(app.getCSSPrefix() + "-" + previousName);
            actionsPanel.add(cancel);
        }

        if ((buttonModes & BUTTON_CANCEL) == BUTTON_CANCEL) {
            String cancelName = getCancelText();
            ClickListener cancelListener = new ClickListener(){
                @Override
                public void onClick(Widget sender){
                    cancelDialog();
                }
            };
            Button cancel = new Button(app.getTranslation("button." + cancelName));
            cancel.addClickListener(cancelListener);
            cancel.addStyleName(getCSSName(cancelName));
            cancel.addStyleName(app.getCSSPrefix() + "-" + cancelName);
            actionsPanel.add(cancel);
        }

        if ((buttonModes & BUTTON_NEXT) == BUTTON_NEXT) {
            String nextName = getNextText();
            ClickListener buttonListener = new ClickListener(){
                @Override
                public void onClick(Widget sender){
                    endDialog();
                }
            };
            Button button = new Button(app.getTranslation("button." + nextName));
            button.addClickListener(buttonListener);
            button.addStyleName(getCSSName(nextName));
            button.addStyleName(app.getCSSPrefix() + "-" + nextName);
            actionsPanel.add(button);
        }

        return actionsPanel;
    }

    protected void endDialog() {
            if (currentResult!=null) {
                hide();
                if (wizard!=null) {
                    wizard.nextStep(getCurrentResult());
                } else {
                    // if we end the dialog we call previousStep with the current state
                    if (nextCallback!=null)
                        nextCallback.onSuccess(getCurrentResult());
                }
            } else {
                String message = app.getTranslation(dialogTranslationName + ".noselection");
                if ((message!=null)&&(!message.equals("")))
                 Window.alert(message);
            }
    }

    protected void cancelDialog() {
        hide();
        if (wizard!=null) {
            wizard.cancel();
        } else {
            // If we cancel we call onFailure with null
            if (nextCallback!=null)
               nextCallback.onFailure(null);
        }
    }
    protected void cancelDialogAsBack() {
        hide();
        if (wizard!=null) {
            wizard.previousStep();
        } else {
            // This will probably never be used if there is no wizard
            // If we ask previousStep we call onSuccess with null
            if (nextCallback!=null)
               nextCallback.onSuccess(null);
        }
    }

    public String getCancelText() {
        return cancelText;
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }

    public String getPreviousText() {
        return previousText;
    }

    public void setPreviousText(String previousText) {
        this.previousText = previousText;
    }

    public String getNextText() {
        return nextText;
    }

    public void setNextText(String nextText) {
        this.nextText = nextText;
    }

    public String getName() {
        return dialogTranslationName;
    }

    public AsyncCallback getNextCallback() {
        return nextCallback;
    }

    public void setNextCallback(AsyncCallback nextCallback) {
        this.nextCallback = nextCallback;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDialogTranslationName() {
        return dialogTranslationName;
    }

    public void setDialogTranslationName(String dialogTranslationName) {
        this.dialogTranslationName = dialogTranslationName;
        setText(app.getTranslation(dialogTranslationName + ".caption"));
    }

    public String getCssPrefix() {
        return cssPrefix;
    }

    public void setCssPrefix(String cssPrefix) {
        this.cssPrefix = cssPrefix;
    }

    public int getButtonModes() {
        return buttonModes;
    }

    public void setButtonModes(int buttonModes) {
        this.buttonModes = buttonModes;
    }
}
