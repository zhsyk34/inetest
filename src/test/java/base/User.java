package base;

import lombok.ToString;

import java.util.Date;

@ToString
public final class User {
	private String name;
	private Date date;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public static void main(String[] args) {
		User user = new User();
		user.setName("a");
		user.setDate(new Date());
		System.out.println(user);
		user.setName("b");
		System.out.println(user);
	}
}
