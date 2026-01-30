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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreAsyncClient;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;

/**
 * Integration test for AgentCore Code Interpreter.
 *
 * <p>
 * Tests code execution with Python, JavaScript, and TypeScript. Verifies file generation
 * (charts, CSVs) and retrieval through the client.
 *
 * <p>
 * Requires: AGENTCORE_IT=true and AWS credentials.
 *
 * @author Yuriy Bezsonov
 */
@EnabledIfEnvironmentVariable(named = "AGENTCORE_IT", matches = "true")
@SpringBootTest(classes = AgentCoreCodeInterpreterIT.TestApp.class,
		properties = { "spring.autoconfigure.exclude="
				+ "org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterAutoConfiguration" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AgentCore Code Interpreter Integration Tests")
class AgentCoreCodeInterpreterIT {

	protected static final String BOLD = "\033[1m";

	protected static final String RESET = "\033[0m";

	private static String testId;

	@Autowired
	private AgentCoreCodeInterpreterClient client;

	@BeforeAll
	static void setup() {
		testId = UUID.randomUUID().toString().substring(0, 8);
		System.out.println(BOLD + "TEST_ID=" + testId + RESET);
	}

	@Test
	@Order(1)
	@DisplayName("Should execute Python code and return text output")
	void shouldExecutePythonCode() {
		System.out.println(BOLD + "\n----- Python Execution Test -----" + RESET);

		String code = "print(2 + 2)";
		CodeExecutionResult result = client.executeInEphemeralSession("python", code);

		System.out.println(BOLD + "Output: " + RESET + result.textOutput());
		System.out.println(BOLD + "Is Error: " + RESET + result.isError());
		System.out.println(BOLD + "Files: " + RESET + result.files().size());

		assertThat(result.isError()).isFalse();
		assertThat(result.textOutput()).contains("4");

		System.out.println(BOLD + "---------------------------------" + RESET + "\n");
	}

	@Test
	@Order(2)
	@DisplayName("Should execute Python code and generate chart")
	void shouldGenerateChart() {
		System.out.println(BOLD + "\n----- Chart Generation Test -----" + RESET);

		String code = """
				import matplotlib.pyplot as plt

				months = ['Jan', 'Feb', 'Mar']
				sales = [100, 150, 200]

				plt.figure(figsize=(8, 6))
				plt.bar(months, sales, color='steelblue')
				plt.title('Q1 Sales')
				plt.xlabel('Month')
				plt.ylabel('Sales')
				plt.savefig('sales_chart.png', dpi=100, bbox_inches='tight')
				print('Chart saved')
				""";

		CodeExecutionResult result = client.executeInEphemeralSession("python", code);

		System.out.println(BOLD + "Output: " + RESET + result.textOutput());
		System.out.println(BOLD + "Is Error: " + RESET + result.isError());
		System.out.println(BOLD + "Files: " + RESET + result.files().size());

		assertThat(result.isError()).isFalse();

		if (result.hasFiles()) {
			for (GeneratedFile file : result.files()) {
				System.out.println("  - " + file.name() + " (" + file.mimeType() + ", " + file.size() + " bytes)");
				if (file.isImage()) {
					assertThat(file.mimeType()).startsWith("image/");
					assertThat(file.size()).isGreaterThan(0);
				}
			}
		}

		System.out.println(BOLD + "---------------------------------" + RESET + "\n");
	}

	@Test
	@Order(3)
	@DisplayName("Should execute JavaScript code")
	void shouldExecuteJavaScript() {
		System.out.println(BOLD + "\n----- JavaScript Execution Test -----" + RESET);

		String code = """
				function factorial(n) {
				    if (n <= 1) return 1;
				    return n * factorial(n - 1);
				}
				console.log('Factorial of 5:', factorial(5));
				""";

		CodeExecutionResult result = client.executeInEphemeralSession("javascript", code);

		System.out.println(BOLD + "Output: " + RESET + result.textOutput());
		System.out.println(BOLD + "Is Error: " + RESET + result.isError());

		assertThat(result.isError()).isFalse();
		assertThat(result.textOutput()).contains("120");

		System.out.println(BOLD + "-------------------------------------" + RESET + "\n");
	}

	@Test
	@Order(4)
	@DisplayName("Should execute TypeScript code")
	void shouldExecuteTypeScript() {
		System.out.println(BOLD + "\n----- TypeScript Execution Test -----" + RESET);

		String code = """
				interface Person {
				    name: string;
				    age: number;
				}

				const person: Person = { name: 'Alice', age: 30 };
				console.log(`Name: ${person.name}, Age: ${person.age}`);
				""";

		CodeExecutionResult result = client.executeInEphemeralSession("typescript", code);

		System.out.println(BOLD + "Output: " + RESET + result.textOutput());
		System.out.println(BOLD + "Is Error: " + RESET + result.isError());

		assertThat(result.isError()).isFalse();
		assertThat(result.textOutput()).containsAnyOf("Alice", "30");

		System.out.println(BOLD + "-------------------------------------" + RESET + "\n");
	}

	@Test
	@Order(5)
	@DisplayName("Should generate CSV file")
	void shouldGenerateCsvFile() {
		System.out.println(BOLD + "\n----- CSV Generation Test -----" + RESET);

		String code = """
				import csv

				data = [
				    ['name', 'age', 'city'],
				    ['Alice', 30, 'New York'],
				    ['Bob', 25, 'Los Angeles'],
				    ['Charlie', 35, 'Chicago']
				]

				with open('data.csv', 'w', newline='') as f:
				    writer = csv.writer(f)
				    writer.writerows(data)

				print('CSV file created')
				""";

		CodeExecutionResult result = client.executeInEphemeralSession("python", code);

		System.out.println(BOLD + "Output: " + RESET + result.textOutput());
		System.out.println(BOLD + "Is Error: " + RESET + result.isError());
		System.out.println(BOLD + "Files: " + RESET + result.files().size());

		assertThat(result.isError()).isFalse();

		if (result.hasFiles()) {
			for (GeneratedFile file : result.files()) {
				System.out.println("  - " + file.name() + " (" + file.mimeType() + ", " + file.size() + " bytes)");
			}
		}

		System.out.println(BOLD + "-------------------------------" + RESET + "\n");
	}

	@Test
	@Order(6)
	@DisplayName("Should handle code error gracefully")
	void shouldHandleCodeError() {
		System.out.println(BOLD + "\n----- Error Handling Test -----" + RESET);

		String code = "print(undefined_variable)";

		CodeExecutionResult result = client.executeInEphemeralSession("python", code);

		System.out.println(BOLD + "Output: " + RESET + result.textOutput());
		System.out.println(BOLD + "Is Error: " + RESET + result.isError());

		assertThat(result.isError()).isTrue();
		assertThat(result.textOutput()).containsAnyOf("NameError", "undefined", "error");

		System.out.println(BOLD + "-------------------------------" + RESET + "\n");
	}

	@SpringBootApplication(exclude = {
			org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterAutoConfiguration.class })
	static class TestApp {

		@Bean
		BedrockAgentCoreClient bedrockAgentCoreClient() {
			return BedrockAgentCoreClient.create();
		}

		@Bean
		BedrockAgentCoreAsyncClient bedrockAgentCoreAsyncClient() {
			return BedrockAgentCoreAsyncClient.create();
		}

		@Bean
		AgentCoreCodeInterpreterConfiguration codeInterpreterConfiguration() {
			return new AgentCoreCodeInterpreterConfiguration(null, null, null, null, null);
		}

		@Bean
		AgentCoreCodeInterpreterClient agentCoreCodeInterpreterClient(BedrockAgentCoreClient syncClient,
				BedrockAgentCoreAsyncClient asyncClient, AgentCoreCodeInterpreterConfiguration config) {
			return new AgentCoreCodeInterpreterClient(syncClient, asyncClient, config);
		}

	}

}
