package com.projectronin.integration.demo.mdaoc;

import static java.util.Arrays.asList;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.projectronin.integration.demo.configuration.MdaOcConfiguration;

@Component
public class MdaOcClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(MdaOcClient.class);
	private final RestTemplate restTemplate;
	private final MdaOcConfiguration mdaOcConfiguration;

	private String apiEndpoint;
	private HttpHeaders mdaOcHeaders;

	@Autowired
	public MdaOcClient(final RestTemplate restTemplate, final MdaOcConfiguration mdaOcConfiguration) {
		this.restTemplate = restTemplate;
		this.mdaOcConfiguration = mdaOcConfiguration;

		this.apiEndpoint = mdaOcConfiguration.getApiEndpoint();
		if (apiEndpoint.endsWith("/")) {
			apiEndpoint = apiEndpoint.substring(0, apiEndpoint.length() - 1);
		}
	}

	public <T> ResponseEntity<T> get(String path, final Class<T> outputClass)
			throws RestClientException, URISyntaxException {
		LOGGER.info("GET request to {} expecting transformation to {}", path, outputClass.getName());
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		String requestUrl;
		if (path.startsWith("http")) {
			requestUrl = path;
		} else {
			requestUrl = String.format("%s/%s", apiEndpoint, path);
		}

		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		return restTemplate.exchange(new URI(requestUrl), HttpMethod.GET, request, outputClass);
	}

	private HttpHeaders getHeaders() {
		if (mdaOcHeaders == null) {
			mdaOcHeaders = new HttpHeaders();
			mdaOcHeaders.setContentType(MediaType.APPLICATION_JSON);
			mdaOcHeaders.setAccept(asList(MediaType.APPLICATION_JSON));

			String bearerToken = getBearerToken(restTemplate, mdaOcConfiguration);
			LOGGER.info("Loaded bearer token {}", bearerToken);
			String authorizationHeader = String.format("Bearer %s", bearerToken);
			mdaOcHeaders.add("Authorization", authorizationHeader);
		}

		return mdaOcHeaders;
	}

	@Retryable(value = { ResourceAccessException.class,
			HttpServerErrorException.class }, maxAttempts = 5, backoff = @Backoff(2000))
	private String getBearerToken(final RestTemplate restTemplate, final MdaOcConfiguration mdaOcConfiguration) {
		String authParams = String.format(
				"{ \"Username\": \"%s\", \"Password\": \"%s\", \"AppName\": \"%s\", \"AppKey\": \"%s\", \"scope\": \"read\" }",
				mdaOcConfiguration.getUsername(), mdaOcConfiguration.getPassword(), mdaOcConfiguration.getAppName(),
				mdaOcConfiguration.getAppKey());

		ResponseEntity<String> entity = restTemplate.postForEntity(mdaOcConfiguration.getStsEndpoint(), authParams,
				String.class);
		if (entity.getStatusCode() != HttpStatus.OK) {
			throw new IllegalStateException(
					String.format("Unable to get bearer token. Server returned %s", entity.getStatusCodeValue()));
		}
		return entity.getBody();
	}
}
