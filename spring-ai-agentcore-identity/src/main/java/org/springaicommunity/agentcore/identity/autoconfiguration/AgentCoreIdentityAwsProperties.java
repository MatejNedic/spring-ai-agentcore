/*
 * Copyright 2025-2026 the original author or authors.
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
package org.springaicommunity.agentcore.identity.autoconfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration properties for CredentialsProvider.
 *
 * @author Matej Nedic
 */
@ConfigurationProperties(AgentCoreIdentityAwsProperties.CONFIG_PREFIX)
public class AgentCoreIdentityAwsProperties {

	public static final String CONFIG_PREFIX = "agentcore.identity";

	/**
	 * AWS region to use. Defaults to us-east-1.
	 */
	private String region = "us-east-1";

	/**
	 * AWS access key.
	 */
	private String accessKey;

	/**
	 * AWS secret key.
	 */
	private String secretKey;

	/**
	 * AWS session token. (optional) When provided the AwsSessionCredentials are used.
	 * Otherwise, the AwsBasicCredentials are used.
	 */
	private String sessionToken;

	/**
	 * Maximum duration of the entire API call operation.
	 */
	private Duration timeout = Duration.ofMinutes(5L);

	/**
	 * Sync HTTP client properties (Apache).
	 */
	@NestedConfigurationProperty
	private SyncClientProperties syncClient = new SyncClientProperties();

	/**
	 * AWS SDK defaults mode.
	 */
	private DefaultsMode defaultsMode;

	/**
	 * Whether to use FIPS endpoints.
	 */
	private Boolean fipsEnabled;

	/**
	 * Whether to use dualstack endpoints.
	 */
	private Boolean dualstackEnabled;

	/**
	 * Custom endpoint URI to override the default AWS service endpoint.
	 */
	private URI endpoint;

	/**
	 * Configures an instance profile credentials provider with no further configuration.
	 */
	private boolean instanceProfile = false;

	private Profile profile;

	public boolean isInstanceProfile() {
		return this.instanceProfile;
	}

	public void setInstanceProfile(boolean instanceProfile) {
		this.instanceProfile = instanceProfile;
	}

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String awsRegion) {
		this.region = awsRegion;
	}

	public String getAccessKey() {
		return this.accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return this.secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public SyncClientProperties getSyncClient() {
		return this.syncClient;
	}

	public void setSyncClient(SyncClientProperties syncClient) {
		this.syncClient = syncClient;
	}

	public String getSessionToken() {
		return this.sessionToken;
	}

	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	public DefaultsMode getDefaultsMode() {
		return this.defaultsMode;
	}

	public void setDefaultsMode(DefaultsMode defaultsMode) {
		this.defaultsMode = defaultsMode;
	}

	public Boolean getFipsEnabled() {
		return this.fipsEnabled;
	}

	public void setFipsEnabled(Boolean fipsEnabled) {
		this.fipsEnabled = fipsEnabled;
	}

	public Boolean getDualstackEnabled() {
		return this.dualstackEnabled;
	}

	public void setDualstackEnabled(Boolean dualstackEnabled) {
		this.dualstackEnabled = dualstackEnabled;
	}

	public URI getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(URI endpoint) {
		this.endpoint = endpoint;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

}