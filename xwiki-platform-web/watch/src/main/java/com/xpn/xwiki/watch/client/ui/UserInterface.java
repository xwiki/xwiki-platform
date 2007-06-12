package com.xpn.xwiki.watch.client.ui;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.xpn.xwiki.watch.client.ui.menu.*;
import com.xpn.xwiki.watch.client.ui.articles.ArticleListWidget;
import com.xpn.xwiki.watch.client.Watch;

import java.util.*;


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

public class UserInterface extends Composite implements WindowResizeListener {
    private Watch watch;

    private FlowPanel panel = new FlowPanel();

    private Map widgetMap = new HashMap();

    public UserInterface() {
        super();
    };

    public UserInterface(Watch watch) {
        this();
        this.watch = watch;
        panel.addStyleName(watch.getStyleName("main"));
        initWidget(panel);
        Window.addWindowResizeListener(this);
   }

    public void init() {
        panel.add(new TitleBarWidget(watch));
        panel.add(new FeedTreeWidget(watch));
        panel.add(new ArticleListWidget(watch));
        panel.add(new FilterBarWidget(watch));
    }

    public void removeWidget(String name) {
        widgetMap.remove(name);
    }

    public void addWidget(WatchWidget widget) {
        widgetMap.put(widget.getName(), widget);
    }

    public void refreshData() {
        Iterator it = widgetMap.values().iterator();
        while (it.hasNext()) {
            ((WatchWidget) it.next()).refreshData();
        }
    }

    public void resetSelections() {
        Iterator it = widgetMap.values().iterator();
        while (it.hasNext()) {
            ((WatchWidget) it.next()).resetSelections();
        }
    }

    public void refreshData(String widgetName) {
        Iterator it = widgetMap.values().iterator();
        while (it.hasNext()) {
            ((WatchWidget) it.next()).refreshData(widgetName);
        }
    }

    public void resetSelections(String widgetName) {
        Iterator it = widgetMap.values().iterator();
        while (it.hasNext()) {
            ((WatchWidget) it.next()).resetSelections(widgetName);
        }
    }

    public int getFeedTreeWidth() {
        Widget w = (Widget)widgetMap.get("feedtree");
        return (w==null) ? 0 : w.getOffsetWidth();
    }

    public int getFilterBarWidth() {
        Widget w = (Widget)widgetMap.get("filterbar");
        return (w==null) ? 0 : w.getOffsetWidth();
    }

    public void resizeWindow() {
        Iterator it = widgetMap.values().iterator();
        while (it.hasNext()) {
            ((WatchWidget) it.next()).resizeWindow();
        }
    }

    public void onWindowResized(int i, int i1) {
        resizeWindow();
    }
}
