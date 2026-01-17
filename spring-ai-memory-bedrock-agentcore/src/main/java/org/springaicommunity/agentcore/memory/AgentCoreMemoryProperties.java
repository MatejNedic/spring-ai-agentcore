package org.springaicommunity.agentcore.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(AgentCoreMemoryProperties.CONFIG_PREFIX)
public record AgentCoreMemoryProperties(String memoryId, Integer totalEventsLimit, String defaultSession, int pageSize,
		boolean ignoreUnknownRoles) {

	public static final String CONFIG_PREFIX = "agentcore.memory";

	public AgentCoreMemoryProperties(String memoryId, Integer totalEventsLimit, String defaultSession, int pageSize,
			boolean ignoreUnknownRoles) {
		this.memoryId = memoryId;
		this.totalEventsLimit = totalEventsLimit;
		this.defaultSession = defaultSession != null ? defaultSession
				: AgentCoreMemoryConversationIdParser.DEFAULT_SESSION;
		this.pageSize = pageSize > 0 ? pageSize : 100;
		this.ignoreUnknownRoles = ignoreUnknownRoles;
	}

}
