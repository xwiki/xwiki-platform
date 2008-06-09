package org.xwiki.platform.patchservice.api;

/**
 * A logical time represents a kind of timestamp that can be used in a p2p environment, where a
 * simple host time isn't enough for event ordering. The actual implementation is hidden, as all we
 * need to do is compare two logical timestamps.
 * 
 * @version $Id$
 */
public interface LogicalTime extends Comparable<LogicalTime>, XmlSerializable
{
}
