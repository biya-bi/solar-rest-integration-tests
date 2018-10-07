package org.rainbow.solar.rest.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author biya-bi
 *
 */
public class DatabaseUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUtil.class);

	public static void execute(DataSource dataSource, String... sqlFilePaths)
			throws SQLException, FileNotFoundException, IOException {
		Objects.requireNonNull(dataSource, "The dataSource argument cannot be null.");
		Objects.requireNonNull(sqlFilePaths, "The sqlFilePaths argument cannot be null.");

		try (Connection connection = dataSource.getConnection()) {
			for (String sqlFilePath : sqlFilePaths) {
				Objects.requireNonNull(sqlFilePath, "A null was found where an sql file path was expected.");

				execute(connection, new File(sqlFilePath));
			}
		}
	}

	public static void execute(DataSource dataSource, File... sqlFiles)
			throws SQLException, FileNotFoundException, IOException {
		Objects.requireNonNull(dataSource, "The dataSource argument cannot be null.");
		Objects.requireNonNull(sqlFiles, "The sqlFiles argument cannot be null.");

		try (Connection connection = dataSource.getConnection()) {
			for (File file : sqlFiles) {
				execute(connection, file);
			}
		}
	}

	private static void execute(Connection connection, File file)
			throws IOException, FileNotFoundException, SQLException {
		Objects.requireNonNull(connection, "The connection argument cannot be null.");
		Objects.requireNonNull(file, "The file argument cannot be null.");

		try (FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				Statement statement = connection.createStatement();) {

			StringBuilder stringBuilder = new StringBuilder();

			String line;
			// Be sure to not have line starting with "--" or "/*" or any other
			// non aplhabetical character
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}

			// Here is our splitter. We use ";" as a delimiter for each request
			// then we are sure to have well formed statements
			String[] instructions = stringBuilder.toString().split(";");

			LOGGER.info(String.format("Started executing the file: '%s'", file.getAbsolutePath()));
			for (String instruction : instructions) {
				// We ensure to that there is no spaces before or after the request string in
				// order to not execute empty statements
				if (!instruction.trim().equals("")) {
					statement.executeUpdate(instruction);
					LOGGER.info(String.format(">> %s", instruction));
				}
			}
			LOGGER.info(String.format("Finished executing the file: '%s'", file.getAbsolutePath()));

		} catch (SQLException e) {
			throw new SQLException(String.format("*** An exception was thrown while executing the file '%s': ",
					file.getAbsolutePath()), e);
		}
	}
}
