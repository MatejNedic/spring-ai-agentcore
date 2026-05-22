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

package org.springaicommunity.agentcore.integration;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that when a user supplies their own {@code @PostMapping("/invocations")}
 * controller, the
 * {@link org.springaicommunity.agentcore.service.AgentCoreInvocationRegistrar} detects
 * the existing mapping and skips registration of the {@code @AgentCoreInvocation} method,
 * allowing the user-provided controller to handle requests.
 */
@SpringBootTest(classes = EndToEndUserOverrideIntegrationTest.TestApp.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndUserOverrideIntegrationTest {

	@SpringBootApplication(scanBasePackages = "org.springaicommunity.agentcore.autoconfigure")
	static class TestApp {

		@Service
		public static class TestAgentService {

			// This SHOULD be ignored because the user-provided controller below already
			// owns POST /invocations.
			@AgentCoreInvocation
			public String shouldBeIgnored(String prompt) {
				return "agentcore: " + prompt;
			}

		}

		@RestController
		public static class UserController {

			@PostMapping("/invocations")
			public String custom(@RequestBody String body) {
				return "user-override: " + body;
			}

		}

	}

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void userControllerWinsOverAgentCoreInvocation() {
		var response = restTemplate.postForEntity("http://localhost:" + port + "/invocations", "Hello", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).startsWith("user-override:");
	}

}
