package com.dnake.smart.core.data;

import java.util.List;

public interface CommonDao {

	int insert(String sql);

	int insert(String sql, Object... parameters);

	int update(String sql);

	int update(String sql, Object... parameters);

	int delete(String sql);

	int delete(String sql, Object... parameters);

	<T> T selectOne(String sql, Class<T> clazz);

	<T> T selectOne(String sql, Class<T> clazz, Object... parameters);

	<T> List<T> selectList(String sql, Class<T> clazz);

	<T> List<T> selectList(String sql, Class<T> clazz, Object... parameters);
}
