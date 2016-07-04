package io.pdf.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.pdf.bridge.plugin.PluginFramework;
import io.pdf.core.PluginContext;
import io.pdf.core.common.Constants;
import io.pdf.core.common.PluginLifeCycleManager;
import io.pdf.core.hook.PluginHookFilter;
import io.pdf.core.hook.PluginHookListener;
import io.pdf.core.servlet.PluginServletManager;

public class PluginActivator implements BundleActivator,SynchronousBundleListener,FrameworkListener{
	
	private Log logger = LogFactory.getLog(PluginActivator.class);
	
	private PluginHookListener hookListener;
	
	private PluginHookFilter hookFilter;
	
	private PluginServletManager servletManager;
	
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		bundleContext.addBundleListener(this);
		bundleContext.addFrameworkListener(this);
		hookListener= new PluginHookListener();
		hookFilter= new PluginHookFilter();
		hookFilter.setApplicationContext(WebApplicationContextUtils.getWebApplicationContext(PluginFramework.getFrameworkServletContext()));
	
		PluginContext ctx= new PluginContextSupport(bundleContext.getBundle());
		servletManager= new PluginServletManager(ctx);
		servletManager.init(bundleContext.getBundle().getResource(Constants.WEB_CONFIG));
		servletManager.newInstance();
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		bundleContext.removeBundleListener(this);
		bundleContext.addFrameworkListener(this);
		servletManager.destroy();
		hookListener.destroy();
		hookFilter.destroy();
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle= event.getBundle();
		//如果不是PDF插件
		if(!"true".equals(bundle.getHeaders().get("Bundle-PDF"))){
			return;
		}
		
		PluginContextSupport ctx= (PluginContextSupport)PluginRepository.getInstance().getPluginContext(bundle);
		switch(event.getType()){
			case BundleEvent.INSTALLED:
			case BundleEvent.RESOLVED:
				break;
			case BundleEvent.STARTING:
				PluginRepository.getInstance().addPluginContext(ctx = new PluginContextSupport(bundle));
				logger.info("Plugin ["+ctx.getName()+"] Context has been registered");
				try {
					ctx.setState(Bundle.STARTING);
					PluginLifeCycleManager.start(ctx);
				} catch (Throwable e) {
					ctx.setState(Bundle.STOPPING);
					logger.error("Plugin ["+ctx.getName()+"] started failure",e);
				}		
				break;
			case BundleEvent.STARTED:
				if(null!= ctx){
					if(ctx.getState()==Bundle.STOPPING){
						try {
							bundle.stop(); 
						} catch (BundleException e) {
							logger.error("Plugin ["+ctx.getName()+"] stopped failure",e);
						}
					}
					else{
						ctx.setState(Bundle.ACTIVE);
					}
				}
				break;
			case BundleEvent.STOPPING:
				if(null!= ctx){
					ctx.setState(Bundle.STOPPING);
					try {
						PluginLifeCycleManager.stop(ctx);
					} catch (Throwable e) {
						logger.error("Plugin ["+ctx.getName()+"] stopped failure",e);
					}
				}
				break;
			case BundleEvent.STOPPED:
				if(ctx!=null){
					ctx.setState(Bundle.RESOLVED);
					PluginRepository.getInstance().removePluginContext(bundle);
					logger.info("Plugin ["+ctx.getName()+"] Context has been unregistered");
				}
				break;
			case BundleEvent.UNINSTALLED:
				break;
		}
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		//TODO
	}

}
