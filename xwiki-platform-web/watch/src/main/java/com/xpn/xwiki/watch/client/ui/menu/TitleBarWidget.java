package com.xpn.xwiki.watch.client.ui.menu;

import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.Constants;
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

public class TitleBarWidget extends WatchWidget {
    private HTML title = new HTML();
    private Image refreshImage;
    private SearchWidget searchWidget;

    public TitleBarWidget() {
        super();
    }

    public String getName() {
        return "titlebar";
    }
    
    public TitleBarWidget(Watch watch) {
        super(watch);
        setPanel(new FlowPanel());
        initWidget(panel);
        init();
    }

    public void init() {
        super.init();
        searchWidget = new SearchWidget(watch);
        searchWidget.init();
        refreshImage = new Image(watch.getSkinFile(Constants.IMAGE_REFRESH));
        refreshImage.setStyleName(watch.getStyleName("titlebar", "refreshData"));
        refreshImage.setTitle(watch.getTranslation("refreshData"));
        refreshImage.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.refreshArticleList();
            }
        });
        title.setStyleName(watch.getStyleName("titlebar", "title"));
        panel.add(title);
        panel.add(refreshImage);
        panel.add(searchWidget);
        refreshData();
    }

    public void refreshData() {
        title.setHTML(watch.getTitleBarText());
    }
}
