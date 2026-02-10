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

package com.unicorn.browser;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agentcore.browser.BrowserClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BrowserExampleApplication {

	private static final Logger logger = LoggerFactory.getLogger(BrowserExampleApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BrowserExampleApplication.class, args);
	}

	@Bean
	CommandLineRunner run(BrowserClient browserClient, @Value("${app.url}") String url,
			@Value("${app.output-dir}") String outputDir) {
		return args -> {
			// Each call creates an independent browser session (separate navigation)
			logger.info("Browsing: {}", url);

			String content = browserClient.browseAndExtract(url);
			logger.info("Page content:\n{}", content);

			byte[] screenshot = browserClient.screenshotBytes(url);
			Path dir = Path.of(outputDir);
			Files.createDirectories(dir);
			Path file = dir.resolve("screenshot.png");
			Files.write(file, screenshot);
			logger.info("Screenshot saved: {} ({} bytes)", file.toAbsolutePath(), screenshot.length);
		};
	}

}
