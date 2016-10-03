package com.labs.dm.auto_tethering.provider;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Cellular;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Daniel Mroczka on 10/3/2016.
 */
public class GoogleGeoLocationProvider {
    private String url = "https://www.googleapis.com/geolocation/v1/geolocate?key=" + BuildConfig.GOOGLE_API_KEY;

    public void send(Cellular item) {
        try {
            JSONObject json;
            int TIMEOUT_MILLISEC = 10000;

//            JSONObject j = new JSONObject();
//            j.put("cellId", item.getCid());
//            j.put("locationAreaCode", item.getLac());
//            j.put("mobileCountryCode", item.getMcc());
//            j.put("mobileNetworkCode", item.getMnc());
//
//            JSONObject root = new JSONObject(j);

            String postMessage = "{\n" +
                    "  \"cellTowers\": [\n" +
                    "    {\n" +
                    "      \"cellId\": " + item.getCid() + " ,\n" +
                    "      \"locationAreaCode\": " + item.getLac() + ",\n" +
                    "      \"mobileCountryCode\": " + item.getMcc() + " ,\n" +
                    "      \"mobileNetworkCode\": " + +item.getMnc() + " \n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
            HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
            HttpClient client = new DefaultHttpClient(httpParams);

            HttpPost request = new HttpPost(url);
            request.setEntity(new ByteArrayEntity(postMessage.toString().getBytes("UTF8")));

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
}
