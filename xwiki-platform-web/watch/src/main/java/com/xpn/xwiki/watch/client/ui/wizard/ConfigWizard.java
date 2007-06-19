package com.xpn.xwiki.watch.client.ui.wizard;

import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.Feed;
import com.xpn.xwiki.watch.client.ui.dialog.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

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

public class ConfigWizard extends WatchWizard {

    public ConfigWizard(Watch watch, AsyncCallback callback) {
        super(watch, callback);

        ChoiceDialog chooseConfigDialog = new ChoiceDialog(watch, "configtype", WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, true);
        chooseConfigDialog.addChoice("addfeed");
        chooseConfigDialog.addChoice("addkeyword");
        chooseConfigDialog.addChoice("addgroup");
        chooseConfigDialog.addChoice("loadingstatus");
        addDialog(chooseConfigDialog);

        ChoiceDialog chooseFeedTypeDialog = new ChoiceDialog(watch, "addfeed", WatchDialog.BUTTON_PREVIOUS | WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, true);
        chooseFeedTypeDialog.addChoice("standard");
        chooseFeedTypeDialog.addChoice("googlenews");
        chooseFeedTypeDialog.addChoice("googleblog");
        chooseFeedTypeDialog.addChoice("technoratitag");
        chooseFeedTypeDialog.addChoice("feedster");
        chooseFeedTypeDialog.addChoice("wikio");
        addDialog(chooseFeedTypeDialog);

        StandardFeedDialog standardFeedDialog = new StandardFeedDialog(watch, "standard", WatchDialog.BUTTON_PREVIOUS | WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, new Feed());
        addDialog(standardFeedDialog, "end");

        String[] languages = { "en", "fr" };
        addSearchEngineDialog("googlenews", "http://news.google.fr/news?hl={1}&ned={1}&ie=UTF-8&output=atom&num=40&q={0}", languages);
        addSearchEngineDialog("googleblog", "http://blogsearch.google.com/blogsearch_feeds?hl={1}&tab=wb&ie=utf-8&num=40&output=atom&q={0}", languages);
        addSearchEngineDialog("technoratitag", "http://feeds.technorati.com/feed/posts/tag/{0}");
        addSearchEngineDialog("feedster", "http://www.feedster.com/search/type/rss/{0}");
        addSearchEngineDialog("wikio", "http://rss.wikio.fr/search/{0}.rss");

        AddKeywordDialog addKeywordDialog = new AddKeywordDialog(watch, "addkeyword", WatchDialog.BUTTON_PREVIOUS | WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, "", "");
        addDialog(addKeywordDialog, "end");

        AddGroupDialog addGroupDialog = new AddGroupDialog(watch, "addgroup", WatchDialog.BUTTON_PREVIOUS | WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, "");
        addDialog(addGroupDialog, "end");

        LoadingStatusDialog loadingStatusDialog = new LoadingStatusDialog(watch, "loadingstatus", WatchDialog.BUTTON_CANCEL);
        loadingStatusDialog.setCancelText("close");
        addDialog(loadingStatusDialog, "");

        MessageDialog messageDialog = new MessageDialog(watch, "end", WatchDialog.BUTTON_CANCEL);
        messageDialog.setCancelText("close");
        addDialog(messageDialog, "");
    }

    private void addSearchEngineDialog(String dialogName, String baseURL) {
        addSearchEngineDialog(dialogName, baseURL, null);
    }

    private void addSearchEngineDialog(String dialogName, String baseURL, String[] languages) {
        SearchEngineFeedDialog searchEngineFeedDialog = new SearchEngineFeedDialog(watch, dialogName, WatchDialog.BUTTON_PREVIOUS | WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, new Feed(),baseURL,languages);
        addDialog(searchEngineFeedDialog, "end");
    }

    /**
     * Move status to the right dialog
     * Either the first one (if status < 0)
     * Either the result of a choice (if we find a dialog named by the choice)
     * Either the last one (if there is no dialog named by the choice)
     * Or the next one (if there wasn't a choice
     */
    protected void updateStatus() {
        // If the status is -1 then we will launche the first dialog (status=0)
        if (status>=0) {
            Object data = getData();
            // If the return of the current dialog is a choice
            // Then we should go to dialog named by the choice
            // If we cannot find such a dialog we will end the wizard
            if (data instanceof ChoiceInfo) {
                String choice = ((ChoiceInfo) getData()).getName();
                status = getStatusIndex(choice);
                return;
            } else {
                // If we have a current dialog we will ask the dialog what the next dialog name is
                // If the next dialog is unknow we will end the wizard
                WatchDialog currentDialog = (WatchDialog) dialogs.get(status);
                if (currentDialog!=null) {
                    String nextDialogName = getNextDialog(currentDialog.getName());
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
             WatchDialog dialog = (WatchDialog) dialogs.get(i);
             if (dialogName.equals(dialog.getName()))
              return i;
         }
         return 100;
    }
}
