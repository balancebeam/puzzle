package io.anyway.puzzle.core.common;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import io.anyway.puzzle.core.event.PluginFrameworkListener;
import io.anyway.puzzle.core.service.PluginExtensionPointService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.util.CollectionUtils;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.internal.PluginContextSupport;
import io.anyway.puzzle.core.servlet.PluginServletManager;
import io.anyway.puzzle.core.spring.PluginSpringManager;

public final class PluginLifeCycleManager {
	
	private PluginLifeCycleManager(){}
	
	private static final Log logger = LogFactory.getLog(PluginLifeCycleManager.class);

	public static final void start(PluginContext ctx) throws Exception{
		
		logger.info("Begin to initialize Plugin ["+ctx.getName()+"]");
		long beginTime= System.currentTimeMillis();
		//最先解析servlet对象，主要先获取初始化参数
		PluginServletManager servletManager= new PluginServletManager(ctx);
		((PluginContextSupport)ctx).setServletManager(servletManager);
		URL url= ctx.findLocalResource(Constants.WEB_CONFIG);
		if(null!= url){
			servletManager.init(url);
			logger.info(Constants.WEB_CONFIG+ " has parsed");
		}
		//解析spring
		url= ctx.findLocalResource(Constants.SPRING_CONFIG);
		if(null!= url){
			PluginSpringManager springManager= new PluginSpringManager(ctx);
			((PluginContextSupport)ctx).setSpringManager(springManager);
			springManager.init("classpath*:"+Constants.SPRING_CONFIG);
			logger.info(Constants.SPRING_CONFIG+ " has parsed");
		}
		//实例化web.xml中所有资源
		servletManager.newInstance();
		
		//初始化自定义扩展点服务
		BundleContext bundleContext= ctx.getBundle().getBundleContext();
		List<PluginExtensionPointService> xServices= getExtensionServicePoints(bundleContext,1);
		if(!xServices.isEmpty()){
			logger.info("Extension Point Service is calling");
			for(PluginExtensionPointService each: xServices){
				each.register(ctx);
			}
		}
		
		//add service listener
		if(null!=((PluginContextSupport)ctx).getSpringManager()){
			((PluginContextSupport)ctx).getSpringManager().addServiceListener();
		}
		//if done execute plugin framework listener,else do not
		PluginFrameworkListener.Processor.execute(false);
		
		long endTime= System.currentTimeMillis();
		logger.info("Plugin ["+ctx.getName()+"] started success,consuming time: "+(endTime-beginTime));
	}
	
	public static final void stop(PluginContext ctx) throws Exception{
		BundleContext bundleContext= ctx.getBundle().getBundleContext();
		List<PluginExtensionPointService> xServices= getExtensionServicePoints(bundleContext,1);
		for(PluginExtensionPointService each: xServices){
			each.unregister(ctx);
		}
		((PluginContextSupport)ctx).destroy();
		logger.info("Plugin ["+ctx.getName()+"] destroyed success");
	}
	
	//获取扩展点服务
	private static List<PluginExtensionPointService> getExtensionServicePoints(BundleContext bundleContext,final int order) throws Exception{
		Collection<ServiceReference<PluginExtensionPointService>> serviceReferences= bundleContext.getServiceReferences(PluginExtensionPointService.class, null);
		List<PluginExtensionPointService> result= new LinkedList<PluginExtensionPointService>();
		if(!CollectionUtils.isEmpty(serviceReferences)){
			for(ServiceReference<PluginExtensionPointService> serviceReference: serviceReferences){
				result.add(bundleContext.getService(serviceReference));
			}
		}
		Collections.sort(result, new Comparator<PluginExtensionPointService>(){
			@Override
			public int compare(PluginExtensionPointService exp1, PluginExtensionPointService exp2) {
				return order*(exp1.getPriority()- exp2.getPriority());
			}
		});
		return result;
	}
}
