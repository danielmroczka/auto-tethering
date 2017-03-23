package com.labs.dm.auto_tethering.provider;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Cellular;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Daniel Mroczka on 03/10/2016.
 */
public class GoogleGeoLocationProvider {

    private final String url = "https://www.googleapis.com/geolocation/v1/geolocate?key=" + BuildConfig.GOOGLE_API_KEY;

    public void loadLocationFromService(Cellular item) {
        JSONObject json;
        int TIMEOUT = 5000;

        try {
            JSONObject root = buildJSON(item);

            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT);
            HttpClient client = new DefaultHttpClient(httpParams);

            StringEntity params = new StringEntity(root.toString());
            HttpPost request = new HttpPost(url);
            request.setEntity(params);
            request.addHeader("content-type", "application/json");
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                String result = Utils.convertStreamToString(inputStream);
                MyLog.d("Load JSON", result);
                json = new JSONObject(result);
                item.setLon(json.getJSONObject("location").getDouble("lng"));
                item.setLat(json.getJSONObject("location").getDouble("lat"));
                inputStream.close();
            }
        } catch (IOException e) {
            MyLog.e("HttpRequest", e.getMessage());
        } catch (JSONException e) {
            MyLog.e("JSONException", e.getMessage());
        }
    }

    private JSONObject buildJSON(Cellular item) throws JSONException {
        JSONObject cell = new JSONObject();
        JSONArray root2 = new JSONArray();
        cell.put("cellId", item.getCid());
        cell.put("locationAreaCode", item.getLac());
        cell.put("mobileCountryCode", item.getMcc());
        cell.put("mobileNetworkCode", item.getMnc());
        root2.put(cell);
        JSONObject root = new JSONObject();
        root.put("cellTowers", root2);
        return root;
    }
}
