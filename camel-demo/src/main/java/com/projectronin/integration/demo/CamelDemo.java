package com.projectronin.integration.demo;

import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.util.Collections;

import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.projectronin.integration.demo.configuration.MdaOcConfiguration;
import com.projectronin.integration.demo.spring.converter.NettyChannelHandlerConverter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Setup the Spring Boot Application.
 *
 * @author Josh Smith
 */
@SpringBootApplication
@EnableConfigurationProperties({ MdaOcConfiguration.class })
@EnableRetry
public class CamelDemo {
	public static void main(final String[] args) {
		SpringApplication.run(CamelDemo.class, args);
	}

	@Bean
	public IParser fhirJsonParser() {
		return FhirContext.forR4().newJsonParser();
	}

	@Bean
	public RestTemplate restTemplate() {
		// None of this should be needed, but something has either changed or things are
		// just loading incorrectly, and the default Jackson converter is not getting
		// the appropriate modules set.
		ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().modules(new GuavaModule())
				.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
		MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(jacksonConverter);
		return restTemplate;
	}

	@Bean
	public HL7MLLPNettyDecoderFactory hl7Decoder() {
		return new HL7MLLPNettyDecoderFactory();
	}

	@Bean
	public HL7MLLPNettyEncoderFactory hl7Encoder() {
		return new HL7MLLPNettyEncoderFactory();
	}

	@Bean
	public ConversionServiceFactoryBean conversionService(
			final NettyChannelHandlerConverter nettyChannelHandlerConverter) {
		ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
		conversionServiceFactoryBean.setConverters(Collections.singleton(nettyChannelHandlerConverter));
		return conversionServiceFactoryBean;
	}

