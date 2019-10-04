package fi.tkgwf.zri.bean.zabbix;

import com.google.gson.annotations.Expose;

public class Item {

    @Expose
    public String itemid;
    @Expose
    public String type;
    @Expose
    public String hostid;
    @Expose
    public String name;
    @Expose
    public String key_;
}
