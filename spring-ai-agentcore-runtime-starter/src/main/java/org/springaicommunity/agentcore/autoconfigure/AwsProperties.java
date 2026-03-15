package org.springaicommunity.agentcore.autoconfigure;

import jakarta.annotation.Nullable;
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
@ConfigurationProperties(AwsProperties.CONFIG_PREFIX)
public class AwsProperties {

	public static final String CONFIG_PREFIX = "spring.agent-core.credentials";

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
	@Nullable
	private DefaultsMode defaultsMode;

	/**
	 * Whether to use FIPS endpoints.
	 */
	@Nullable
	private Boolean fipsEnabled;

	/**
	 * Whether to use dualstack endpoints.
	 */
	@Nullable
	private Boolean dualstackEnabled;

	/**
	 * Custom endpoint URI to override the default AWS service endpoint.
	 */
	@Nullable
	private URI endpoint;

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

	@Nullable
	public DefaultsMode getDefaultsMode() {
		return this.defaultsMode;
	}

	public void setDefaultsMode(@Nullable DefaultsMode defaultsMode) {
		this.defaultsMode = defaultsMode;
	}

	@Nullable
	public Boolean getFipsEnabled() {
		return this.fipsEnabled;
	}

	public void setFipsEnabled(@Nullable Boolean fipsEnabled) {
		this.fipsEnabled = fipsEnabled;
	}

	@Nullable
	public Boolean getDualstackEnabled() {
		return this.dualstackEnabled;
	}

	public void setDualstackEnabled(@Nullable Boolean dualstackEnabled) {
		this.dualstackEnabled = dualstackEnabled;
	}

	@Nullable
	public URI getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(@Nullable URI endpoint) {
		this.endpoint = endpoint;
	}

}
