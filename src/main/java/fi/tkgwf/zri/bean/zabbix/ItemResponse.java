package fi.tkgwf.zri.bean.zabbix;

import com.google.gson.annotations.Expose;
import java.util.List;

public class ItemResponse {

    @Expose
    public List<Item> result;
}
