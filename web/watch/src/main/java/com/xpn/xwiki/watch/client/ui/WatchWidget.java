package com.xpn.xwiki.watch.client.ui;

import com.xpn.xwiki.watch.client.Watch;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.FlowPanel;

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

public class WatchWidget extends Composite {
    protected Watch watch;
    protected Panel panel = new FlowPanel();

    public WatchWidget() {
        super();
    }

    public WatchWidget(Watch watch) {
        this();
        this.watch = watch;
    }

    public String getName() {
        return "watch";
    }

    public Panel getPanel() {
        return panel;
    }

    public void setPanel(Panel panel) {
        this.panel = panel;
    }

    public void init() {
        Panel p = getPanel();
        if (p!=null)
         getPanel().setStyleName(watch.getStyleName(getName()));
        watch.getUserInterface().removeWidget(getName());
        watch.getUserInterface().addWidget(this);
    }

    public void refreshData() {
    }

    public void resetSelections() {
    }

    public void refreshData(String widgetName) {
        if (widgetName.equals(getName()))
         refreshData();
    }

    public void resetSelections(String widgetName) {
        if (widgetName.equals(getName()))
         resetSelections();
    }

    public void resizeWindow() {        
    }
}
