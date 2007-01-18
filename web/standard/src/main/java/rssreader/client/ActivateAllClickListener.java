package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 15 janv. 2007
 * Time: 08:52:09
 * To change this template use File | Settings | File Templates.
 */
public class ActivateAllClickListener implements ClickListener {
    private String name;
    private RSSReader reader;

    public ActivateAllClickListener(RSSReader reader) {
        this.reader = reader;
    }

    public void onClick(Widget sender) {
        reader.onActivateAllFeedsFilter();
    }
}
