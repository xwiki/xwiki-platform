package com.xpn.xwiki.watch.client.ui.menu;

import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.ui.WatchWidget;
import com.google.gwt.user.client.ui.*;

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

public class FilterBarWidget  extends WatchWidget {

    protected TagCloudWidget tagCloudWidget;
    protected KeywordsWidget keywordsWidget;

    public FilterBarWidget() {
        super();
    }

    public String getName() {
        return "filterbar";
    }
    
    public FilterBarWidget(Watch watch) {
        super(watch);
        setPanel(new FlowPanel());
        initWidget(panel);
        init();
        panel.add(getTitlePanel());
        panel.add(getInitFilterPanel());
        panel.add(getFilterPanel());
        panel.add(getKeywordsPanel());
        panel.add(getTagCloudPanel());
    }

    public void refreshData() {
    }

    public void resetSelections() {
        panel.clear();
        panel.add(getTitlePanel());
        panel.add(getInitFilterPanel());
        panel.add(getFilterPanel());
        panel.add(getKeywordsPanel());
        panel.add(getTagCloudPanel());
    }

    private Widget getTagCloudPanel() {
        if (tagCloudWidget==null)
         tagCloudWidget =  new TagCloudWidget(watch);
        return tagCloudWidget;
    }

    private Widget getKeywordsPanel() {
        if (keywordsWidget==null)
         keywordsWidget = new KeywordsWidget(watch);
        return keywordsWidget;
    }

    private Widget getSeeOnlyTitlePanel() {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("filter", "seeonly-title"));
        HTML titleHTML = new HTML(watch.getTranslation("filter.seeonly.title"));
        titleHTML.setStyleName(watch.getStyleName("filter", "title-seeonly-text"));
        p.add(titleHTML);
        return p;
    }


    private Widget getFilterPanel() {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("filter", "filter"));
        p.add(getSeeOnlyTitlePanel());
        p.add(getCheckBoxPanel("flagged", (watch.getFilterStatus().getFlagged()==1), new ClickListener() {
            public void onClick(Widget widget) {
                if (((CheckBox)widget).isChecked())
                 watch.refreshOnShowOnlyFlaggedArticles();
                else
                 watch.refreshOnNotShowOnlyFlaggedArticles();
            }
        }));
        p.add(getCheckBoxPanel("read", (watch.getFilterStatus().getRead()==1), new ClickListener() {
            public void onClick(Widget widget) {
                if (((CheckBox)widget).isChecked())
                 watch.refreshOnShowOnlyReadArticles();
                else
                 watch.refreshOnNotShowOnlyReadArticles();
            }
        }));
        p.add(getCheckBoxPanel("unread", (watch.getFilterStatus().getRead()==-1), new ClickListener() {
            public void onClick(Widget widget) {
                if (((CheckBox)widget).isChecked())
                 watch.refreshOnShowOnlyUnReadArticles();
                else
                 watch.refreshOnNotShowOnlyUnReadArticles();
            }
        }));
        p.add(getCheckBoxPanel("trashed", (watch.getFilterStatus().getTrashed()==1), new ClickListener() {
            public void onClick(Widget widget) {
                if (((CheckBox)widget).isChecked())
                 watch.refreshOnShowOnlyTrashedArticles();
                else
                 watch.refreshOnNotShowOnlyTrashedArticles();
            }
        }));
        return p;
    }

    private Widget getCheckBoxPanel(String name, boolean checked, ClickListener clickListener) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("filter", "seeonly-" + name));
        CheckBox checkBox = new CheckBox();
        if (checked)
            checkBox.setChecked(true);
        checkBox.setHTML(watch.getTranslation("filter.seeonly." + name));
        checkBox.addClickListener(clickListener);
        p.add(checkBox);
        return p;
    }

    private Widget getInitFilterPanel() {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("filter", "init"));
        HTML textHTML = new HTML(watch.getTranslation("filter.initfilter"));
        textHTML.setStyleName(watch.getStyleName("filter", "init-text"));
        textHTML.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.refreshOnResetFilter();
                resetSelections();
            }
        });
        p.add(textHTML);
        return p;
    }

    private Widget getTitlePanel() {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("filter", "title"));
        HTML titleHTML = new HTML(watch.getTranslation("filter.title"));
        titleHTML.setStyleName(watch.getStyleName("filter", "title-text"));
        titleHTML.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                // Show hide filter zone
            }
        });
        p.add(titleHTML);
        return p;
    }

    public void resizeWindow() {
        // Watch.setMaxHeight(panel);
    }
}
