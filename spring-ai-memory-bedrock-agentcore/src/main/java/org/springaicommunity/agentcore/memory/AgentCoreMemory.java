package org.springaicommunity.agentcore.memory;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.ArrayList;
import java.util.List;

public class AgentCoreMemory {

	public final MessageChatMemoryAdvisor shortMemoryAdvisor;

	public final List<AgentCoreLongMemoryAdvisor> longMemoryAdvisors;

	public final List<Advisor> advisors;

	AgentCoreMemory(MessageChatMemoryAdvisor stmAdvisor, List<AgentCoreLongMemoryAdvisor> ltmAdvisors) {
		this.shortMemoryAdvisor = stmAdvisor;
		this.longMemoryAdvisors = ltmAdvisors;

		this.advisors = new ArrayList<>(ltmAdvisors);
		this.advisors.add(stmAdvisor);
	}

}
