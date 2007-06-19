package com.xpn.xwiki.watch.client.ui.dialog;

import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
import com.xpn.xwiki.watch.client.Feed;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;

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

public class StandardFeedDialog extends FeedDialog {
    protected TextBox feedNameTextBox;
    protected TextBox feedURLTextBox;

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes WatchDialog.BUTTON_CANCEL|WatchDialog.BUTTON_NEXT for Cancel / Next
     */
    public StandardFeedDialog(XWikiGWTApp app, String name, int buttonModes, Feed feed) {
        super(app, name, buttonModes, feed);
    }

    protected boolean updateFeed() {
        feed.setName(feedNameTextBox.getText());
        feed.setUrl(feedURLTextBox.getText());
        List groups = new ArrayList();
        for (int i=0;i<groupsListBox.getItemCount();i++) {
            if (groupsListBox.isItemSelected(i))
             groups.add(groupsListBox.getValue(i));
        }
        feed.setGroups(groups);
                       
        if (feed.getUrl().equals("")) {
            Window.alert(app.getTranslation(getDialogTranslationName() + ".nofeedurl"));
            return false;
        }

        if (feed.getName().equals("")) {
            Window.alert(app.getTranslation(getDialogTranslationName() + ".nofeedname"));
            return false;
        }

        return true;
    }

    protected Widget getParametersPanel() {
        FlowPanel paramsPanel = new FlowPanel();
        Label feedNameLabel = new Label();
        feedNameLabel.setStyleName("mailsubject-label");
        feedNameLabel.setText(app.getTranslation(getDialogTranslationName() + ".feedname"));
        paramsPanel.add(feedNameLabel);
        feedNameTextBox = new TextBox();
        if ((feed!=null)&&(feed.getName()!=null))
            feedNameTextBox.setText(feed.getName());
        feedNameTextBox.setVisibleLength(60);
        feedNameTextBox.setName("feedname");
        feedNameTextBox.setStyleName(getCSSName("feedname"));
        paramsPanel.add(feedNameTextBox);
        Label feedURLLabel = new Label();
        feedURLLabel.setStyleName("feedurl-label");
        feedURLLabel.setText(app.getTranslation(getDialogTranslationName() + ".feedurl"));
        paramsPanel.add(feedURLLabel);
        feedURLTextBox = new TextBox();
        if ((feed!=null)&&(feed.getUrl()!=null))
            feedURLTextBox.setText(feed.getUrl());
        feedURLTextBox.setVisibleLength(60);
        feedURLTextBox.setName("feedurl");
        feedURLTextBox.setStyleName(getCSSName("feedurl"));
        paramsPanel.add(feedURLTextBox);
        paramsPanel.add(getGroupsFields());
        return paramsPanel;
    }

}
