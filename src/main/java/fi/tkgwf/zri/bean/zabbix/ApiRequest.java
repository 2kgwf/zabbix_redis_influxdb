package fi.tkgwf.zri.bean.zabbix;

import com.google.gson.annotations.Expose;
import java.util.HashMap;
import java.util.Map;

public class ApiRequest {

    @Expose
    public String jsonrpc = "2.0";
    @Expose
    public Integer id = 1;
    @Expose
    public String method;
    @Expose
    public Map<String, Object> params = new HashMap<>();
    @Expose
    public Object auth;
}
