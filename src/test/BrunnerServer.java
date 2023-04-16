package com.brunner.server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.brunner.service.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import mw.launchers.RPCServer;
import mw.utility.BrunnerLogger;
import mw.utility.ExceptionUtil;
import mw.utility.StringUtil;


@Component
public class BrunnerServer {
	private final String CLASS = this.getClass().getName();
	private final Logger logger = BrunnerLogger.getLogger(CLASS);

	@Autowired
	private ApplicationContext appContext;
	
	@Resource(name = "transactionManager") 
	private DataSourceTransactionManager transactionManager;
	
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
			
	@PostConstruct
	public void init() {
		System.out.println(String.format("init bean [%s]", this.getClass().getName()));
		
		String queueName = BrunnerServer.class.getSimpleName(); 
		RPCServer server = new RPCServer() {
		   
		    protected JsonObject executeService(JsonObject request) {
		    	String txnId = request.get(Constants.msgFieldName_inputData).getAsJsonObject().get(Constants.msgFieldName_txnId).getAsString();
		    	
		    	logger.log(Level.INFO, String.format("[%s] received request: %s", txnId, gson.toJson(request)));
		    	
		    	JsonObject reply = executeBrunnerService(request);
		    	
		    	logger.log(Level.INFO, String.format("[%s] sent reply: %s", txnId, gson.toJson(reply)));	
		    	return reply;
		    }
		};
		
		try {
			server.start(queueName);
		} catch (IOException e) {
			logger.log(Level.SEVERE, ExceptionUtil.getFullMessage(e));
		} catch (TimeoutException e) {
			logger.log(Level.SEVERE, ExceptionUtil.getFullMessage(e));
		}
	}
	
	/***
	 * call referenced business service
	 *  
	 * @param request
	 * @return
	 * @throws IOException 
	 */
	JsonObject executeBrunnerService(JsonObject request) {
		JsonObject reply = null;

		SqlSession session; 
		String txnId = null;
		try {
			if(request.get(Constants.msgFieldName_inputData).getAsJsonObject().has(Constants.msgFieldName_txnId))
				txnId = request.get(Constants.msgFieldName_inputData).getAsJsonObject().get(Constants.msgFieldName_txnId).getAsString();
			else {
				txnId = StringUtil.getTxnId();
				request.get(Constants.msgFieldName_inputData).getAsJsonObject().addProperty(Constants.msgFieldName_txnId, txnId);
			}
			
			session = BrunnerSqlSession.getInstance();
		} catch (IOException e1) {
			reply = new JsonObject();
			reply.addProperty(Constants.msgFieldName_txnId, txnId);
			reply.addProperty(Constants.msgFieldName_resultCode, Constants.resultCode_systemException);
			reply.addProperty(Constants.msgFieldName_resultMessage, ExceptionUtil.getFullMessage(e1));
			
			return reply;
		}

		if(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp")) {
			// Debuging time call
		}
		else {
			// Run time call
		}
		
		DefaultTransactionDefinition td = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED); 
		td.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		int timeoutS = request.has("requestTimeoutMS") && request.get("requestTimeoutMS").getAsInt() > 0 ?  request.get("requestTimeoutMS").getAsInt() / 1000 : 60;
		td.setTimeout(timeoutS); 
		TransactionStatus status = transactionManager.getTransaction(td);

		Class<?> cls = null;
		Object obj = null;
		Method method = null;
		
		try {
			String className = request.get("className").getAsString();
			String methodName = request.get("methodName").getAsString();

			obj = appContext.getBean(className);
			cls = obj.getClass();
			method = cls.getDeclaredMethod(methodName, SqlSession.class, JsonObject.class);
			method.setAccessible(true);
			reply = (JsonObject) method.invoke(obj, session, request);		
			transactionManager.commit(status); 
		} catch (Exception e) {
			transactionManager.rollback(status);

			reply = new JsonObject();
			reply.addProperty(Constants.msgFieldName_txnId, txnId);
			reply.addProperty(Constants.msgFieldName_resultCode, Constants.resultCode_systemException);
			reply.addProperty(Constants.msgFieldName_resultMessage, ExceptionUtil.getFullMessage(e));
		}		
    	return reply;		
	}
}
