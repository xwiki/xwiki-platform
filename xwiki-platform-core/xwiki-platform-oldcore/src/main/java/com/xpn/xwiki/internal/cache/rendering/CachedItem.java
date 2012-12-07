package com.xpn.xwiki.internal.cache.rendering;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CachedItem {

	public static class UsedExtension{
		public UsedExtension(Set<String> resource, Map<String,Map<String, Object>> parameters) {
			this.resource = resource;
			this.parameters = parameters;
		}
		public UsedExtension() {
		}
		public Set<String> resource;
		public Map<String, Map<String, Object>> parameters;
	}
	
	public String rendered;
	public Map<String,UsedExtension> extensions = new HashMap<String, CachedItem.UsedExtension>();
}
