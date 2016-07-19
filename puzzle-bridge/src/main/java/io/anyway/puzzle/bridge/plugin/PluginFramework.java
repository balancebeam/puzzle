package io.anyway.puzzle.bridge.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.anyway.puzzle.bridge.plugin.internal.PluginFrameworkClassLoader;
import io.anyway.puzzle.bridge.plugin.internal.PluginPropertyBuilder;
/**
 * OSGi framework manager
 * @author yangzz
 *
 */
public class PluginFramework {
	
	private Log logger = LogFactory.getLog(PluginFramework.class);
	
	private String eclipseStarter = "org.eclipse.core.runtime.adaptor.EclipseStarter";
	
	private static ClassLoader frameworkContextClassLoader;
	
	private PluginFrameworkClassLoader frameworkClassLoader;
	
	private static ServletContext servletContext;
	
	final public static ServletContext getFrameworkServletContext(){
		return servletContext;
	}
	
	/**
	 * initialize
	 * @param ServletContext
	 */
	public void init(ServletContext context) { 
		synchronized (this){
			PluginFramework.servletContext= context;
		}
		
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			logger.info("plugin starter booting...");
			PluginPropertyBuilder builder= new PluginPropertyBuilder(context);
			System.setProperty("osgi.framework.useSystemProperties", "false"); 
			frameworkClassLoader = builder.getFrameworkClassLoader();
			Class<?> clazz = frameworkClassLoader.loadClass(eclipseStarter);
			Method setInitialProperties = clazz.getDeclaredMethod("setInitialProperties", new Class[] { Map.class }); 
			setInitialProperties.invoke(null,	new Object[] { builder.getFrameworkProperties() });
			logger.info("OSGi Engine initialization properties: "+builder.getFrameworkProperties());

			Method runMethod = clazz.getDeclaredMethod("startup", new Class[] { String[].class, Runnable.class }); 
			runMethod.invoke(null, new Object[] { builder.buildCommandArguments(), null });
			PluginFramework.frameworkContextClassLoader = Thread.currentThread().getContextClassLoader();

		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t == null)
				t = e;
			logger.error("OSGi Engine boot failure", t);
			throw new IllegalArgumentException(t.getMessage());
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new IllegalArgumentException(e.getMessage(),e);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}
		
	public void destroy(){
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Class<?> clazz = frameworkClassLoader.loadClass(eclipseStarter);
			Method method = clazz.getDeclaredMethod("shutdown", (Class[]) null); //$NON-NLS-1$
			Thread.currentThread().setContextClassLoader(frameworkContextClassLoader);
			method.invoke(clazz, (Object[]) null);
			logger.info("OSGi Engine destroy success");
		} catch (Exception e) {
			logger.error("OSGi Engine destroy failure", e);
		} finally {
			frameworkClassLoader = null;
			frameworkContextClassLoader = null;
			Thread.currentThread().setContextClassLoader(original);
		}
	}
	
	final public static ClassLoader getFrameworkContextClassLoader() {
		return frameworkContextClassLoader;
	}

}
