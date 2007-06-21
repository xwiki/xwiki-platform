package com.xpn.xwiki.watch.client.ui.wizard;

import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.Constants;
import com.xpn.xwiki.watch.client.ui.dialog.*;
import com.xpn.xwiki.gwt.api.client.dialog.ChoiceDialog;
import com.xpn.xwiki.gwt.api.client.dialog.Dialog;
import com.xpn.xwiki.gwt.api.client.dialog.MessageDialog;
import com.xpn.xwiki.gwt.api.client.dialog.ChoiceInfo;
import com.xpn.xwiki.gwt.api.client.wizard.Wizard;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;
import java.util.ArrayList;

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

public class PressReviewWizard extends Wizard {
    protected String[] prPages;
    protected PressReviewDialog pressReviewDialog;
    protected String pressReviewOutput;
    protected String pressReviewPage;

    public PressReviewWizard(Watch watch, AsyncCallback callback) {
        super(watch, callback);

        ChoiceDialog choosePressReviewOutputDialog = new ChoiceDialog(watch, "chooseproutput", Dialog.BUTTON_CANCEL | Dialog.BUTTON_NEXT, false);
        choosePressReviewOutputDialog.addChoice("web");
        choosePressReviewOutputDialog.addChoice("rss");
        choosePressReviewOutputDialog.addChoice("pdf");
        choosePressReviewOutputDialog.addChoice("email");
        addDialog(choosePressReviewOutputDialog);

        ChoiceDialog choosePressReviewDialog = new ChoiceDialog(watch, "chooseprpage", Dialog.BUTTON_PREVIOUS | Dialog.BUTTON_CANCEL | Dialog.BUTTON_NEXT, true);
        String[] prPages = watch.getPressReviewPages();
        if (prPages.length>1) {
            for (int i=0;i<prPages.length;i++) {
                choosePressReviewDialog.addChoice(prPages[i]);
            }
            addDialog(choosePressReviewDialog);
        }

        pressReviewDialog = new PressReviewDialog(watch, "preview", Dialog.BUTTON_PREVIOUS | Dialog.BUTTON_CANCEL | Dialog.BUTTON_NEXT, Constants.DEFAULT_CODE_SPACE + "." + Constants.PAGE_PRESSREVIEW);
        addDialog(pressReviewDialog);

        MessageDialog webDialog = new MessageDialog(watch, "web", Dialog.BUTTON_PREVIOUS | Dialog.BUTTON_CANCEL);
        webDialog.setCancelText("close");
        String[] args = new String[1];
        args[0] = watch.getViewUrl(Constants.DEFAULT_CODE_SPACE + "." + Constants.PAGE_PRESSREVIEW, "space=" + watch.getWatchSpace() + watch.getFilterStatus().getQueryString());
        webDialog.setMessage("web", args);
        addDialog(webDialog);

        MessageDialog rssDialog = new MessageDialog(watch, "rss", Dialog.BUTTON_PREVIOUS | Dialog.BUTTON_CANCEL);
        rssDialog.setCancelText("close");
        args = new String[1];
        args[0] = watch.getViewUrl(Constants.DEFAULT_CODE_SPACE + "." + Constants.PAGE_PRESSREVIEW_RSS, "space=" + watch.getWatchSpace() + watch.getFilterStatus().getQueryString() + "&amp;xpage=rdf");
        rssDialog.setMessage("rss", args);
        addDialog(rssDialog);

        MessageDialog pdfDialog = new MessageDialog(watch, "pdf", Dialog.BUTTON_PREVIOUS | Dialog.BUTTON_CANCEL);
        pdfDialog.setCancelText("close");
        args = new String[1];
        args[0] = watch.getPDFUrl(Constants.DEFAULT_CODE_SPACE + "." + Constants.PAGE_PRESSREVIEW, "space=" + watch.getWatchSpace() + watch.getFilterStatus().getQueryString());
        pdfDialog.setMessage("pdf", args);
        addDialog(pdfDialog);

        // TODO: email dialog
        PressReviewMailDialog pressReviewMailDialog = new PressReviewMailDialog(watch, "email", Dialog.BUTTON_PREVIOUS | Dialog.BUTTON_CANCEL | Dialog.BUTTON_NEXT);
        addDialog(pressReviewMailDialog);

        /*
        MessageDialog messageDialog = new MessageDialog(watch, "end", Dialog.BUTTON_CANCEL);
        messageDialog.setCancelText("close");
        addDialog(messageDialog, "");
        */
    }

    /**
     * Move status to the right dialog
     * Either the first one (if status < 0)
     * Either the result of a choice (if we find a dialog named by the choice)
     * Either the last one (if there is no dialog named by the choice)
     * Or the next one (if there wasn't a choice
     */
    protected void updateStatus() {
        Dialog currentDialog = (status==-1) ? null : (Dialog) dialogs.get(status);
        String currentDialogName = (currentDialog == null) ? "" : currentDialog.getName();

        // If the status is -1 then we will launche the first dialog (status=0)
        if (status>=0) {
            Object data = getData();
            // If the return of the current dialog is a choice
            // Then we should go to dialog named by the choice
            // If we cannot find such a dialog we will end the wizard
            if (data instanceof ChoiceInfo) {
                String choice = ((ChoiceInfo) getData()).getName();
                if (currentDialogName.equals("chooseproutput")) {
                    pressReviewOutput = choice;
                    if (choice.equals("pdf")||choice.equals("email")||choice.equals("web")) {
                        // move to choosing the pr type if it exists
                        // otherwise move directly to preview
                        status = getStatusIndex("chooseprpage");
                        if (status==100) {
                            status = getStatusIndex("preview");
                        }
                        return;
                    } else {
                        status = getStatusIndex("preview");
                        return;
                    }
                } else if (currentDialogName.equals("chooseprpage")) {
                    // go to preview page
                    pressReviewPage = choice;
                    pressReviewDialog.setPressReviewPage(pressReviewPage);
                    status = getStatusIndex("preview");
                } else {
                    status = getStatusIndex(choice);
                    return;
                }

            }  else if (currentDialogName.equals("preview")) {
                // Go to the rss/email/pdf page
                status = getStatusIndex(pressReviewOutput);
                return;
            } else {
                // If we have a current dialog we will ask the dialog what the next dialog name is
                // If the next dialog is unknow we will end the wizard
                if (currentDialog!=null) {
                    String nextDialogName = getNextDialog(currentDialogName);
                    if (nextDialogName!=null) {
                        status = getStatusIndex(nextDialogName);
                    }
                }
            }
        }
        status++;
    }

    /**
     * Finding the dialog position based on it's name
     * @param dialogName
     * @return
     */
    protected int getStatusIndex(String dialogName) {
         for(int i=0;i<dialogs.size();i++) {
             Dialog dialog = (Dialog) dialogs.get(i);
             if (dialogName.equals(dialog.getName()))
              return i;
         }
         return 100;
    }

    public String getPressReviewOutput() {
        return pressReviewOutput;
    }

    public String getPressReviewPage() {
        return pressReviewPage;
    }

}
