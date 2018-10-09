/**
 *
 */
package org.rainbow.solar.rest.controller;

import java.net.URI;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.rainbow.solar.model.Panel;
import org.rainbow.solar.model.UnitOfMeasure;
import org.rainbow.solar.rest.dto.PanelDto;
import org.rainbow.solar.rest.err.PanelNotFoundError;
import org.rainbow.solar.rest.err.PanelSerialDuplicateError;
import org.rainbow.solar.rest.err.PanelSerialMaxLengthExceededError;
import org.rainbow.solar.rest.err.PanelSerialRequiredError;
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
public class PanelControllerTests extends ControllerTests {

	@Test
	public void create_PanelIsValid_PanelCreated() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "232323")
				.setProperty("latitude", 54.123232).setProperty("longitude", 54.123232).setProperty("brand", "tesla")
				.setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<?> response = template.postForEntity("/api/panels", panel, Object.class);

		Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());

		URI location = response.getHeaders().getLocation();

		Assert.assertNotNull(location);
		Assert.assertTrue(RegexUtil.endsWithDigit("/api/panels/", location.toString()));
	}

	@Test
	public void create_SerialNumberIsEmpty_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "")
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("brand", "suntech").setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<PanelSerialRequiredError> response = template.postForEntity("/api/panels", panel,
				PanelSerialRequiredError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		PanelSerialRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("panel.serial.required"), error.getMessage());
	}

	@Test
	public void create_SerialNumberLengthIsGreaterThanMaximum_UnprocessableEntityErrorReturned() {
		String serial = "1234567890123456789";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("brand", "suntech").setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<PanelSerialMaxLengthExceededError> response = template.postForEntity("/api/panels", panel,
				PanelSerialMaxLengthExceededError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		PanelSerialMaxLengthExceededError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_MAX_LENGTH_EXCEEDED.value(), error.getCode());
		Assert.assertEquals(
				String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.length.too.long"), serial, 16),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
		Assert.assertEquals(Integer.valueOf(16), Integer.valueOf(error.getMaxLength()));
	}

	@Test
	public void create_AnotherPanelHasSameSerial_UnprocessableEntityErrorReturned() {
		String serial = "100001";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "80.446189").setProperty("longitude", "85.756328")
				.setProperty("brand", "suntech").setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<PanelSerialDuplicateError> response = template.postForEntity("/api/panels", panel,
				PanelSerialDuplicateError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		PanelSerialDuplicateError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_DUPLICATE.value(), error.getCode());
		Assert.assertEquals(String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.duplicate"), serial),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
	}

	@Test
	public void update_PanelIsValid_PanelUpdated() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "22222")
				.setProperty("latitude", 80.123456).setProperty("longitude", 81.654321).setProperty("brand", "tesla")
				.setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<Panel> response = template.exchange("/api/panels/2", HttpMethod.PUT, panel, Panel.class);

		Assert.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void update_SerialNumberIsEmpty_UnprocessableEntityErrorReturned() {
		Long panelId = 3L;
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "")
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("unitOfMeasure", "KW").setProperty("brand", "suntech").build();

		ResponseEntity<PanelSerialDuplicateError> response = template.exchange("/api/panels/" + panelId, HttpMethod.PUT,
				panel, PanelSerialDuplicateError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		PanelSerialDuplicateError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("panel.serial.required"), error.getMessage());
	}

	@Test
	public void update_SerialNumberLengthIsGreaterThanMaximum_UnprocessableEntityErrorReturned() {
		String serial = "1234567890123456789";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("unitOfMeasure", "KW").setProperty("brand", "suntech").build();

		ResponseEntity<PanelSerialMaxLengthExceededError> response = template.exchange("/api/panels/3", HttpMethod.PUT,
				panel, PanelSerialMaxLengthExceededError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		PanelSerialMaxLengthExceededError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_MAX_LENGTH_EXCEEDED.value(), error.getCode());
		Assert.assertEquals(
				String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.length.too.long"), serial, 16),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
		Assert.assertEquals(Integer.valueOf(16), Integer.valueOf(error.getMaxLength()));
	}

	@Test
	public void update_AnotherPanelHasSameSerial_UnprocessableEntityErrorReturned() {
		String serial = "100001";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "80.446189").setProperty("longitude", "85.756328")
				.setProperty("unitOfMeasure", "KW").setProperty("brand", "suntech").build();

		ResponseEntity<PanelSerialDuplicateError> response = template.exchange("/api/panels/3", HttpMethod.PUT, panel,
				PanelSerialDuplicateError.class);

		Assert.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

		PanelSerialDuplicateError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_DUPLICATE.value(), error.getCode());
		Assert.assertEquals(String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.duplicate"), serial),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
	}

	@Test
	public void update_PanelDoesNotExist_NotFoundErrorReturned() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "22222")
				.setProperty("latitude", 80.123456).setProperty("longitude", 81.654321).setProperty("brand", "tesla")
				.setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<Panel> response = template.exchange("/api/panels/5000", HttpMethod.PUT, panel, Panel.class);

		Assert.assertEquals(404, response.getStatusCode().value());
	}

	@Test
	public void delete_PanelIdGiven_PanelDeleted() {
		String uri = "/api/panels/4";

		ResponseEntity<?> response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The first delete operation should return a NO_CONTENT HTTP status code.
		Assert.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

		response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The second delete operation should return a NOT_FOUND HTTP status code.
		Assert.assertEquals(404, response.getStatusCode().value());
	}

	@Test
	public void delete_PanelDoesNotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s", panelId);

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
	public void getById_PanelIdGiven_PanelReturned() {
		String uri = "/api/panels/1";

		ResponseEntity<PanelDto> response = template.getForEntity(uri, PanelDto.class);

		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		PanelDto actual = response.getBody();
		Assert.assertNotNull(actual);
		Assert.assertTrue(actual.getUri().toString().endsWith(uri));
		Assert.assertEquals("100001", actual.getSerial());
		Assert.assertEquals(Double.valueOf(70.650001), actual.getLatitude());
		Assert.assertEquals(Double.valueOf(72.512351), actual.getLongitude());
		Assert.assertEquals("canadiansolar", actual.getBrand());
		Assert.assertTrue(actual.getHourlyUri().toString().endsWith(uri + "/hourly"));
		Assert.assertTrue(actual.getDailyUri().toString().endsWith(uri + "/daily"));
		Assert.assertTrue(actual.getHourlyCountUri().toString().endsWith(uri + "/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.W.toString(), actual.getUnitOfMeasure());
	}

	@Test
	public void getById_PanelDoesNotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s", panelId);

		ResponseEntity<PanelNotFoundError> response = template.getForEntity(uri, PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void get_PanelsExist_PanelsReturned() throws Exception {
		ResponseEntity<List<PanelDto>> response = template.exchange("/api/panels", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<PanelDto>>() {
				});

		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		List<PanelDto> panelDtos = response.getBody();
		Assert.assertNotNull(panelDtos);
		Assert.assertEquals(5, panelDtos.size());

		PanelDto panelDto1 = panelDtos.get(0);
		Assert.assertEquals("100001", panelDto1.getSerial());
		Assert.assertEquals(Double.valueOf(70.650001), panelDto1.getLatitude());
		Assert.assertEquals(Double.valueOf(72.512351), panelDto1.getLongitude());
		Assert.assertEquals("canadiansolar", panelDto1.getBrand());
		Assert.assertTrue(panelDto1.getUri().toString().endsWith("/api/panels/1"));
		Assert.assertTrue(panelDto1.getHourlyUri().toString().endsWith("/api/panels/1/hourly"));
		Assert.assertTrue(panelDto1.getDailyUri().toString().endsWith("/api/panels/1/daily"));
		Assert.assertTrue(panelDto1.getHourlyCountUri().toString().endsWith("/api/panels/1/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.W.toString(), panelDto1.getUnitOfMeasure());

		PanelDto panelDto2 = panelDtos.get(1);
		Assert.assertEquals("100002", panelDto2.getSerial());
		Assert.assertEquals(Double.valueOf(60.753268), panelDto2.getLatitude());
		Assert.assertEquals(Double.valueOf(62.412378), panelDto2.getLongitude());
		Assert.assertEquals("sunpower", panelDto2.getBrand());
		Assert.assertTrue(panelDto2.getUri().toString().endsWith("/api/panels/2"));
		Assert.assertTrue(panelDto2.getHourlyUri().toString().endsWith("/api/panels/2/hourly"));
		Assert.assertTrue(panelDto2.getDailyUri().toString().endsWith("/api/panels/2/daily"));
		Assert.assertTrue(panelDto2.getHourlyCountUri().toString().endsWith("/api/panels/2/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.KW.toString(), panelDto2.getUnitOfMeasure());

		PanelDto panelDto3 = panelDtos.get(2);
		Assert.assertEquals("100003", panelDto3.getSerial());
		Assert.assertEquals(Double.valueOf(85.112412), panelDto3.getLatitude());
		Assert.assertEquals(Double.valueOf(84.415987), panelDto3.getLongitude());
		Assert.assertEquals("jasolar", panelDto3.getBrand());
		Assert.assertTrue(panelDto3.getUri().toString().endsWith("/api/panels/3"));
		Assert.assertTrue(panelDto3.getHourlyUri().toString().endsWith("/api/panels/3/hourly"));
		Assert.assertTrue(panelDto3.getDailyUri().toString().endsWith("/api/panels/3/daily"));
		Assert.assertTrue(panelDto3.getHourlyCountUri().toString().endsWith("/api/panels/3/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.KW.toString(), panelDto3.getUnitOfMeasure());

		PanelDto panelDto4 = panelDtos.get(3);
		Assert.assertEquals("100004", panelDto4.getSerial());
		Assert.assertEquals(Double.valueOf(50.976518), panelDto4.getLatitude());
		Assert.assertEquals(Double.valueOf(51.014987), panelDto4.getLongitude());
		Assert.assertEquals("qcells", panelDto4.getBrand());
		Assert.assertTrue(panelDto4.getUri().toString().endsWith("/api/panels/4"));
		Assert.assertTrue(panelDto4.getHourlyUri().toString().endsWith("/api/panels/4/hourly"));
		Assert.assertTrue(panelDto4.getDailyUri().toString().endsWith("/api/panels/4/daily"));
		Assert.assertTrue(panelDto4.getHourlyCountUri().toString().endsWith("/api/panels/4/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.W.toString(), panelDto4.getUnitOfMeasure());

		PanelDto panelDto5 = panelDtos.get(4);
		Assert.assertEquals("100005", panelDto5.getSerial());
		Assert.assertEquals(Double.valueOf(75.45127), panelDto5.getLatitude());
		Assert.assertEquals(Double.valueOf(74.359468), panelDto5.getLongitude());
		Assert.assertEquals("rec", panelDto5.getBrand());
		Assert.assertTrue(panelDto5.getUri().toString().endsWith("/api/panels/5"));
		Assert.assertTrue(panelDto5.getHourlyUri().toString().endsWith("/api/panels/5/hourly"));
		Assert.assertTrue(panelDto5.getDailyUri().toString().endsWith("/api/panels/5/daily"));
		Assert.assertTrue(panelDto5.getHourlyCountUri().toString().endsWith("/api/panels/5/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.KW.toString(), panelDto5.getUnitOfMeasure());
	}

	@Test
	public void get_PageNumberAndSizeGiven_PanelsReturned() throws Exception {
		ResponseEntity<List<PanelDto>> response = template.exchange("/api/panels?page=0&size=3", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<PanelDto>>() {
				});

		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		List<PanelDto> panelDtos = response.getBody();
		Assert.assertNotNull(panelDtos);
		Assert.assertEquals(3, panelDtos.size());

		PanelDto panelDto1 = panelDtos.get(0);
		Assert.assertEquals("100001", panelDto1.getSerial());
		Assert.assertEquals(Double.valueOf(70.650001), panelDto1.getLatitude());
		Assert.assertEquals(Double.valueOf(72.512351), panelDto1.getLongitude());
		Assert.assertEquals("canadiansolar", panelDto1.getBrand());
		Assert.assertTrue(panelDto1.getUri().toString().endsWith("/api/panels/1"));
		Assert.assertTrue(panelDto1.getHourlyUri().toString().endsWith("/api/panels/1/hourly"));
		Assert.assertTrue(panelDto1.getDailyUri().toString().endsWith("/api/panels/1/daily"));
		Assert.assertTrue(panelDto1.getHourlyCountUri().toString().endsWith("/api/panels/1/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.W.toString(), panelDto1.getUnitOfMeasure());

		PanelDto panelDto2 = panelDtos.get(1);
		Assert.assertEquals("100002", panelDto2.getSerial());
		Assert.assertEquals(Double.valueOf(60.753268), panelDto2.getLatitude());
		Assert.assertEquals(Double.valueOf(62.412378), panelDto2.getLongitude());
		Assert.assertEquals("sunpower", panelDto2.getBrand());
		Assert.assertTrue(panelDto2.getUri().toString().endsWith("/api/panels/2"));
		Assert.assertTrue(panelDto2.getHourlyUri().toString().endsWith("/api/panels/2/hourly"));
		Assert.assertTrue(panelDto2.getDailyUri().toString().endsWith("/api/panels/2/daily"));
		Assert.assertTrue(panelDto2.getHourlyCountUri().toString().endsWith("/api/panels/2/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.KW.toString(), panelDto2.getUnitOfMeasure());

		PanelDto panelDto3 = panelDtos.get(2);
		Assert.assertEquals("100003", panelDto3.getSerial());
		Assert.assertEquals(Double.valueOf(85.112412), panelDto3.getLatitude());
		Assert.assertEquals(Double.valueOf(84.415987), panelDto3.getLongitude());
		Assert.assertEquals("jasolar", panelDto3.getBrand());
		Assert.assertTrue(panelDto3.getUri().toString().endsWith("/api/panels/3"));
		Assert.assertTrue(panelDto3.getHourlyUri().toString().endsWith("/api/panels/3/hourly"));
		Assert.assertTrue(panelDto3.getDailyUri().toString().endsWith("/api/panels/3/daily"));
		Assert.assertTrue(panelDto3.getHourlyCountUri().toString().endsWith("/api/panels/3/hourly/count"));
		Assert.assertEquals(UnitOfMeasure.KW.toString(), panelDto3.getUnitOfMeasure());
	}

	@Test
	public void count_PanelsExist_PanelsCountReturned() {
		ResponseEntity<Long> response = template.getForEntity("/api/panels/count", Long.class);

		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		Long actual = response.getBody();

		Assert.assertEquals(Long.valueOf(5), actual);
	}

}
