/**
 * 
 */
package org.rainbow.solar.rest.util;

import java.util.regex.Pattern;

/**
 * @author biya-bi
 *
 */
public class RegexUtil {
	public static boolean endsWithDigit(String pattern, String input) {
		Pattern p = Pattern.compile(String.format("^.*%s\\d+$", pattern));
		return p.matcher(input).matches();
	}
}
