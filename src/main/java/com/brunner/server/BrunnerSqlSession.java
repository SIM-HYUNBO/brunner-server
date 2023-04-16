package com.brunner.server;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class BrunnerSqlSession {

	static SqlSessionFactory sqlSessionFactory = null;
	static SqlSession sqlSession = null;
	
	public static SqlSession getInstance() throws IOException {
		if(sqlSessionFactory == null) {
			String resource = "mybatis-config.xml";
			InputStream inputStream;
			
			try {
				 inputStream = Resources.getResourceAsStream(resource);
			} catch (IOException e) {
				throw e;
			}
			
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
			
			// set auto commit true 
			sqlSession = sqlSessionFactory.openSession(true);
			sqlSession.clearCache();
		}
		return sqlSession;
	}
}
