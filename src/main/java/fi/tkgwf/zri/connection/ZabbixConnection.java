package fi.tkgwf.zri.connection;

import com.google.gson.Gson;
import fi.tkgwf.zri.bean.zabbix.ApiRequest;
import fi.tkgwf.zri.bean.zabbix.AuthResponse;
import fi.tkgwf.zri.bean.zabbix.Host;
import fi.tkgwf.zri.bean.zabbix.HostResponse;
import fi.tkgwf.zri.bean.zabbix.Item;
import fi.tkgwf.zri.bean.zabbix.ItemResponse;
import fi.tkgwf.zri.config.Config;
import fi.tkgwf.zri.utils.ExpirableMap;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZabbixConnection {

    private static final Logger LOG = LogManager.getLogger();

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json-rpc");
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000;
    private static final long CACHE_NULL_DURATION_MS = 30 * 60 * 1000;

    private final String apiUrl = Config.get("zabbix.url") + "/api_jsonrpc.php";
    private final OkHttpClient client = new OkHttpClient();
    private final ExpirableMap<String, Item> itemCache = new ExpirableMap<>();
    private final ExpirableMap<String, Host> hostCache = new ExpirableMap<>();

    private String auth;

    private void login() throws IOException {
        ApiRequest request = new ApiRequest();
        request.auth = null;
        request.method = "user.login";
        request.params.put("user", Config.get("zabbix.user"));
        request.params.put("password", Config.get("zabbix.password"));

        ResponseBody body = client.newCall(createRequest(request)).execute().body();
        if (body == null) {
            throw new IllegalStateException("Null response body from login");
        }
        auth = new Gson().fromJson(body.charStream(), AuthResponse.class).result;
    }

    public Item getItem(String itemId) throws IOException {
        Item cached = itemCache.get(itemId);
        if (itemCache.containsKey(itemId)) {
            return cached;
        }
        if (auth == null) {
            login();
        }
        ApiRequest request = new ApiRequest();
        request.auth = auth;
        request.method = "item.get";
        request.params.put("itemids", itemId);

        ResponseBody body = client.newCall(createRequest(request)).execute().body();
        if (body == null) {
            throw new IllegalStateException("Null response body from item.get with id " + itemId);
        }
        List<Item> result = new Gson().fromJson(body.charStream(), ItemResponse.class).result;
        if (CollectionUtils.isEmpty(result)) {
            itemCache.put(itemId, null, System.currentTimeMillis() + CACHE_NULL_DURATION_MS);
            return null;
        } else {
            Item item = result.get(0);
            itemCache.put(itemId, item, System.currentTimeMillis() + CACHE_DURATION_MS);
            return item;
        }
    }

    public Host getHost(String hostId) throws IOException {
        Host cached = hostCache.get(hostId);
        if (cached != null) {
            return cached;
        }
        if (auth == null) {
            login();
        }
        ApiRequest request = new ApiRequest();
        request.auth = auth;
        request.method = "host.get";
        request.params.put("hostids", hostId);

        ResponseBody body = client.newCall(createRequest(request)).execute().body();
        if (body == null) {
            throw new IllegalStateException("Null response body from host.get with id " + hostId);
        }
        List<Host> result = new Gson().fromJson(body.charStream(), HostResponse.class).result;
        if (CollectionUtils.isEmpty(result)) {
            return null;
        } else {
            Host item = result.get(0);
            hostCache.put(hostId, item, System.currentTimeMillis() + CACHE_DURATION_MS);
            return item;
        }
    }

    private Request createRequest(ApiRequest apiRequest) {
        return new Request.Builder().url(apiUrl).addHeader("Content-Type", "application/json-rpc").addHeader("cache-control", "no-cache").post(RequestBody.create(new Gson().toJson(apiRequest), MEDIA_TYPE)).build();
    }
}
