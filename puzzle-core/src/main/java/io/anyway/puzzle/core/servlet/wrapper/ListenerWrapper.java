package io.anyway.puzzle.core.servlet.wrapper;

import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ListenerWrapper {
	
	private EventListener listener;
	
	private ServletContextEvent event;
	
	public ListenerWrapper(EventListener listener,ServletContextEvent event){
		this.listener= listener;
		this.event= event;
	}
	
	public EventListener getListener(){
		return listener;
	}
	
	public ServletContextEvent getServletContextEvent(){
		return event;
	}
	
	public ServletContext getServletContext(){
		return event.getServletContext();
	}
}
