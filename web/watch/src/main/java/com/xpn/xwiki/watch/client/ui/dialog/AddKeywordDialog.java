package com.xpn.xwiki.watch.client.ui.dialog;

import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
import com.xpn.xwiki.watch.client.Feed;
import com.xpn.xwiki.watch.client.Watch;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;
import java.util.ArrayList;
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

public class AddKeywordDialog extends WatchDialog {
    protected TextBox keywordTextBox = new TextBox();
    protected ListBox groupListBox = new ListBox();
    protected String keyword;
    protected String group;

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes WatchDialog.BUTTON_CANCEL|WatchDialog.BUTTON_NEXT for Cancel / Next
     */
    public AddKeywordDialog(XWikiGWTApp app, String name, int buttonModes, String keyword, String group) {
        super(app, name, buttonModes);
        this.keyword = keyword;
        this.group = group;

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
        keyword = keywordTextBox.getText();
        int selectedIndex = (groupListBox==null) ? -1 : groupListBox.getSelectedIndex();
        if (selectedIndex!=-1)
         group = groupListBox.getItemText(selectedIndex);

        if (keyword.equals("")) {
            Window.alert(app.getTranslation(getDialogTranslationName() + ".nokeyword"));
            return false;
        }

        return true;
    }

    protected Widget getParametersPanel() {
        FlowPanel paramsPanel = new FlowPanel();
        Label keywordLabel = new Label();
        keywordLabel.setStyleName("keyword-label");
        keywordLabel.setText(app.getTranslation(getDialogTranslationName() + ".keyword"));
        paramsPanel.add(keywordLabel);
        if (keyword!=null)
            keywordTextBox.setText(keyword);
        keywordTextBox.setVisibleLength(20);
        keywordTextBox.setName("keyword");
        keywordTextBox.setStyleName(getCSSName("keyword"));
        paramsPanel.add(keywordTextBox);
        paramsPanel.add(getGroupsFields());
        return paramsPanel;
    }

    protected Widget getGroupsFields() {
        FlowPanel groupsPanel = new FlowPanel();
        Label groupLabel = new Label();
        groupLabel.setStyleName("groups-label");
        groupLabel.setText(app.getTranslation(getDialogTranslationName() + ".groups"));
        groupsPanel.add(groupLabel);
        groupListBox.setMultipleSelect(false);
        Map groupMap = ((Watch)app).getConfig().getGroups();
        Iterator it = groupMap.keySet().iterator();
        while (it.hasNext()) {
            String groupname = (String) it.next();
            String all = ((Watch)app).getTranslation("all");
            if (!groupname.equals(all)) {
                String grouptitle = (String) groupMap.get(groupname);
                groupListBox.addItem(groupname, grouptitle);
                if (group.equals(groupname)) {
                    groupListBox.setItemSelected(groupListBox.getItemCount(), true);
                }
            }
        }
        groupsPanel.add(groupListBox);
        return groupsPanel;
    }

    protected void endDialog() {
        if (updateData()) {
            setCurrentResult(keyword);
            ((Watch)app).addKeyword(keyword, group, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    // There should already have been an error display
                    ((Watch)app).refreshConfig();
                }

                public void onSuccess(Object object) {
                    endDialog2();
                    ((Watch)app).refreshConfig();
                }
            });
        }
    }

    private void endDialog2() {
        super.endDialog();
    }

}
