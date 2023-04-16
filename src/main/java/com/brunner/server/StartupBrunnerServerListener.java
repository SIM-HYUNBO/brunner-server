package com.brunner.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import mw.utility.BrunnerLogger;

public class StartupBrunnerServerListener implements ApplicationListener<ContextRefreshedEvent> {
	private final String CLASS = this.getClass().getName();
	private final Logger logger = BrunnerLogger.getLogger(CLASS);
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.log(Level.FINE, "starting brunner server ...");
	}
}
