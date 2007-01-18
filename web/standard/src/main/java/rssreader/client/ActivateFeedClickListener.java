package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 2 déc. 2006
 * Time: 16:24:44
 * To change this template use File | Settings | File Templates.
 */
public class ActivateFeedClickListener implements ClickListener {
    private String name;
    private String url;
    private RSSReader reader;

    public ActivateFeedClickListener(RSSReader reader, String name, String url) {
        this.name = name;
        this.url = url;
        this.reader = reader;
    }

    public void onClick(Widget sender) {
        reader.onActivateFeed(name, url);
    }
}
