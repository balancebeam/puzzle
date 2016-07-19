package io.anyway.puzzle.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.aware.PluginContextAware;
import io.anyway.puzzle.core.common.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.core.task.TaskExecutor;

/**
 * Event Publisher, other service can use it 
 * 
 * @author yangzz
 *
 */
@SuppressWarnings("rawtypes")
final public class EventPublisher implements PluginContextAware {

	private static Log logger = LogFactory.getLog(EventPublisher.class);

	private PluginContext pluginContext;
	
	private TaskExecutor taskExecutor;

	@Override
	final public void setPluginContext(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@SuppressWarnings("unchecked")
	private List<EventSubscriber> getHandlers(String topic) {
		BundleContext bundleContext = pluginContext.getBundle()
				.getBundleContext();
		List<EventSubscriber> handers = new ArrayList<EventSubscriber>();
		try {
			String expression = "(" + Constants.EVENT_TOPIC + "=" + topic + ")";
			ServiceReference[] references = bundleContext.getServiceReferences(
					EventSubscriber.class.getName(), expression);
			if (references != null) {
				for (ServiceReference reference : references) {
					handers.add((EventSubscriber) bundleContext
							.getService(reference));
				}
				// order subscriber
				Collections.sort(handers, new Comparator<EventSubscriber>() {
					@Override
					public int compare(EventSubscriber o1, EventSubscriber o2) {
						return o1.getPriority() - o2.getPriority();
					}
				});
			}
		} catch (InvalidSyntaxException e) {
			logger.error("handle topic [" + topic + "] error", e);
		}
		return handers;
	}

	/**
	 * publish event method
	 * 
	 * @param event
	 */
	public void sendEvent(Event event) {
		String topic = event.getTopic();
		List<EventSubscriber> handers = getHandlers(topic);
		if (handers.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("get event list of topic [" + topic + "] is empty");
			}
			return;
		}
		for (EventSubscriber handler : handers) {
			if (handler.isAsync()) {
				taskExecutor.execute(new EventTask(event, handler));
				continue;
			}
			handler.handleEvent(event);
		}
	}

	private static class EventTask implements Runnable {
		private Event event;
		private EventSubscriber handler;

		public EventTask(Event event, EventSubscriber handler) {
			this.event = event;
			this.handler = handler;
		}

		@Override
		public void run() {
			handler.handleEvent(event);
		}
	}
}
