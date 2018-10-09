/**
 *
 */
package org.rainbow.solar.rest.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.rainbow.solar.rest.dto.HourlyElectricityDto;
import org.rainbow.solar.rest.err.HourlyElectricityNotFoundError;
import org.rainbow.solar.rest.err.HourlyElectricityPanelMismatchError;
import org.rainbow.solar.rest.err.HourlyElectricityReadingDateRequiredError;
import org.rainbow.solar.rest.err.HourlyElectricityReadingRequiredError;
import org.rainbow.solar.rest.err.PanelNotFoundError;
import org.rainbow.solar.rest.err.SolarErrorCode;
import org.rainbow.solar.rest.util.ErrorMessagesResourceBundle;
import org.rainbow.solar.rest.util.JsonHttpEntityBuilder;
import org.rainbow.solar.rest.util.RegexUtil;
import org.rainbow.solar.service.util.ExceptionMessagesResourceBundle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * This class tests APIs in {@link PanelController}
 * 
 * @author biya-bi
 *
 */
public class HourlyElectricityControllerTests extends ControllerTests {

	@Test
	public void create_HourlyElectricityIsValid_HourlyElectricityCreated() {
		LocalDateTime now = LocalDateTime.now();

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", now.format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<?> response = template.postForEntity("/api/panels/2/hourly", hourlyElectricity, Object.class);

		Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());

		URI location = response.getHeaders().getLocation();

