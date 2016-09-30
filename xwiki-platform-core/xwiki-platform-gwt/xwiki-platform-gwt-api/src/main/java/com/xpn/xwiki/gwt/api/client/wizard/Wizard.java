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
package com.xpn.xwiki.gwt.api.client.wizard;

import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
import com.xpn.xwiki.gwt.api.client.dialog.Dialog;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Wizard {
    protected XWikiGWTApp app;
    protected AsyncCallback callback;
    protected List dialogs = new ArrayList();
    protected Map nextDialog = new HashMap();
    protected int status = -1;
    protected Object data;
    protected List previousSteps = new ArrayList();


    public Wizard(XWikiGWTApp app, AsyncCallback callback) {
        this.app = app;
        this.callback = callback;
    }

    public void addDialog(Dialog dialog) {
        addDialog(dialog, null);
    }

    public void addDialog(Dialog dialog, String nextDialogName) {
        dialog.setWizard(this);
        dialogs.add(dialog);
        if (nextDialogName!=null)
         nextDialog.put(dialog.getName(), nextDialogName);
    }


    public int getStatus() {
        return status;
    }

    public void launchWizard() {
        nextStep(null);

    }

    public void nextStep(Object data) {
        // If there is a data object we are called by a dialog finishing it's step
        if (data!=null) {
            updateData(data);
        }

        // We call a function to update the status to find the next dialog
        updateStatus();

        // Show the next dialog unless we are finished
        previousSteps.add(status);
        if (status<dialogs.size()) {
            Dialog nextDialog = (Dialog) dialogs.get(status);
            nextDialog.show();
        } else {
            // We are finished we call the finishWizard function
            finishWizard();
        }
    }

    protected void setData(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    /**
     * Override this function to perform actions
     * at the end of the wizard
     */
    protected void finishWizard() {
        callback.onSuccess(getData());
    }

    /**
     * Override this function to perform actions
     * in case of wizard failure
     * @param caught
     */
    protected void failWizard(Throwable caught) {
        callback.onFailure(caught);
    }

    /**
     * Override this function to update the data object from the result object of the dialog
     * as well as to perform other functions like saving data to the server
     * Default behavior overwrite the current data and no other action
     * @param object
     */
    protected void updateData(Object object) {
        data = object;
    }

    /**
     * Override this function if we want to branch to a different dialog depending on the selection
     */
    protected void updateStatus() {
        status++;
    }

    /**
     * Cancel this wizard
     */
    public void cancel() {
        callback.onFailure(null);
    }

    /**
     * Called on a 'back'
     */
    public void previousStep() {
        if (previousSteps.size()>1) {
            previousSteps.remove(previousSteps.size()-1);
            status = ((Integer) previousSteps.get(previousSteps.size()-1)).intValue();
            Dialog previousDialog = (Dialog) dialogs.get(status);
            if (previousDialog!=null) {
                previousDialog.show();
            } else {
                // This is not possible
                failWizard(null);
            }

        } else {
            // This is not possible. It means there was a back on the first screen
            failWizard(null);
        }
    }

    public String getNextDialog(String dialogName) {
        return (String) nextDialog.get(dialogName);
    }
}
