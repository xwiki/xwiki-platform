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
public class ActivateKeywordClickListener implements ClickListener {
    private String keyword;
    private RSSReader reader;

    public ActivateKeywordClickListener(RSSReader reader, String keyword) {
        this.keyword = keyword;
        this.reader = reader;
    }

    public void onClick(Widget sender) {
        reader.onActivateKeyword(keyword);
    }
}