		Assert.assertNotNull(location);
		Assert.assertTrue(RegexUtil.endsWithDigit("/api/panels/2/hourly/", location.toString()));
	}

	@Test
	public void create_PanelDoesnotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly", panelId);

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<PanelNotFoundError> response = template.postForEntity(uri, hourlyElectricity,
				PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void create_ReadingAtIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.build();

		ResponseEntity<HourlyElectricityReadingDateRequiredError> response = template.postForEntity(
				"/api/panels/3/hourly", hourlyElectricity, HourlyElectricityReadingDateRequiredError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		HourlyElectricityReadingDateRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_DATE_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.date.required"),
				error.getMessage());
	}

	@Test
	public void create_GeneratedElectricityIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder()
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityReadingRequiredError> response = template.postForEntity("/api/panels/4/hourly",
				hourlyElectricity, HourlyElectricityReadingRequiredError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		HourlyElectricityReadingRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.required"),
				error.getMessage());
	}

	@Test
	public void update_HourlyElectricityIsValid_HourlyElectricityUpdated() {
		LocalDateTime now = LocalDateTime.now();

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "2000")
				.setProperty("readingAt", now.format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<?> response = template.exchange("/api/panels/1/hourly/1", HttpMethod.PUT, hourlyElectricity,
				PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void update_PanelDoesnotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly/1", panelId);

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.PUT, hourlyElectricity,
				PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void update_ReadingAtIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.build();

		ResponseEntity<HourlyElectricityReadingDateRequiredError> response = template.exchange("/api/panels/1/hourly/1",
				HttpMethod.PUT, hourlyElectricity, HourlyElectricityReadingDateRequiredError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		HourlyElectricityReadingDateRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_DATE_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.date.required"),
				error.getMessage());
	}

	@Test
	public void update_GeneratedElectricityIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder()
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityReadingRequiredError> response = template.exchange("/api/panels/1/hourly/1",
				HttpMethod.PUT, hourlyElectricity, HourlyElectricityReadingRequiredError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		HourlyElectricityReadingRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.required"),
				error.getMessage());
	}

	@Test
	public void update_HourlyElectriciyDoesnotExist_NotFoundErrorReturned() {
		Long hourlyElectricityId = 5000L;
		String uri = String.format("/api/panels/1/hourly/%s", hourlyElectricityId);
		LocalDateTime now = LocalDateTime.now();

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "2500")
				.setProperty("readingAt", now.format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityNotFoundError> response = template.exchange(uri, HttpMethod.PUT,
				hourlyElectricity, HourlyElectricityNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		HourlyElectricityNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("hourly.electricity.id.not.found"),
				hourlyElectricityId), error.getMessage());
		Assert.assertEquals(hourlyElectricityId, error.getId());
	}

	@Test
	public void update_HourlyElectricityNotGeneratedByPanel_UnprocessableEntityErrorReturned() {
		Long panelId = 2L;
		Long hourlyElectricityId = 1L;
		String uri = String.format("/api/panels/%s/hourly/%s", panelId, hourlyElectricityId);

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityPanelMismatchError> response = template.exchange(uri, HttpMethod.PUT,
				hourlyElectricity, HourlyElectricityPanelMismatchError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		HourlyElectricityPanelMismatchError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_PANEL_MISMATCH.value(), error.getCode());
		Assert.assertEquals(
				String.format(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.panel.mismatch"),
						hourlyElectricityId, panelId),
				error.getMessage());
	}

	@Test
	public void delete_HourlyElectricityNotGeneratedByPanel_UnprocessableEntityErrorReturned() throws Exception {
		Long panelId = 2L;
		Long hourlyElectricityId = 1L;

		String uri = String.format("/api/panels/%s/hourly/%s", panelId, hourlyElectricityId);

		ResponseEntity<HourlyElectricityPanelMismatchError> response = template.exchange(uri, HttpMethod.DELETE, null,
				HourlyElectricityPanelMismatchError.class);
		
		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		HourlyElectricityPanelMismatchError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_PANEL_MISMATCH.value(), error.getCode());
		Assert.assertEquals(
				String.format(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.panel.mismatch"),
						hourlyElectricityId, panelId),
				error.getMessage());
	}

	
	@Test
	public void delete_PanelIdAndHourlyElectricityIdGiven_HourlyElectricityDeleted() {
		String uri = "/api/panels/1/hourly/1";

		ResponseEntity<?> response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The first delete operation should return a NO_CONTENT HTTP status code.
		Assert.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

		response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The second delete operation should return a NOT_FOUND HTTP status code.
		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	public void delete_PanelDoesnotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly/1", panelId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.DELETE, null,
				PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void delete_HourlyElectricityDoesnotExist_NotFoundErrorReturned() {
		Long hourlyElectricityId = 5000L;
		String uri = String.format("/api/panels/1/hourly/%s", hourlyElectricityId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.DELETE, null,
				PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("hourly.electricity.id.not.found"),
				hourlyElectricityId), error.getMessage());
		Assert.assertEquals(hourlyElectricityId, error.getId());
	}

	@Test
	public void getByPanelId_PanelIdGiven_HourlyElectricitiesReturned() throws Exception {
		// We construct and make a GET request that should return 3 JSON hourly
		// electricity objects starting from page 0.
		ResponseEntity<List<HourlyElectricityDto>> response = template.exchange("/api/panels/1/hourly?page=0&size=3",
				HttpMethod.GET, null, new ParameterizedTypeReference<List<HourlyElectricityDto>>() {
				});

		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		List<HourlyElectricityDto> hourlyElectricities = response.getBody();
		Assert.assertNotNull(hourlyElectricities);
		Assert.assertEquals(3, hourlyElectricities.size());

		Assert.assertTrue(
				RegexUtil.endsWithDigit("/api/panels/1/hourly/", hourlyElectricities.get(0).getUri().toString()));
		Assert.assertTrue(
				RegexUtil.endsWithDigit("/api/panels/1/hourly/", hourlyElectricities.get(1).getUri().toString()));
		Assert.assertTrue(
				RegexUtil.endsWithDigit("/api/panels/1/hourly/", hourlyElectricities.get(2).getUri().toString()));
	}

	@Test
	public void getByPanelId_PanelDoesNotExist_NotFoundErrorReturned() throws Exception {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly?page=0&size=3", panelId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.GET, null,
				PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void countByPanelId_PanelExists_HourlyElectricitiesCountReturned() {
		ResponseEntity<Long> response = template.getForEntity("/api/panels/1/hourly/count", Long.class);

		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		Long actual = response.getBody();
		Assert.assertEquals(Long.valueOf(10), actual);
	}

	@Test
	public void countByPanelId_PanelDoesNotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly/count", panelId);

		ResponseEntity<PanelNotFoundError> response = template.getForEntity(uri, PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

}
