package com.labs.dm.auto_tethering.provider;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Cellular;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Daniel Mroczka on 10/3/2016.
 */
public class OpenCellIDProvider {

    private final String FORMAT_URL = "http://opencellid.org/cell/get?key=%s&mcc=%d&mnc=%d&lac=%d&cellid=%d&format=json";

    public void loadLocationFromService(Cellular item) {
        JSONObject json;
        String url = String.format(FORMAT_URL, BuildConfig.OPENCELLID_KEY, item.getMcc(), item.getMnc(), item.getLac(), item.getCid());

        if (!item.hasLocation()) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response;
            try {
                response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = entity.getContent();
                    String result = Utils.convertStreamToString(inputStream);
                    MyLog.d("Load JSON", result);
                    json = new JSONObject(result);
                    item.setLon(json.getDouble("lon"));
                    item.setLat(json.getDouble("lat"));
                    inputStream.close();
                }
            } catch (IOException e) {
                MyLog.e("HttpRequest", e.getMessage());
            } catch (JSONException e) {
                MyLog.e("JSONException", e.getMessage());
            }
        }
    }
}
