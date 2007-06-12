package com.xpn.xwiki.watch.client.ui.menu;

import com.xpn.xwiki.watch.client.ui.WatchWidget;
import com.xpn.xwiki.watch.client.Watch;
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

public class NavigationBarWidget extends WatchWidget {
    private Hyperlink previous;
    private Hyperlink next;
    private HTML nbarticles;

    public NavigationBarWidget() {
        super();
    }

    public String getName() {
        return "navbar";
    }

    public NavigationBarWidget(Watch watch) {
        super(watch);
        setPanel(new FlowPanel());
        initWidget(panel);
        init();        
    }

    public void init() {
        super.init();
        previous = new Hyperlink(watch.getTranslation("navbar.previous"), "");
        previous.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.refreshOnPrevious();
            }
        });
        previous.addStyleName(watch.getStyleName("navbar","previous"));
        nbarticles = new HTML();
        nbarticles.addStyleName(watch.getStyleName("navbar","nbarticles"));
        next = new Hyperlink(watch.getTranslation("navbar.next"), "");
        next.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.refreshOnNext();
            }
        });
        previous.addStyleName(watch.getStyleName("navbar","next"));
        panel.add(previous);
        panel.add(nbarticles);
        panel.add(next);
    }

    public void refreshData() {
        previous.removeStyleName(watch.getStyleName("navbar","previous-active"));
        next.addStyleName(watch.getStyleName("navbar","next-active"));
    }

    private void setPreviousActive(boolean active) {
        if (active)
         previous.removeStyleName(watch.getStyleName("navbar","previous-inactive"));
        else
         previous.addStyleName(watch.getStyleName("navbar","previous-active"));
    }

    private void setNextActive(boolean active) {
        if (active)
         next.removeStyleName(watch.getStyleName("navbar","next-inactive"));
        else
         next.addStyleName(watch.getStyleName("navbar","next-active"));
    }
}
