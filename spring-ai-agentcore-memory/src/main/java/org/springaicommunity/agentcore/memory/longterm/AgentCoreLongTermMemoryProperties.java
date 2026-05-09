/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agentcore.memory.longterm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(AgentCoreLongTermMemoryProperties.CONFIG_PREFIX)
public class AgentCoreLongTermMemoryProperties {

	public static final String CONFIG_PREFIX = "agentcore.memory.long-term";

	private final boolean autoDiscovery;

	private final Namespace namespace;

	private final Episodic episodic;

	private final Semantic semantic;

	private final Summary summary;

	private final UserPreference userPreference;

	public AgentCoreLongTermMemoryProperties(boolean autoDiscovery, Namespace namespace, Episodic episodic,
			Semantic semantic, Summary summary, UserPreference userPreference) {
		this.autoDiscovery = autoDiscovery;
		this.namespace = namespace != null ? namespace : new Namespace(false);
		this.episodic = episodic;
		this.semantic = semantic;
		this.summary = summary;
		this.userPreference = userPreference;
	}

	public boolean autoDiscovery() {
		return autoDiscovery;
	}

	public Namespace namespace() {
		return namespace;
	}

	public Episodic episodic() {
		return episodic;
	}

	public Semantic semantic() {
		return semantic;
	}

	public Summary summary() {
		return summary;
	}

	public UserPreference userPreference() {
		return userPreference;
	}

	/**
	 * Returns the typed per-strategy config record that corresponds to the given memory
	 * strategy kind, or {@code null} if no config applies.
	 * {@link AgentCoreLongTermMemoryStrategyType#CUSTOM} has no matching config record by
	 * design — user-defined handlers provide their own configuration.
	 */
	public AgentCoreLongTermMemoryStrategy byKind(AgentCoreLongTermMemoryStrategyType kind) {
		return switch (kind) {
			case SEMANTIC -> semantic;
			case USER_PREFERENCE -> userPreference;
			case SUMMARY -> summary;
			case EPISODIC -> episodic;
			case CUSTOM -> null;
		};
	}

	public record Episodic(String strategyId, String reflectionsStrategyId, int episodesTopK, int reflectionsTopK,
			AgentCoreLongTermMemoryNamespace namespace, String namespacePattern,
			AgentCoreLongTermMemoryNamespace reflectionsNamespace,
			String reflectionsNamespacePattern) implements AgentCoreLongTermMemoryStrategy {

		public static final String CONFIG_PREFIX = AgentCoreLongTermMemoryProperties.CONFIG_PREFIX + ".episodic";

		private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Episodic.class);

		public Episodic {
			episodesTopK = episodesTopK > 0 ? episodesTopK : 3;
			reflectionsTopK = reflectionsTopK > 0 ? reflectionsTopK : 2;
			namespace = namespace != null ? namespace : AgentCoreLongTermMemoryNamespace.ACTOR;
			if (reflectionsStrategyId != null && !reflectionsStrategyId.isEmpty()) {
				boolean hasNamespaceOverride = (reflectionsNamespacePattern != null
						&& !reflectionsNamespacePattern.isEmpty()) || reflectionsNamespace != null;
				logger.warn("'reflections-strategy-id' is deprecated and will be removed in a future release. "
						+ "In AWS AgentCore Memory, reflections are a namespace under the same episodic strategy, "
						+ "not a separate strategy. Migrate to 'reflections-namespace-pattern' or "
						+ "'reflections-namespace'. See: "
						+ "https://docs.aws.amazon.com/bedrock-agentcore/latest/devguide/episodic-memory-strategy.html"
						+ (hasNamespaceOverride ? " (Note: a reflections namespace is also set and takes precedence.)"
								: ""));
			}
		}

		public String resolveNamespacePattern() {
			return (namespacePattern != null && !namespacePattern.isEmpty()) ? namespacePattern
					: namespace.getPattern();
		}

		/**
		 * @deprecated Reflections in AWS AgentCore Memory are a namespace of the same
		 * episodic strategy, not a separate strategy. Use {@link #reflectionsNamespace()}
		 * or {@link #reflectionsNamespacePattern()} instead. Kept for one release for
		 * backward compatibility; will be removed.
		 */
		@Deprecated(forRemoval = true)
		@Override
		public String reflectionsStrategyId() {
			return reflectionsStrategyId;
		}

		/**
		 * Resolves the reflections namespace pattern using precedence:
		 * {@code reflectionsNamespacePattern} &gt; {@code reflectionsNamespace} &gt;
		 * {@code null} (no reflections).
		 * @return the reflections namespace pattern, or {@code null} if reflections are
		 * disabled via the modern config
		 */
		public String resolveReflectionsNamespacePattern() {
			if (reflectionsNamespacePattern != null && !reflectionsNamespacePattern.isEmpty()) {
				return reflectionsNamespacePattern;
			}
			if (reflectionsNamespace != null) {
				return reflectionsNamespace.getPattern();
			}
			return null;
		}

		/**
		 * Returns true if reflections are configured via the deprecated separate-strategy
		 * path and no modern configuration overrides it. Advisor + auto-config branch on
		 * this to keep legacy behaviour alive while warning.
		 */
		public boolean usesLegacyReflectionsStrategy() {
			return resolveReflectionsNamespacePattern() == null && reflectionsStrategyId != null
					&& !reflectionsStrategyId.isEmpty();
		}

	}

	public record Semantic(String strategyId, int topK, AgentCoreLongTermMemoryNamespace namespace,
			String namespacePattern) implements AgentCoreLongTermMemoryStrategy {

		public static final String CONFIG_PREFIX = AgentCoreLongTermMemoryProperties.CONFIG_PREFIX + ".semantic";

		public Semantic {
			topK = topK > 0 ? topK : 3;
			namespace = namespace != null ? namespace : AgentCoreLongTermMemoryNamespace.ACTOR;
		}

		public String resolveNamespacePattern() {
			return (namespacePattern != null && !namespacePattern.isEmpty()) ? namespacePattern
					: namespace.getPattern();
		}

	}

	public record Summary(String strategyId, int topK, AgentCoreLongTermMemoryNamespace namespace,
			String namespacePattern) implements AgentCoreLongTermMemoryStrategy {

		public static final String CONFIG_PREFIX = AgentCoreLongTermMemoryProperties.CONFIG_PREFIX + ".summary";

		public Summary {
			topK = topK > 0 ? topK : 3;
			namespace = namespace != null ? namespace : AgentCoreLongTermMemoryNamespace.SESSION;
		}

		public String resolveNamespacePattern() {
			return (namespacePattern != null && !namespacePattern.isEmpty()) ? namespacePattern
					: namespace.getPattern();
		}

	}

	public record UserPreference(String strategyId, AgentCoreLongTermMemoryNamespace namespace,
			String namespacePattern) implements AgentCoreLongTermMemoryStrategy {

		public static final String CONFIG_PREFIX = AgentCoreLongTermMemoryProperties.CONFIG_PREFIX + ".user-preference";

		public UserPreference {
			namespace = namespace != null ? namespace : AgentCoreLongTermMemoryNamespace.ACTOR;
		}

		public String resolveNamespacePattern() {
			return (namespacePattern != null && !namespacePattern.isEmpty()) ? namespacePattern
					: namespace.getPattern();
		}

	}

	public record Namespace(boolean autoRegister) {
	}

}
