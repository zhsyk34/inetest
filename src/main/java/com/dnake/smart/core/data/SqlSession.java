package com.dnake.smart.core.data;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class SqlSession {

	private static final JdbcTemplate jdbcTemplate;

	static {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("spring.xml");
		jdbcTemplate = ctx.getBean(JdbcTemplate.class);
	}

	public static JdbcTemplate session() {
		return jdbcTemplate;
	}

	public static DataSource source() {
		return jdbcTemplate.getDataSource();
	}
}
