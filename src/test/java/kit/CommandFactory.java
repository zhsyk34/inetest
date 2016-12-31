package kit;

import com.alibaba.fastjson.JSONObject;

public class CommandFactory {

	public static JSONObject answer(String s) {
		JSONObject json = new JSONObject();
		json.put("result", "ok");
		json.put("desc", "网关应答" + s);
		return json;
	}

	public static JSONObject ask(String sn) {
		JSONObject json = new JSONObject();
		json.put("action", "test");
		json.put("desc", "app请求==>" + sn);
		return json;
	}

	public static JSONObject login(int type, String sn, int port) {
		JSONObject json = new JSONObject();
		json.put("action", "loginReq");
		json.put("clientType", type);
		json.put("devSN", sn);
		json.put("UDPPort", port);
		return json;
	}

	public static JSONObject verify(String key) {
		JSONObject json = new JSONObject();
		json.put("keyCode", key);
		json.put("result", "ok");
		return json;
	}

}
