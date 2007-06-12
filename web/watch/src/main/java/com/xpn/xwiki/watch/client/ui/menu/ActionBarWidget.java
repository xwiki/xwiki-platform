package com.xpn.xwiki.watch.client.ui.menu;

import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.Constants;
import com.xpn.xwiki.watch.client.ui.WatchWidget;
import com.xpn.xwiki.watch.client.ui.utils.ImageTextButton;
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

public class ActionBarWidget extends WatchWidget {
    private HTML title;
    private ImageTextButton pressReviewPanel;
    private ImageTextButton analysisPanel;
    private ImageTextButton hideReadArticlesPanel;
    private ImageTextButton showReadArticlesPanel;


    public ActionBarWidget() {
        super();
    }

    public String getName() {
        return "actionbar";
    }

    public ActionBarWidget(Watch watch) {
        super(watch);
        setPanel(new FlowPanel());
        initWidget(panel);
        init();
    }

    public void init() {
        super.init();
        title = new HTML(watch.getTranslation("actionbar.title"));
        title.setStyleName(watch.getStyleName("actionbar", "title"));
        panel.add(title);
        pressReviewPanel = new ImageTextButton(watch.getTranslation("actionbar.pressreview"), watch.getSkinFile(Constants.IMAGE_PRESS_REVIEW));
        pressReviewPanel.addStyleName(watch.getStyleName("actionbar", "pressreview"));
        analysisPanel = new ImageTextButton(watch.getTranslation("actionbar.analysis"), watch.getSkinFile(Constants.IMAGE_ANALYSIS));
        analysisPanel.addStyleName(watch.getStyleName("actionbar", "analysis"));
        pressReviewPanel.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.openPressReviewWizard();
            }
        });
        panel.add(pressReviewPanel);
        analysisPanel.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.openAnalysisWizard();
            }
        });
        panel.add(analysisPanel);
        /*
        TODO: this should allow to mark all articles read.. currently this is not the case
        hideReadArticlesPanel = new ImageTextButton(watch.getTranslation("actionbar.hidereadarticles"), watch.getSkinFile(Constants.IMAGE_SHOW_READ));
        hideReadArticlesPanel.addStyleName(watch.getStyleName("actionbar", "showhidereadarticles"));
        showReadArticlesPanel = new ImageTextButton(watch.getTranslation("actionbar.showreadarticles"), watch.getSkinFile(Constants.IMAGE_HIDE_READ));
        showReadArticlesPanel.addStyleName(watch.getStyleName("actionbar", "showhidereadarticles"));

        hideReadArticlesPanel.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.refreshOnHideReadArticles();
                setShowReadArticlesButton();
            }
        });
        panel.add(hideReadArticlesPanel);
        showReadArticlesPanel.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.refreshOnShowReadArticles();
                setHideReadArticlesButton();
            }
        });
        */
    }

    private void setShowReadArticlesButton() {
        panel.remove(hideReadArticlesPanel);
        panel.add(showReadArticlesPanel);
    }

    private void setHideReadArticlesButton() {
        panel.remove(showReadArticlesPanel);
        panel.add(hideReadArticlesPanel);
    }
}
