package io.anyway.puzzle.core.event;

import java.util.LinkedList;
import java.util.List;

public interface PluginFrameworkListener {

	final public static class Processor{
		
		private static List<PluginFrameworkListener> pluginFrameworkListeners= new LinkedList<PluginFrameworkListener>();
		
		private static boolean finished= false;
		
		public static synchronized void addListener(PluginFrameworkListener listener){
			pluginFrameworkListeners.add(listener);
		}
		
		public static synchronized void execute(boolean done){
			if(finished || done && (finished= true)){
				for(PluginFrameworkListener each: pluginFrameworkListeners){
					each.onFinish();
				}
				pluginFrameworkListeners.clear();
			}
		}
		
	}
	/**
	 * OSGi 框架启动结束后
	 */
	void onFinish();
}
