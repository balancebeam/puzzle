package io.anyway.puzzle.core.hook;

import java.util.EventListener;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import io.anyway.puzzle.bridge.hook.PuzzleListener;
import io.anyway.puzzle.core.servlet.PluginWebInstanceRepository;

/**
 * Hook Listener delegate
 * @author yangzz
 *
 */
public class PluginHookListener implements 
	HttpSessionListener, 
	HttpSessionAttributeListener,
	HttpSessionActivationListener, 
	HttpSessionBindingListener{
	
	public PluginHookListener(){
		PuzzleListener.registerListenerDelegate(this);
	}
	
	public void valueBound(HttpSessionBindingEvent event) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) listener).valueBound(event);
			}
		}
	}

	public void valueUnbound(HttpSessionBindingEvent event) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionActivationListener) {
				((HttpSessionBindingListener) listener).valueUnbound(event);
			}
		}
	}

	public void sessionWillPassivate(HttpSessionEvent se) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionActivationListener) {
				((HttpSessionActivationListener) listener).sessionWillPassivate(se);
			}
		}
	}

	public void sessionDidActivate(HttpSessionEvent se) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionAttributeListener) {
				((HttpSessionActivationListener) listener).sessionDidActivate(se);
			}
		}
	}

	public void attributeAdded(HttpSessionBindingEvent se) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionAttributeListener) {
				((HttpSessionAttributeListener) listener).attributeAdded(se);
			}
		}
	}

	public void attributeRemoved(HttpSessionBindingEvent se) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionAttributeListener) {
				((HttpSessionAttributeListener) listener).attributeRemoved(se);
			}
		}
	}

	public void attributeReplaced(HttpSessionBindingEvent se) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionAttributeListener) {
				((HttpSessionAttributeListener) listener).attributeReplaced(se);
			}
		}
	}

	public void sessionCreated(HttpSessionEvent se) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionListener) {
				((HttpSessionListener) listener).sessionCreated(se);
			}
		}
	}

	public void sessionDestroyed(HttpSessionEvent se) {
		for (EventListener listener: PluginWebInstanceRepository.getListeners()) {
			if (listener instanceof HttpSessionListener) {
				((HttpSessionListener) listener).sessionDestroyed(se);
			}
		}
	}
	
	public void destroy(){
		PuzzleListener.unregisterListenerDelegate();
	}

}
