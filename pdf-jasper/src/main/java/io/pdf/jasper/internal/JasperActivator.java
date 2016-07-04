package io.pdf.jasper.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.pdf.core.PluginContext;
import io.pdf.core.common.PluginLifeCycleManager;
import io.pdf.core.internal.PluginContextSupport;

public class JasperActivator implements BundleActivator{

	private PluginContext pluginContext;
	
	private Log logger = LogFactory.getLog(JasperActivator.class);
	
	@Override
	public void start(BundleContext context) throws Exception {
		pluginContext= new PluginContextSupport(context.getBundle());
		try {
			PluginLifeCycleManager.start(pluginContext);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			PluginLifeCycleManager.stop(pluginContext);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
}

