/**
 *
 */
package org.rainbow.solar.rest.controller;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.rainbow.solar.model.DailyElectricity;
import org.rainbow.solar.model.Panel;
import org.rainbow.solar.model.UnitOfMeasure;
import org.rainbow.solar.rest.dto.HourlyElectricityDto;
import org.rainbow.solar.rest.dto.PanelDto;
import org.rainbow.solar.rest.err.HourlyElectricityNotFoundError;
import org.rainbow.solar.rest.err.HourlyElectricityReadingDateRequiredError;
import org.rainbow.solar.rest.err.HourlyElectricityReadingRequiredError;
import org.rainbow.solar.rest.err.PanelNotFoundError;
import org.rainbow.solar.rest.err.PanelSerialDuplicateError;
import org.rainbow.solar.rest.err.PanelSerialMaxLengthExceededError;
import org.rainbow.solar.rest.err.PanelSerialRequiredError;
import org.rainbow.solar.rest.err.SolarErrorCode;
import org.rainbow.solar.rest.util.DatabaseUtil;
import org.rainbow.solar.rest.util.ErrorMessagesResourceBundle;
import org.rainbow.solar.rest.util.JsonHttpEntityBuilder;
import org.rainbow.solar.rest.util.RegexUtil;
import org.rainbow.solar.service.util.ExceptionMessagesResourceBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This class tests APIs in {@link PanelController}
 * 
 * @author biya-bi
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class PanelControllerTests {

	@Mock
	private PanelController panelController;

	@Autowired
	private TestRestTemplate template;

	@Autowired
	private DataSource dataSource;

	@Before
	public void setup() throws Exception {
		DatabaseUtil.execute(dataSource, new ClassPathResource("sql/delete_from_tables.sql").getFile(),
				new ClassPathResource("sql/insert_panels.sql").getFile(),
				new ClassPathResource("sql/insert_hourly_electricities.sql").getFile());
	}

	@After
	public void cleanup() throws Exception {
		// Each test should clear the panel table to leave it in a state that will not
		// affect other tests.
		DatabaseUtil.execute(dataSource, new ClassPathResource("sql/delete_from_tables.sql").getFile());
	}

	@Test
	public void createPanel_AllFieldsAreValid_PanelCreated() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "232323")
				.setProperty("latitude", 54.123232).setProperty("longitude", 54.123232).setProperty("brand", "tesla")
				.setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<?> response = template.postForEntity("/api/panels", panel, Object.class);

		Assert.assertEquals(201, response.getStatusCode().value());

		URI location = response.getHeaders().getLocation();

		Assert.assertNotNull(location);
		Assert.assertTrue(RegexUtil.endsWithDigit("/api/panels/", location.toString()));
	}

	@Test
	public void createPanel_SerialNumberIsEmpty_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "")
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("brand", "suntech").setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<PanelSerialRequiredError> response = template.postForEntity("/api/panels", panel,
				PanelSerialRequiredError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		PanelSerialRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("panel.serial.required"), error.getMessage());
	}

	@Test
	public void createPanel_SerialNumberLengthIsGreaterThanMaximum_UnprocessableEntityErrorReturned() {
		String serial = "1234567890123456789";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("brand", "suntech").setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<PanelSerialMaxLengthExceededError> response = template.postForEntity("/api/panels", panel,
				PanelSerialMaxLengthExceededError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		PanelSerialMaxLengthExceededError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_MAX_LENGTH_EXCEEDED.value(), error.getCode());
		Assert.assertEquals(
				String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.length.too.long"), serial, 16),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
		Assert.assertEquals(Integer.valueOf(16), Integer.valueOf(error.getMaxLength()));
	}

	@Test
	public void createPanel_AnotherPanelHasSameSerial_UnprocessableEntityErrorReturned() {
		String serial = "100001";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "80.446189").setProperty("longitude", "85.756328")
				.setProperty("brand", "suntech").setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<PanelSerialDuplicateError> response = template.postForEntity("/api/panels", panel,
				PanelSerialDuplicateError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		PanelSerialDuplicateError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_DUPLICATE.value(), error.getCode());
		Assert.assertEquals(String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.duplicate"), serial),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
	}

	@Test
	public void updatePanel_AllFieldsAreValid_PanelUpdated() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "22222")
				.setProperty("latitude", 80.123456).setProperty("longitude", 81.654321).setProperty("brand", "tesla")
				.setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<Panel> response = template.exchange("/api/panels/2", HttpMethod.PUT, panel, Panel.class);

		Assert.assertEquals(204, response.getStatusCode().value());
	}

	@Test
	public void updatePanel_SerialNumberIsEmpty_UnprocessableEntityErrorReturned() {
		Long panelId = 3L;
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "")
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("unitOfMeasure", "KW").setProperty("brand", "suntech").build();

		ResponseEntity<PanelSerialDuplicateError> response = template.exchange("/api/panels/" + panelId, HttpMethod.PUT,
				panel, PanelSerialDuplicateError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		PanelSerialDuplicateError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("panel.serial.required"), error.getMessage());
	}

	@Test
	public void updatePanel_SerialNumberLengthIsGreaterThanMaximum_UnprocessableEntityErrorReturned() {
		String serial = "1234567890123456789";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "75.645289").setProperty("longitude", "75.147852")
				.setProperty("unitOfMeasure", "KW").setProperty("brand", "suntech").build();

		ResponseEntity<PanelSerialMaxLengthExceededError> response = template.exchange("/api/panels/3", HttpMethod.PUT,
				panel, PanelSerialMaxLengthExceededError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		PanelSerialMaxLengthExceededError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_MAX_LENGTH_EXCEEDED.value(), error.getCode());
		Assert.assertEquals(
				String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.length.too.long"), serial, 16),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
		Assert.assertEquals(Integer.valueOf(16), Integer.valueOf(error.getMaxLength()));
	}

	@Test
	public void updatePanel_AnotherPanelHasSameSerial_UnprocessableEntityErrorReturned() {
		String serial = "100001";

		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", serial)
				.setProperty("latitude", "80.446189").setProperty("longitude", "85.756328")
				.setProperty("unitOfMeasure", "KW").setProperty("brand", "suntech").build();

		ResponseEntity<PanelSerialDuplicateError> response = template.exchange("/api/panels/3", HttpMethod.PUT, panel,
				PanelSerialDuplicateError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		PanelSerialDuplicateError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_SERIAL_DUPLICATE.value(), error.getCode());
		Assert.assertEquals(String.format(ExceptionMessagesResourceBundle.getMessage("panel.serial.duplicate"), serial),
				error.getMessage());
		Assert.assertEquals(serial, error.getSerial());
	}

	@Test
	public void updatePanel_PanelDoesNotExist_NotFoundErrorReturned() {
		HttpEntity<Object> panel = new JsonHttpEntityBuilder().setProperty("serial", "22222")
				.setProperty("latitude", 80.123456).setProperty("longitude", 81.654321).setProperty("brand", "tesla")
				.setProperty("unitOfMeasure", "KW").build();

		ResponseEntity<Panel> response = template.exchange("/api/panels/5000", HttpMethod.PUT, panel, Panel.class);

		Assert.assertEquals(404, response.getStatusCode().value());
	}

	@Test
	public void deletePanel_PanelIdGiven_PanelDeleted() {
		String uri = "/api/panels/4";

		ResponseEntity<?> response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The first delete operation should return a NO_CONTENT HTTP status code.
		Assert.assertEquals(204, response.getStatusCode().value());

		response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The second delete operation should return a NOT_FOUND HTTP status code.
		Assert.assertEquals(404, response.getStatusCode().value());
	}

	@Test
	public void deletePanel_PanelDoesNotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s", panelId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.DELETE, null,
				PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void getPanel_PanelIdGiven_PanelReturned() {
		String uri = "/api/panels/1";

		ResponseEntity<PanelDto> response = template.getForEntity(uri, PanelDto.class);

		Assert.assertEquals(200, response.getStatusCode().value());

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
	public void getPanel_PanelDoesNotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s", panelId);

		ResponseEntity<PanelNotFoundError> response = template.getForEntity(uri, PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void getPanels_PanelsExist_PanelsReturned() throws Exception {
		ResponseEntity<List<PanelDto>> response = template.exchange("/api/panels", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<PanelDto>>() {
				});

		Assert.assertEquals(200, response.getStatusCode().value());

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
	public void getPanels_PageNumberAndSizeGiven_PanelsReturned() throws Exception {
		ResponseEntity<List<PanelDto>> response = template.exchange("/api/panels?page=0&size=3", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<PanelDto>>() {
				});

		Assert.assertEquals(200, response.getStatusCode().value());

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
	public void getPanelsCount_PanelsExist_PanelsCountReturned() {
		ResponseEntity<Long> response = template.getForEntity("/api/panels/count", Long.class);

		Assert.assertEquals(200, response.getStatusCode().value());

		Long actual = response.getBody();

		Assert.assertEquals(Long.valueOf(5), actual);
	}

	@Test
	public void createHourlyElectricity_AllFieldsAreValid_HourlyElectricityCreated() {
		LocalDateTime now = LocalDateTime.now();

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", now.format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<?> response = template.postForEntity("/api/panels/2/hourly", hourlyElectricity, Object.class);

		Assert.assertEquals(201, response.getStatusCode().value());

		URI location = response.getHeaders().getLocation();

		Assert.assertNotNull(location);
		Assert.assertTrue(RegexUtil.endsWithDigit("/api/panels/2/hourly/", location.toString()));
	}

	@Test
	public void createHourlyElectricity_PanelDoesnotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly", panelId);

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<PanelNotFoundError> response = template.postForEntity(uri, hourlyElectricity,
				PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void createHourlyElectricity_ReadingAtIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.build();

		ResponseEntity<HourlyElectricityReadingDateRequiredError> response = template.postForEntity(
				"/api/panels/3/hourly", hourlyElectricity, HourlyElectricityReadingDateRequiredError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		HourlyElectricityReadingDateRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_DATE_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.date.required"),
				error.getMessage());
	}

	@Test
	public void createHourlyElectricity_GeneratedElectricityIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder()
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityReadingRequiredError> response = template.postForEntity("/api/panels/4/hourly",
				hourlyElectricity, HourlyElectricityReadingRequiredError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		HourlyElectricityReadingRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.required"),
				error.getMessage());
	}

	@Test
	public void updateHourlyElectricity_AllFieldsAreValid_HourlyElectricityUpdated() {
		LocalDateTime now = LocalDateTime.now();

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "2000")
				.setProperty("readingAt", now.format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<?> response = template.exchange("/api/panels/1/hourly/1", HttpMethod.PUT, hourlyElectricity,
				PanelNotFoundError.class);

		Assert.assertEquals(204, response.getStatusCode().value());
	}

	@Test
	public void updateHourlyElectricity_PanelDoesnotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly/1", panelId);

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.PUT, hourlyElectricity,
				PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void updateHourlyElectricity_ReadingAtIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.build();

		ResponseEntity<HourlyElectricityReadingDateRequiredError> response = template.exchange("/api/panels/1/hourly/1",
				HttpMethod.PUT, hourlyElectricity, HourlyElectricityReadingDateRequiredError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		HourlyElectricityReadingDateRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_DATE_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.date.required"),
				error.getMessage());
	}

	@Test
	public void updateHourlyElectricity_GeneratedElectricityIsNotSpecified_UnprocessableEntityErrorReturned() {
		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder()
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityReadingRequiredError> response = template.exchange("/api/panels/1/hourly/1",
				HttpMethod.PUT, hourlyElectricity, HourlyElectricityReadingRequiredError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		HourlyElectricityReadingRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_READING_REQUIRED.value(), error.getCode());
		Assert.assertEquals(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.reading.required"),
				error.getMessage());
	}

	@Test
	public void updateHourlyElectricity_HourlyElectriciyDoesnotExist_NotFoundErrorReturned() {
		Long hourlyElectricityId = 5000L;
		String uri = String.format("/api/panels/1/hourly/%s", hourlyElectricityId);
		LocalDateTime now = LocalDateTime.now();

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "2500")
				.setProperty("readingAt", now.format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityNotFoundError> response = template.exchange(uri, HttpMethod.PUT,
				hourlyElectricity, HourlyElectricityNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		HourlyElectricityNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("hourly.electricity.id.not.found"),
				hourlyElectricityId), error.getMessage());
		Assert.assertEquals(hourlyElectricityId, error.getId());
	}

	@Test
	public void updateHourlyElectricity_HourlyElectricityNotGeneratedByPanel_UnprocessableEntityErrorReturned() {
		Long panelId = 2L;
		Long hourlyElectricityId = 1L;
		String uri = String.format("/api/panels/%s/hourly/%s", panelId, hourlyElectricityId);

		HttpEntity<Object> hourlyElectricity = new JsonHttpEntityBuilder().setProperty("generatedElectricity", "500")
				.setProperty("readingAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).build();

		ResponseEntity<HourlyElectricityReadingRequiredError> response = template.exchange(uri, HttpMethod.PUT,
				hourlyElectricity, HourlyElectricityReadingRequiredError.class);

		Assert.assertEquals(422, response.getStatusCode().value());

		HourlyElectricityReadingRequiredError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_PANEL_MISMATCH.value(), error.getCode());
		Assert.assertEquals(
				String.format(ExceptionMessagesResourceBundle.getMessage("hourly.electricity.panel.mismatch"),
						hourlyElectricityId, panelId),
				error.getMessage());
	}

	@Test
	public void deleteHourlyElectricity_PanelIdAndHourlyElectricityIdGiven_HourlyElectricityDeleted() {
		String uri = "/api/panels/1/hourly/1";

		ResponseEntity<?> response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The first delete operation should return a NO_CONTENT HTTP status code.
		Assert.assertEquals(204, response.getStatusCode().value());

		response = template.exchange(uri, HttpMethod.DELETE, null, Object.class);

		// The second delete operation should return a NOT_FOUND HTTP status code.
		Assert.assertEquals(404, response.getStatusCode().value());
	}

	@Test
	public void deleteHourlyElectricity_PanelDoesnotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly/1", panelId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.DELETE, null,
				PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void deleteHourlyElectricity_HourlyElectricityDoesnotExist_NotFoundErrorReturned() {
		Long hourlyElectricityId = 5000L;
		String uri = String.format("/api/panels/1/hourly/%s", hourlyElectricityId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.DELETE, null,
				PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.HOURLY_ELECTRICITY_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("hourly.electricity.id.not.found"),
				hourlyElectricityId), error.getMessage());
		Assert.assertEquals(hourlyElectricityId, error.getId());
	}

	@Test
	public void getHourlyElectricities_PanelIdGiven_HourlyElectricitiesReturned() throws Exception {
		// We construct and make a GET request that should return 3 JSON hourly
		// electricity objects starting from page 0.
		ResponseEntity<List<HourlyElectricityDto>> response = template.exchange("/api/panels/1/hourly?page=0&size=3",
				HttpMethod.GET, null, new ParameterizedTypeReference<List<HourlyElectricityDto>>() {
				});

		Assert.assertEquals(200, response.getStatusCode().value());

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
	public void getHourlyElectricities_PanelDoesNotExist_NotFoundErrorReturned() throws Exception {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly?page=0&size=3", panelId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.GET, null,
				PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void getAllDailyElectricityFromYesterday_PanelIdGiven_DailyElectricitiesReturned() throws Exception {
		ResponseEntity<List<DailyElectricity>> response = template.exchange("/api/panels/2/daily", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<DailyElectricity>>() {
				});

		Assert.assertEquals(200, response.getStatusCode().value());

		List<DailyElectricity> dailyElectricities = response.getBody();
		Assert.assertNotNull(dailyElectricities);
		Assert.assertEquals(3, dailyElectricities.size());

		LocalDate today = LocalDate.now();

		DailyElectricity dailyElectricity1 = dailyElectricities.get(0);

		Assert.assertEquals(today.minusDays(1), dailyElectricity1.getDate());
		Assert.assertEquals(Long.valueOf(4700), dailyElectricity1.getSum());
		Assert.assertEquals(Double.valueOf(1175), dailyElectricity1.getAverage());
		Assert.assertEquals(Long.valueOf(975), dailyElectricity1.getMin());
		Assert.assertEquals(Long.valueOf(1500), dailyElectricity1.getMax());

		DailyElectricity dailyElectricity2 = dailyElectricities.get(1);

		Assert.assertEquals(today.minusDays(2), dailyElectricity2.getDate());
		Assert.assertEquals(Long.valueOf(3025), dailyElectricity2.getSum());
		Assert.assertEquals(Double.valueOf(756.25), dailyElectricity2.getAverage());
		Assert.assertEquals(Long.valueOf(700), dailyElectricity2.getMin());
		Assert.assertEquals(Long.valueOf(850), dailyElectricity2.getMax());

		DailyElectricity dailyElectricity3 = dailyElectricities.get(2);

		Assert.assertEquals(today.minusDays(3), dailyElectricity3.getDate());
		Assert.assertEquals(Long.valueOf(3575), dailyElectricity3.getSum());
		Assert.assertEquals(Double.valueOf(893.75), dailyElectricity3.getAverage());
		Assert.assertEquals(Long.valueOf(800), dailyElectricity3.getMin());
		Assert.assertEquals(Long.valueOf(950), dailyElectricity3.getMax());
	}

	@Test
	public void getAllDailyElectricityFromYesterday_PanelDoesNotExist_NotFoundErrorReturned() throws Exception {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/daily", panelId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.GET, null,
				PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

	@Test
	public void getHourlyElectricitiesCount_PanelExists_HourlyElectricitiesCountReturned() {
		ResponseEntity<Long> response = template.getForEntity("/api/panels/1/hourly/count", Long.class);

		Assert.assertEquals(200, response.getStatusCode().value());

		Long actual = response.getBody();
		Assert.assertEquals(Long.valueOf(10), actual);
	}

	@Test
	public void getHourlyElectricitiesCount_PanelDoesNotExist_NotFoundErrorReturned() {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/hourly/count", panelId);

		ResponseEntity<PanelNotFoundError> response = template.getForEntity(uri, PanelNotFoundError.class);

		Assert.assertEquals(404, response.getStatusCode().value());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

}