	// Create a Mock rest server to mimic MDAOC.
	@Bean
	public MockRestServiceServer mockRestServiceServer(final RestTemplate restTemplate) throws Exception {
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

		//@formatter:off
		// Mock Bearer token
		server.expect(manyTimes(), requestTo(new URI("http://localhost:8080/auth/token")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().string("{ \"Username\": \"username\", \"Password\": \"password\", \"AppName\": \"AppName\", \"AppKey\": \"AppKey\", \"scope\": \"read\" }"))
				.andRespond(withStatus(HttpStatus.OK)
								.body("AuthToken"));

		// Mock patient identifier lookup
		server.expect(manyTimes(), requestTo(new URI("http://localhost:8080/api/oc/patient/0MRN123/identifiers/type/MRN")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer AuthToken"))
				.andExpect(header("Content-Type", "application/json"))
				.andExpect(header("Accept", "application/json"))
				.andRespond(withStatus(HttpStatus.OK)
								.contentType(MediaType.APPLICATION_JSON)
								.body("{ \"historicalIdentifiers\": [], \"identifiers\": [ { \"id\": \"MRN123\", \"idType\": \"MRN\" }, { \"id\": \"fhir12345\", \"idType\": \"FHIR STU3\" } ] }"));

		// Mock encounters
		server.expect(manyTimes(), requestTo(new URI("http://localhost:8080/api/oc/api/FHIR/STU3/Encounter?patient=fhir12345&date=ge2021-07-01&date=le2021-07-08")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer AuthToken"))
				.andExpect(header("Content-Type", "application/json"))
				.andExpect(header("Accept", "application/json"))
				.andRespond(withStatus(HttpStatus.OK)
								.contentType(MediaType.APPLICATION_JSON)
								.body(encounterPage1));

		server.expect(manyTimes(), requestTo(new URI("http://localhost:8080/api/oc/api/FHIR/STU3?_getpages=6bba9175-c09b-4565-b86f-b4d9174c2695&_getpagesoffset=2&_count=2&_pretty=true&_bundletype=searchset")))
		.andExpect(method(HttpMethod.GET))
		.andExpect(header("Authorization", "Bearer AuthToken"))
		.andExpect(header("Content-Type", "application/json"))
		.andExpect(header("Accept", "application/json"))
		.andRespond(withStatus(HttpStatus.OK)
						.contentType(MediaType.APPLICATION_JSON)
						.body(encounterPage2));
		//@formatter:on

		return server;
	}

	// These responses were generated based off data in the HAPI FHIR server :) but
	// edited to have our fake URLs for the links.
	private final String encounterPage1 = "{\n" + "  \"resourceType\": \"Bundle\",\n"
			+ "  \"id\": \"6bba9175-c09b-4565-b86f-b4d9174c2695\",\n" + "  \"meta\": {\n"
			+ "    \"lastUpdated\": \"2021-07-08T22:21:36.224+00:00\"\n" + "  },\n" + "  \"type\": \"searchset\",\n"
			+ "  \"link\": [ {\n" + "    \"relation\": \"self\",\n"
			+ "    \"url\": \"http://localhost:8080/api/oc/api/FHIR/STU3/Encounter?patient=fhir12345&date=ge2021-07-01&date=le2021-07-08\"\n"
			+ "  }, {\n" + "    \"relation\": \"next\",\n"
			+ "    \"url\": \"http://localhost:8080/api/oc/api/FHIR/STU3?_getpages=6bba9175-c09b-4565-b86f-b4d9174c2695&_getpagesoffset=2&_count=2&_pretty=true&_bundletype=searchset\"\n"
			+ "  } ],\n" + "  \"entry\": [ {\n"
			+ "    \"fullUrl\": \"http://hapi.fhir.org/baseR4/Encounter/1366573\",\n" + "    \"resource\": {\n"
			+ "      \"resourceType\": \"Encounter\",\n" + "      \"id\": \"1366573\",\n" + "      \"meta\": {\n"
			+ "        \"versionId\": \"1\",\n" + "        \"lastUpdated\": \"2020-07-04T07:27:51.428+00:00\",\n"
			+ "        \"source\": \"#666y0aXuBm4PVYu7\"\n" + "      },\n" + "      \"identifier\": [ {\n"
			+ "        \"type\": {\n" + "          \"coding\": [ {\n"
			+ "            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0203\"\n" + "          } ],\n"
			+ "          \"text\": \"visit number\"\n" + "        },\n" + "        \"value\": \"VISIT101\"\n"
			+ "      } ],\n" + "      \"status\": \"unknown\",\n" + "      \"class\": {\n"
			+ "        \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\n"
			+ "        \"code\": \"IMP\",\n" + "        \"display\": \"inpatient encounter\"\n" + "      },\n"
			+ "      \"type\": [ {\n" + "        \"coding\": [ {\n"
			+ "          \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0007\",\n"
			+ "          \"code\": \"R\",\n" + "          \"display\": \"Routine\"\n" + "        } ]\n" + "      } ],\n"
			+ "      \"subject\": {\n" + "        \"reference\": \"Patient/1366572\"\n" + "      },\n"
			+ "      \"period\": {\n" + "        \"start\": \"2000-08-21T11:56:23.000Z\"\n" + "      },\n"
			+ "      \"reasonCode\": [ {\n" + "        \"coding\": [ {\n" + "          \"code\": \"Anxiety Disorder\"\n"
			+ "        } ]\n" + "      } ],\n" + "      \"location\": [ {\n" + "        \"status\": \"active\"\n"
			+ "      } ]\n" + "    },\n" + "    \"search\": {\n" + "      \"mode\": \"match\"\n" + "    }\n"
			+ "  }, {\n" + "    \"fullUrl\": \"http://hapi.fhir.org/baseR4/Encounter/1366488\",\n"
			+ "    \"resource\": {\n" + "      \"resourceType\": \"Encounter\",\n" + "      \"id\": \"1366488\",\n"
			+ "      \"meta\": {\n" + "        \"versionId\": \"1\",\n"
			+ "        \"lastUpdated\": \"2020-07-04T07:10:50.343+00:00\",\n"
			+ "        \"source\": \"#OTwUCgIpLDe1xGf5\"\n" + "      },\n" + "      \"identifier\": [ {\n"
			+ "        \"type\": {\n" + "          \"coding\": [ {\n"
			+ "            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0203\"\n" + "          } ],\n"
			+ "          \"text\": \"visit number\"\n" + "        },\n" + "        \"value\": \"VISIT101\"\n"
			+ "      } ],\n" + "      \"status\": \"unknown\",\n" + "      \"class\": {\n"
			+ "        \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\n"
			+ "        \"code\": \"IMP\",\n" + "        \"display\": \"inpatient encounter\"\n" + "      },\n"
			+ "      \"type\": [ {\n" + "        \"coding\": [ {\n"
			+ "          \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0007\",\n"
			+ "          \"code\": \"R\",\n" + "          \"display\": \"Routine\"\n" + "        } ]\n" + "      } ],\n"
			+ "      \"subject\": {\n" + "        \"reference\": \"Patient/1366487\"\n" + "      },\n"
			+ "      \"period\": {\n" + "        \"start\": \"2000-08-21T11:56:23.000Z\"\n" + "      },\n"
			+ "      \"location\": [ {\n" + "        \"status\": \"active\"\n" + "      } ]\n" + "    },\n"
			+ "    \"search\": {\n" + "      \"mode\": \"match\"\n" + "    }\n" + "  } ]\n" + "}";

	private final String encounterPage2 = "{\n" + "  \"resourceType\": \"Bundle\",\n"
			+ "  \"id\": \"6bba9175-c09b-4565-b86f-b4d9174c2695\",\n" + "  \"meta\": {\n"
			+ "    \"lastUpdated\": \"2021-07-08T22:21:36.224+00:00\"\n" + "  },\n" + "  \"type\": \"searchset\",\n"
			+ "  \"link\": [ {\n" + "    \"relation\": \"self\",\n"
			+ "    \"url\": \"http://localhost:8080/api/oc/api/FHIR/STU3?_getpages=6bba9175-c09b-4565-b86f-b4d9174c2695&_getpagesoffset=2&_count=2&_pretty=true&_bundletype=searchset\"\n"
			+ "  }, {\n" + "    \"relation\": \"previous\",\n"
			+ "    \"url\": \"http://localhost:8080/api/oc/api/FHIR/STU3?_getpages=6bba9175-c09b-4565-b86f-b4d9174c2695&_getpagesoffset=0&_count=2&_pretty=true&_bundletype=searchset\"\n"
			+ "  } ],\n" + "  \"entry\": [ {\n"
			+ "    \"fullUrl\": \"http://hapi.fhir.org/baseR4/Encounter/1366483\",\n" + "    \"resource\": {\n"
			+ "      \"resourceType\": \"Encounter\",\n" + "      \"id\": \"1366483\",\n" + "      \"meta\": {\n"
			+ "        \"versionId\": \"1\",\n" + "        \"lastUpdated\": \"2020-07-04T07:08:21.234+00:00\",\n"
			+ "        \"source\": \"#ObELNRdCRUTqwM3h\"\n" + "      },\n" + "      \"identifier\": [ {\n"
			+ "        \"type\": {\n" + "          \"coding\": [ {\n"
			+ "            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0203\"\n" + "          } ],\n"
			+ "          \"text\": \"visit number\"\n" + "        },\n" + "        \"value\": \"VISIT101\"\n"
			+ "      } ],\n" + "      \"status\": \"unknown\",\n" + "      \"class\": {\n"
			+ "        \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\n"
			+ "        \"code\": \"IMP\",\n" + "        \"display\": \"inpatient encounter\"\n" + "      },\n"
			+ "      \"type\": [ {\n" + "        \"coding\": [ {\n"
			+ "          \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0007\",\n"
			+ "          \"code\": \"R\",\n" + "          \"display\": \"Routine\"\n" + "        } ]\n" + "      } ],\n"
			+ "      \"subject\": {\n" + "        \"reference\": \"Patient/1366482\"\n" + "      },\n"
			+ "      \"period\": {\n" + "        \"start\": \"2000-08-21T11:56:23.000Z\"\n" + "      },\n"
			+ "      \"reasonCode\": [ {\n" + "        \"coding\": [ {\n" + "          \"code\": \"Anxiety Disorder\"\n"
			+ "        } ]\n" + "      } ],\n" + "      \"location\": [ {\n" + "        \"status\": \"active\"\n"
			+ "      } ]\n" + "    },\n" + "    \"search\": {\n" + "      \"mode\": \"match\"\n" + "    }\n" + "  } ]\n"
			+ "}";
}
