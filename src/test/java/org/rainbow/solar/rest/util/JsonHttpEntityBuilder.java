/**
 *
 */
package org.rainbow.solar.rest.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * @author biya-bi
 *
 */
public class JsonHttpEntityBuilder {

	private Map<String, Object> map = new HashMap<>();

	public JsonHttpEntityBuilder setProperty(String key, Object value) {
		map.put(key, value);
		return this;
	}

	public HttpEntity<Object> build() {
		StringBuilder b = new StringBuilder();
		b.append("{");
		int i = 0;
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			b.append("\"");
			b.append(entry.getKey());
			b.append("\":");
			b.append("\"");
			b.append(entry.getValue());
			b.append("\"");
			if (i < map.size() - 1)
				b.append(",");
			i++;

		}
		b.append("}");
		return getHttpEntity(b.toString());
	}

	private HttpEntity<Object> getHttpEntity(Object body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<Object>(body, headers);
	}

}