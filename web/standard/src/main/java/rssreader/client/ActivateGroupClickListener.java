package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 14 janv. 2007
 * Time: 17:08:36
 * To change this template use File | Settings | File Templates.
 */
public class ActivateGroupClickListener implements ClickListener {
    private String groupname;
    private RSSReader reader;

    public ActivateGroupClickListener(RSSReader reader, String groupname) {
        this.groupname = groupname;
        this.reader = reader;
    }

    public void onClick(Widget sender) {
        reader.onActivateGroup(groupname);
    }
}
