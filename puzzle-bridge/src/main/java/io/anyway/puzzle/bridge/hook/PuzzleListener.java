package io.anyway.puzzle.bridge.hook;

import java.util.EventListener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.web.context.ContextLoaderListener;

/**
 * Framework Bridge Listener
 * 
 * @author yangzz
 *
 */
public class PuzzleListener extends ContextLoaderListener implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener, HttpSessionActivationListener, HttpSessionBindingListener
{

	private static EventListener delegate;

	final public static synchronized void registerListenerDelegate(EventListener delegate)
	{
		PuzzleListener.delegate = delegate;
	}

	final public static synchronized void unregisterListenerDelegate()
	{
		delegate = null;
	}

	@Override
	final public void contextInitialized(ServletContextEvent event)
	{
		super.contextInitialized(event);
	}

	@Override
	final public synchronized void contextDestroyed(ServletContextEvent event)
	{
		delegate = null;
		super.contextDestroyed(event);
	}

	@Override
	public void valueBound(HttpSessionBindingEvent event)
	{
		((HttpSessionBindingListener) delegate).valueBound(event);
	}

	@Override
	public void valueUnbound(HttpSessionBindingEvent event)
	{
		((HttpSessionBindingListener) delegate).valueUnbound(event);
	}

	@Override
	public void sessionWillPassivate(HttpSessionEvent event)
	{
		((HttpSessionActivationListener) delegate).sessionWillPassivate(event);
	}

	@Override
	public void sessionDidActivate(HttpSessionEvent event)
	{
		((HttpSessionActivationListener) delegate).sessionDidActivate(event);
	}

	@Override
	public void attributeAdded(HttpSessionBindingEvent event)
	{
		((HttpSessionAttributeListener) delegate).attributeAdded(event);
	}

	@Override
	public void attributeRemoved(HttpSessionBindingEvent event)
	{
		((HttpSessionAttributeListener) delegate).attributeRemoved(event);
	}

	@Override
	public void attributeReplaced(HttpSessionBindingEvent event)
	{
		((HttpSessionAttributeListener) delegate).attributeReplaced(event);
	}
	
	@Override
	public void sessionCreated(HttpSessionEvent event)
	{
		((HttpSessionListener) delegate).sessionCreated(event);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event)
	{
		((HttpSessionListener) delegate).sessionDestroyed(event);
	}
}
