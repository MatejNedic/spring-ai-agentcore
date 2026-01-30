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

package org.springaicommunity.agentcore.codeinterpreter;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Code Interpreter tool implementation for executing code in a secure sandbox.
 * <p>
 * This class contains the tool logic. Tool registration with configurable description is
 * handled by {@link AgentCoreCodeInterpreterAutoConfiguration}.
 *
 * @author Yuriy Bezsonov
 */
public class CodeInterpreterTools {

	private static final Logger logger = LoggerFactory.getLogger(CodeInterpreterTools.class);

	private static final Set<String> SUPPORTED_LANGUAGES = Set.of("python", "javascript", "typescript");

	public static final String DEFAULT_TOOL_DESCRIPTION = """
			Execute code in a secure sandbox environment.
			Supported languages: python, javascript, typescript.
			Use for calculations, data analysis, visualizations, file processing.
			Common libraries pre-installed (numpy, pandas, matplotlib for Python).
			For charts: use plt.savefig('name.png'). Use unique names for multiple charts.
			Generated files are automatically retrieved and displayed.
			""";

	private final AgentCoreCodeInterpreterClient client;

	private final CodeInterpreterFileStore fileStore;

	public CodeInterpreterTools(AgentCoreCodeInterpreterClient client, CodeInterpreterFileStore fileStore) {
		this.client = client;
		this.fileStore = fileStore;
		logger.debug("CodeInterpreterTools initialized");
	}

	/**
	 * Execute code in a secure sandbox environment.
	 * @param language programming language (python, javascript, typescript)
	 * @param code code to execute
	 * @param toolContext context containing session ID for multi-user support
	 * @return execution result text
	 */
	public String executeCode(String language, String code, ToolContext toolContext) {

		// Input validation
		if (language == null || language.isBlank()) {
			return "Error: language parameter is required (python, javascript, or typescript)";
		}
		String normalizedLanguage = language.toLowerCase().trim();
		if (!SUPPORTED_LANGUAGES.contains(normalizedLanguage)) {
			return "Error: unsupported language '" + language + "'. Supported: python, javascript, typescript";
		}
		if (code == null || code.isBlank()) {
			return "Error: code parameter is required";
		}

		// Extract session ID from tool context (defaults to DEFAULT_SESSION_ID)
		String sessionId = CodeInterpreterFileStore.DEFAULT_SESSION_ID;
		if (toolContext != null && toolContext.getContext() != null) {
			Object sessionIdObj = toolContext.getContext().get(CodeInterpreterFileStore.SESSION_ID_KEY);
			if (sessionIdObj instanceof String s && !s.isBlank()) {
				sessionId = s;
			}
		}

		logger.debug("executeCode called: language={}, sessionId={}, code:\n{}", normalizedLanguage, sessionId, code);

		CodeExecutionResult result = this.client.executeInEphemeralSession(normalizedLanguage, code);

		logger.debug("Result: {} chars text, {} files, isError={}", result.textOutput().length(), result.files().size(),
				result.isError());

		// Store files for ChatService to append later (keyed by session ID)
		if (result.hasFiles()) {
			this.fileStore.store(sessionId, result.files());
			logger.debug("Stored {} files for session {}", result.files().size(), sessionId);
		}

		return formatTextForLlm(result);
	}

	private String formatTextForLlm(CodeExecutionResult result) {
		StringBuilder sb = new StringBuilder();

		if (result.isError()) {
			sb.append("Error executing code:\n");
		}

		if (!result.textOutput().isEmpty()) {
			sb.append(result.textOutput());
		}

		// Describe files so LLM knows they exist
		for (GeneratedFile file : result.files()) {
			if (file.isImage()) {
				sb.append("\n[Chart generated: ").append(file.name()).append("]");
			}
			else {
				sb.append("\n[File generated: ").append(file.name()).append(" (").append(file.mimeType()).append(")]");
			}
		}

		if (sb.isEmpty()) {
			return "Code executed successfully (no output)";
		}

		return sb.toString();
	}

}
