/**
 * 
 */
package org.rainbow.solar.rest.controller;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.rainbow.solar.rest.util.DatabaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author biya-bi
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class ControllerTests {
	
	@Autowired
	protected TestRestTemplate template;

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
}
