package io.anyway.puzzle.core.event;

import java.util.HashMap;
import java.util.Map;

import io.anyway.puzzle.core.common.Constants;
import io.anyway.puzzle.core.exception.PuzzleException;
/**
 * Event
 * @author yangzz
 *
 */
public class Event {
	
	private final String topic;
	
	private Map<String,Object> properties;
	
	public Event(String topic){
		this(topic,null);
	}
	
	public Event(String topic, Map<String, Object> properties) {
		if(topic==null || "".equals(topic)){
			throw new PuzzleException("Topic can not be empty");
		}
		this.topic = topic;
		this.properties= null!=properties? properties: new HashMap<String,Object>();
	}
	
	public final String getTopic() {
		return topic;
	}
	
	public final Object getProperty(String name) {
		if (Constants.EVENT_TOPIC.equals(name)) {
			return topic;
		}
		return properties.get(name);
	}
	
	public final boolean containsProperty(String name) {
		if (Constants.EVENT_TOPIC.equals(name)) {
			return true;
		}
		return properties.containsKey(name);
	}
	
	public final void put(String key,Object value){
		properties.put(key, value);
	}
	
	public final String[] getPropertyNames() {
		int size = properties.size();
		String[] result = new String[size + 1];
		properties.keySet().toArray(result);
		result[size] = Constants.EVENT_TOPIC;
		return result;
	}
	
	@Override
	public String toString() {
		return getClass().getName() + " [topic=" + topic + "]";
	}
}
