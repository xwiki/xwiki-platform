package com.xpn.xwiki.watch.client.ui.dialog;

import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.dialog.Dialog;
import com.xpn.xwiki.watch.client.Feed;
import com.xpn.xwiki.watch.client.Watch;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;
import java.util.Map;
import java.util.Iterator;

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

public abstract class FeedDialog extends Dialog {
    protected Feed feed;
    protected ListBox groupsListBox = new ListBox();
    protected String[] languages;

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     */
    public FeedDialog(XWikiGWTApp app, String name, int buttonModes, Feed feed) {
        this(app, name, buttonModes, feed, null);
    }

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     * @languages list of available languages
     */
    public FeedDialog(XWikiGWTApp app, String name, int buttonModes, Feed feed, String[] languages) {
        super(app, name, buttonModes);
        this.languages = languages;
        this.feed = feed;

        FlowPanel main = new FlowPanel();
        main.addStyleName(getCSSName("main"));

        HTMLPanel invitationPanel = new HTMLPanel(app.getTranslation(getDialogTranslationName() + ".invitation"));
        invitationPanel.addStyleName(getCssPrefix() + "-invitation");
        main.add(invitationPanel);
        main.add(getParametersPanel());
        main.add(getActionsPanel());
        add(main);
    }

    protected void endDialog() {
        if (updateFeed()) {
            setCurrentResult(feed);
            if (feed.getPageName().equals("")) {
                ((Watch)app).addFeed(feed, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        // There should already have been an error display
                        ((Watch)app).refreshConfig();
                    }

                    public void onSuccess(Object object) {
                        endDialog2();
                        ((Watch)app).refreshConfig();
                        // this will force a reload of feeds on the server
                        ((Watch)app).forceServerLoading();
                    }
                });
            } else {
                endDialog2();
            }
        }
    }

    private void endDialog2() {
        super.endDialog();
    }

    protected Widget getGroupsFields() {
        FlowPanel groupsPanel = new FlowPanel();
        Label groupLabel = new Label();
        groupLabel.setStyleName("groups-label");
        groupLabel.setText(app.getTranslation(getDialogTranslationName() + ".groups"));
        groupsPanel.add(groupLabel);
        List currentGroups = feed.getGroups();
        groupsListBox.setMultipleSelect(true);
        Map groupMap = ((Watch)app).getConfig().getGroups();
        Iterator it = groupMap.keySet().iterator();
        while (it.hasNext()) {
            String groupname = (String) it.next();
            String all = ((Watch)app).getTranslation("all");
            if (!groupname.equals(all)) {
                String grouptitle = (String) groupMap.get(groupname);
                if (groupname.indexOf(".")==-1)
                 grouptitle = "[" + grouptitle + "]";
                groupsListBox.addItem(grouptitle, groupname);
                if (currentGroups.contains(groupname)) {
                    groupsListBox.setItemSelected(groupsListBox.getItemCount()-1, true);
                }
            }
        }
        groupsPanel.add(groupsListBox);
        return groupsPanel;
    }

    protected abstract boolean updateFeed();
    protected abstract Widget getParametersPanel();


}
