package com.xpn.xwiki.watch.client.ui.menu;

import com.google.gwt.user.client.ui.*;
import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.ui.WatchWidget;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

public class TagCloudWidget extends WatchWidget {
    private Map tagsLink = new HashMap();

    public TagCloudWidget() {
        super();
    }

    public String getName() {
        return "tagcloud";
    }
    
    public TagCloudWidget(Watch watch) {
        super(watch);
        panel = new FlowPanel();
        panel.add(getTitlePanel());
        initWidget(panel);
        init();
    }

    private Widget getTitlePanel() {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("filter", "tagcloud-title"));
        HTML titleHTML = new HTML(watch.getTranslation("filter.tagcloud.title"));
        titleHTML.setStyleName(watch.getStyleName("filter", "title-tagcloud-text"));
        p.add(titleHTML);
        return p;
    }


    public void refreshData() {
        // Load the tags list
        watch.getDataManager().getTagsList(new XWikiAsyncCallback(watch) {
            public void onSuccess(Object result) {
                super.onSuccess(result);
                updateTagsList((List) result);
            }
        });
    }

    public void resetSelections() {
        setActiveTags((Object[]) watch.getFilterStatus().getTags().toArray());
    }

    public void updateTagsList(List list) {
        panel.clear();
        panel.add(getTitlePanel());
        tagsLink.clear();
        if (list!=null) {
            for (int i=0;i<list.size();i++) {
                List result = (List) list.get(i);
                final String name = (String) result.get(0);
                int count = ((Integer)result.get(1)).intValue();
                Hyperlink link = new Hyperlink(name, "");
                int pixels = 9 + count;
                if (pixels>15)
                 pixels = 15;
                link.setStyleName(watch.getStyleName("tagscloud", "link"));
                link.setStyleName(watch.getStyleName("tagscloud", "" + pixels));
                tagsLink.put(name, link);
                link.addClickListener(new ClickListener() {
                    public void onClick(Widget widget) {
                            watch.refreshOnTagActivated(name);
                        }
                    });
                panel.add(link);
            }
        }
    }

    public void setActiveTags(Object[] tags) {
        Iterator it = tagsLink.values().iterator();
        while (it.hasNext()) {
            ((Hyperlink)it.next()).removeStyleName(watch.getStyleName("tagscloud", "active"));
        }
        for (int i=0;i<tags.length;i++) {
            Hyperlink link = (Hyperlink) tagsLink.get(tags[i]);
            if (link!=null)
             link.addStyleName(watch.getStyleName("tagscloud", "active"));
        }
    }

}
