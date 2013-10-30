package org.xwiki.wikistream.instance.internal.output;

import org.xwiki.wikistream.filter.user.GroupFilter;
import org.xwiki.wikistream.filter.user.UserFilter;
import org.xwiki.wikistream.model.filter.WikiFilter;

/**
 * Events supported by user instance output module.
 * 
 * @version $Id$
 * @since 5.3M2
 */
public interface UserInstanceOutputFilter extends UserFilter, GroupFilter, WikiFilter
{

}
