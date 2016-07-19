package io.anyway.puzzle.demo.listener;

import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class DemoListener implements ServletContextListener,HttpSessionListener{

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		System.out.println("-------------create session-------------");
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println("-------------destory session-------------");
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		System.out.println("-------------init listener-------------");
		@SuppressWarnings("unchecked")
		Enumeration<String> each= sce.getServletContext().getInitParameterNames();
		while(each.hasMoreElements()){
			String key= each.nextElement();
			String value= sce.getServletContext().getInitParameter(key);
			System.out.println(sce.getServletContext().getServletContextName()+": "+key+"="+value);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("-------------destory listener-------------");
	}

}
