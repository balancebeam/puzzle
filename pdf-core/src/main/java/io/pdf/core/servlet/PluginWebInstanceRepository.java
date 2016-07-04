package io.pdf.core.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;

import io.pdf.core.aware.ElementPostfixAware;
import io.pdf.core.aware.ElementPrefixAware;
import io.pdf.core.servlet.wrapper.FilterWrapper;
import io.pdf.core.servlet.wrapper.HttpServletWrapper;

public final class PluginWebInstanceRepository {

	final private static List<EventListener> LISTENERS= new ArrayList<EventListener>();
	
	final private static List<HttpServletWrapper> HTTPSERVLETS= new ArrayList<HttpServletWrapper>();
	
	final private static ArrayList<FilterWrapper> FILTERS= new ArrayList<FilterWrapper>();
	
	public static void registerListeners(List<EventListener> listeners){
		LISTENERS.addAll(listeners);
	}

	public static void unregisterListeners(List<EventListener> listeners){
		LISTENERS.removeAll(listeners);
	}

	public static List<EventListener> getListeners(){
		return LISTENERS;
	}

	public static void registerFilters(List<FilterWrapper> filters){
		FILTERS.addAll(filters);
		Collections.sort(FILTERS, new Comparator<FilterWrapper>(){
			public int compare(FilterWrapper o1,FilterWrapper o2) {
				if(o1.getFilter() instanceof ElementPrefixAware && !(o2.getFilter() instanceof ElementPrefixAware)){
					return -1;
				}
				if(o2.getFilter() instanceof ElementPostfixAware && !(o1.getFilter() instanceof ElementPostfixAware)){
					return -1;
				}
				return o1.getPriority()- o2.getPriority();
			}
		});
	}

	public static void unregisterFilters(List<FilterWrapper> filters){
		FILTERS.removeAll(filters);
	}

	public static List<FilterWrapper> getFilters(){
		return FILTERS;
	}

	public static void registerHttpServlets(List<HttpServletWrapper> httpServlets){
		HTTPSERVLETS.addAll(httpServlets);
	}
	
	public static void unregisterHttpServlets(List<HttpServletWrapper> httpServlets){
		HTTPSERVLETS.removeAll(httpServlets);
	}
	
	public static List<HttpServletWrapper> getHttpServlets(){
		return HTTPSERVLETS;
	}
}
