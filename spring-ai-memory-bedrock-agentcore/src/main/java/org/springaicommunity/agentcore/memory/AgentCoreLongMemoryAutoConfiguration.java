package org.springaicommunity.agentcore.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springaicommunity.agentcore.memory.AgentCoreLongMemoryAdvisor.Mode;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcorecontrol.BedrockAgentCoreControlClient;

/**
 * Auto-configuration for AgentCore Long-Term Memory.
 *
 * @author Yuriy Bezsonov
 */
@AutoConfiguration(after = AgentCoreShortMemoryRepositoryAutoConfiguration.class)
@EnableConfigurationProperties(AgentCoreLongMemoryConfiguration.class)
@ConditionalOnBean({ BedrockAgentCoreClient.class, AgentCoreShortMemoryRepositoryConfiguration.class })
public class AgentCoreLongMemoryAutoConfiguration {

	/**
	 * Factory for creating ControlClient instances. Allows tests to provide a mock
	 * factory while production uses the default SDK client creation.
	 */
	@Bean
	@ConditionalOnMissingBean
	Supplier<BedrockAgentCoreControlClient> controlClientFactory() {
		return BedrockAgentCoreControlClient::create;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "agentcore.memory.long-term.enabled", havingValue = "true")
	AgentCoreLongMemoryRetriever agentCoreLongMemoryRetriever(BedrockAgentCoreClient client,
			AgentCoreShortMemoryRepositoryConfiguration shortMemoryConfig, AgentCoreLongMemoryConfiguration config,
			Supplier<BedrockAgentCoreControlClient> controlClientFactory) {
		String memoryId = shortMemoryConfig.memoryId();

		// Validate namespaces at startup to fail fast on misconfiguration
		// ControlClient is only needed for validation, so create/use/close inline
		Map<String, AgentCoreLongMemoryScope> strategyConfigs = buildStrategyConfigs(config);
		if (!strategyConfigs.isEmpty()) {
			try (BedrockAgentCoreControlClient controlClient = controlClientFactory.get()) {
				AgentCoreLongMemoryNamespaceValidator validator = new AgentCoreLongMemoryNamespaceValidator(
						controlClient);
				validator.validateNamespaces(memoryId, strategyConfigs);
			}
		}

		return new AgentCoreLongMemoryRetriever(client, memoryId);
	}

	private Map<String, AgentCoreLongMemoryScope> buildStrategyConfigs(AgentCoreLongMemoryConfiguration config) {
		Map<String, AgentCoreLongMemoryScope> configs = new HashMap<>();
		if (config.semantic() != null && config.semantic().isEnabled()) {
			configs.put(config.semantic().strategyId(), config.semantic().scope());
		}
		if (config.userPreference() != null && config.userPreference().isEnabled()) {
			configs.put(config.userPreference().strategyId(), config.userPreference().scope());
		}
		if (config.summary() != null && config.summary().isEnabled()) {
			configs.put(config.summary().strategyId(), config.summary().scope());
		}
		if (config.episodic() != null && config.episodic().isEnabled()) {
			configs.put(config.episodic().strategyId(), config.episodic().scope());
			if (config.episodic().hasReflections()) {
				configs.put(config.episodic().reflectionsStrategyId(), config.episodic().scope());
			}
		}
		return configs;
	}

	@Bean
	@ConditionalOnProperty(name = "agentcore.memory.long-term.semantic.strategy-id")
	AgentCoreLongMemoryAdvisor semanticAdvisor(AgentCoreLongMemoryRetriever retriever,
			AgentCoreLongMemoryConfiguration config) {
		AgentCoreLongMemorySemanticConfig semanticConfig = config.semantic();
		return AgentCoreLongMemoryAdvisor.builder(retriever, Mode.SEMANTIC)
			.strategyId(semanticConfig.strategyId())
			.contextLabel("Known facts about the user (use naturally in conversation)")
			.topK(semanticConfig.topK())
			.scope(semanticConfig.scope())
			.build();
	}

	@Bean
	@ConditionalOnProperty(name = "agentcore.memory.long-term.user-preference.strategy-id")
	AgentCoreLongMemoryAdvisor userPreferenceAdvisor(AgentCoreLongMemoryRetriever retriever,
			AgentCoreLongMemoryConfiguration config) {
		AgentCoreLongMemoryUserPreferenceConfig prefConfig = config.userPreference();
		return AgentCoreLongMemoryAdvisor.builder(retriever, Mode.USER_PREFERENCE)
			.strategyId(prefConfig.strategyId())
			.contextLabel("User preferences (apply when relevant)")
			.scope(prefConfig.scope())
			.build();
	}

	@Bean
	@ConditionalOnProperty(name = "agentcore.memory.long-term.summary.strategy-id")
	AgentCoreLongMemoryAdvisor summaryAdvisor(AgentCoreLongMemoryRetriever retriever,
			AgentCoreLongMemoryConfiguration config) {
		AgentCoreLongMemorySummaryConfig summaryConfig = config.summary();
		return AgentCoreLongMemoryAdvisor.builder(retriever, Mode.SUMMARY)
			.strategyId(summaryConfig.strategyId())
			.contextLabel("Previous conversation summaries (use for continuity)")
			.topK(summaryConfig.topK())
			.scope(summaryConfig.scope())
			.build();
	}

	@Bean
	@ConditionalOnProperty(name = "agentcore.memory.long-term.episodic.strategy-id")
	AgentCoreLongMemoryAdvisor episodicAdvisor(AgentCoreLongMemoryRetriever retriever,
			AgentCoreLongMemoryConfiguration config) {
		AgentCoreLongMemoryEpisodicConfig episodicConfig = config.episodic();
		return AgentCoreLongMemoryAdvisor.builder(retriever, Mode.EPISODIC)
			.strategyId(episodicConfig.strategyId())
			.reflectionsStrategyId(episodicConfig.reflectionsStrategyId())
			.contextLabel("Past interactions and reflections (reference when relevant)")
			.topK(episodicConfig.episodesTopK())
			.reflectionsTopK(episodicConfig.reflectionsTopK())
			.scope(episodicConfig.scope())
			.build();
	}

}
