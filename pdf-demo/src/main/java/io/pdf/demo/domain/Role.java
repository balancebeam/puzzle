package io.pdf.demo.domain;

import java.util.Date;

public class Role {

	private String roleId;

    private String roleName;

    private Date roleBirthday;

    private Double roleSalary;

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public Date getRoleBirthday() {
		return roleBirthday;
	}

	public void setRoleBirthday(Date roleBirthday) {
		this.roleBirthday = roleBirthday;
	}

	public Double getRoleSalary() {
		return roleSalary;
	}

	public void setRoleSalary(Double roleSalary) {
		this.roleSalary = roleSalary;
	}
}
