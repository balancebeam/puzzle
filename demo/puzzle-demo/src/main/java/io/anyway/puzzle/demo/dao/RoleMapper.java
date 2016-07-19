package io.anyway.puzzle.demo.dao;

import io.anyway.puzzle.demo.domain.Role;

public interface RoleMapper {

	int deleteByPrimaryKey(String userId);

    int insert(Role record);

    int insertSelective(Role record);

    Role selectByPrimaryKey(String userId);

    int updateByPrimaryKeySelective(Role record);

    int updateByPrimaryKey(Role record);
}
