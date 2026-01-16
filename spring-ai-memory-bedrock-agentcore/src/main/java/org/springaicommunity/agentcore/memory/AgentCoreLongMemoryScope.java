package org.springaicommunity.agentcore.memory;

/**
 * Namespace scope determines the search scope for long-term memory strategies.
 *
 * <p>
 * ACTOR scope searches across all sessions for a user - richer context but slower.
 * SESSION scope searches only the current session - faster but limited context.
 *
 * @author Yuriy Bezsonov
 */
public enum AgentCoreLongMemoryScope {

	/**
	 * Actor-scoped namespace: /strategy/{memoryStrategyId}/actors/{actorId}. Searches
	 * across all sessions for the user.
	 */
	ACTOR("/strategy/{memoryStrategyId}/actors/{actorId}"),

	/**
	 * Session-scoped namespace:
	 * /strategy/{memoryStrategyId}/actors/{actorId}/sessions/{sessionId}. Searches only
	 * the current session.
	 */
	SESSION("/strategy/{memoryStrategyId}/actors/{actorId}/sessions/{sessionId}");

	private final String pattern;

	AgentCoreLongMemoryScope(String pattern) {
		this.pattern = pattern;
	}

	public String getPattern() {
		return this.pattern;
	}

	/**
	 * Builds the resolved namespace by replacing template variables with actual values.
	 * @param strategyId the memory strategy ID
	 * @param actorId the actor ID
	 * @return the resolved namespace string
	 * @throws IllegalArgumentException if strategyId or actorId is null/empty
	 */
	public String buildNamespace(String strategyId, String actorId) {
		if (strategyId == null || strategyId.isEmpty()) {
			throw new IllegalArgumentException("strategyId is required");
		}
		if (actorId == null || actorId.isEmpty()) {
			throw new IllegalArgumentException("actorId is required");
		}
		return this.pattern.replace("{memoryStrategyId}", strategyId).replace("{actorId}", actorId);
	}

	/**
	 * Builds the resolved namespace including session ID.
	 * @param strategyId the memory strategy ID
	 * @param actorId the actor ID
	 * @param sessionId the session ID (required for SESSION scope, ignored for ACTOR)
	 * @return the resolved namespace string
	 * @throws IllegalArgumentException if SESSION scope and sessionId is null/empty
	 */
	public String buildNamespace(String strategyId, String actorId, String sessionId) {
		String namespace = buildNamespace(strategyId, actorId);
		if (this == SESSION) {
			if (sessionId == null || sessionId.isEmpty()) {
				throw new IllegalArgumentException("sessionId is required for SESSION scope");
			}
			namespace = namespace.replace("{sessionId}", sessionId);
		}
		return namespace;
	}

}
