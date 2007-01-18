package rssreader.client;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 11 janv. 2007
 * Time: 11:55:22
 * To change this template use File | Settings | File Templates.
 */
public class FilterStatus {
    public int flagged;
    public int trashed;
    public int read;
    public List tags = new ArrayList();
    public String keyword;
    public String date;
    public String feed;
    public String group;

    public void reset() {
        flagged = 0;
        trashed = 0;
        read = 0;
        tags = new ArrayList();
        keyword = null;
        date = null;
        feed = null;
        group = null;
    }

    public String toString() {
        String status = "";
        if (feed!=null) {
            status = RSSReader.getConstants().Feed();
            status += " " + feed;
        }
        if (group!=null) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().Group();
            status += " " + group;
        }
        if (keyword!=null) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().Keyword();
            status += " " + keyword;
        }
        if (tags.size()>0) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().Tags();
            for (int i=0;i<tags.size();i++)
                status += " " + tags.get(i);
        }
        if (flagged ==1) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().FlagOn();
        }
        if (flagged ==-1) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().FlagOff();
        }
        if (trashed == 1) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().TrashedOn();
        }
        if (trashed ==-1) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().TrashedOff();
        }

        if (read==1) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().ReadOn();
        }
        if (read==-11) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().ReadOff();
        }
        if (date!=null) {
            if (!status.equals(""))
                status += " - ";
            status += RSSReader.getConstants().Limitfrom();
            status += date;
        }
        return status;
    }
}
