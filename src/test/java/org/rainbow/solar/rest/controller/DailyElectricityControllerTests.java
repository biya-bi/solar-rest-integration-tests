/**
 *
 */
package org.rainbow.solar.rest.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.rainbow.solar.model.DailyElectricity;
import org.rainbow.solar.rest.err.PanelNotFoundError;
import org.rainbow.solar.rest.err.SolarErrorCode;
import org.rainbow.solar.rest.util.ErrorMessagesResourceBundle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * This class tests APIs in {@link PanelController}
 * 
 * @author biya-bi
 *
 */
public class DailyElectricityControllerTests extends ControllerTests {

	@Test
	public void getBeforeToday_PanelIdGiven_DailyElectricitiesReturned() throws Exception {
		ResponseEntity<List<DailyElectricity>> response = template.exchange("/api/panels/2/daily", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<DailyElectricity>>() {
				});

		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

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
	public void getBeforeToday_PanelDoesNotExist_NotFoundErrorReturned() throws Exception {
		Long panelId = 5000L;
		String uri = String.format("/api/panels/%s/daily", panelId);

		ResponseEntity<PanelNotFoundError> response = template.exchange(uri, HttpMethod.GET, null,
				PanelNotFoundError.class);

		Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

		PanelNotFoundError error = response.getBody();
		Assert.assertEquals(SolarErrorCode.PANEL_ID_NOT_FOUND.value(), error.getCode());
		Assert.assertEquals(String.format(ErrorMessagesResourceBundle.getMessage("panel.id.not.found"), panelId),
				error.getMessage());
		Assert.assertEquals(panelId, error.getId());
	}

}
