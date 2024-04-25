package com.example.simplechatclient;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

public class WebServiceModel extends AbstractModel {

    private static final String TAG = "WebServiceModel";

    private static final String GET_URL = "https://testbed.jaysnellen.com:8443/SimpleChat/board";
    private static final String POST_URL = "https://testbed.jaysnellen.com:8443/SimpleChat/board";
    private static final String DELETE_URL = "https://testbed.jaysnellen.com:8443/SimpleChat/board";

    public static final String NAME = "Suni";

    private MutableLiveData<JSONObject> jsonData;
    private String outputText, postData;

    private final ExecutorService requestThreadExecutor;
    private final Runnable httpGetRequestThread, httpPostRequestThread, httpDeleteRequestThread;
    private Future<?> pending;


    public WebServiceModel() {
        requestThreadExecutor = Executors.newSingleThreadExecutor();

        httpGetRequestThread = () -> {
            if (pending != null) {
                pending.cancel(true);
            }
            try {
                pending = requestThreadExecutor.submit(new HTTPRequestTask("GET", GET_URL, null));
            } catch (Exception e) {
                Log.e(TAG, " Exception: ", e);
            }
        };
        httpPostRequestThread = () -> {
            if (pending != null) { pending.cancel(true); }
            try {
                pending = requestThreadExecutor.submit(new HTTPRequestTask("POST", POST_URL, postData));
            } catch (Exception e) { Log.e(TAG, " Exception: ", e); }
        };
        httpDeleteRequestThread = () -> {
            if (pending != null) { pending.cancel(true); }
            try {
                pending = requestThreadExecutor.submit(new HTTPRequestTask("DELETE", DELETE_URL, null));
            } catch (Exception e) {
                Log.e(TAG, " Exception: ", e);
            }
        };
    }

    public void initDefault() {
        sendGetRequest();
    }

    public void setOutputText(String newText) {

        String oldText = this.outputText;
        this.outputText = newText;
        try {
            JSONObject jsonObject = new JSONObject(newText);
            Map<String, String> messageList = new HashMap<>();

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonObject.getString(key);
                messageList.put(key, value);
            }

            StringBuilder b = new StringBuilder();
            for (Map.Entry<String, String> entry : messageList.entrySet()) {
                String value = entry.getValue();
                b.append(value).append("\n");
            }

            this.outputText = b.toString();

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }
        Log.i(TAG, "Output Text Change: From " + oldText + " to " + newText);
        firePropertyChange(DefaultController.ELEMENT_OUTPUT_PROPERTY, oldText, this.outputText);
    }

    public void sendGetRequest() {
        httpGetRequestThread.run();
    }

    public void sendPostRequest(String jsonData) {
        this.postData = String.valueOf(jsonData);
        httpPostRequestThread.run();
    }

    public void sendDeleteRequest() {
        httpDeleteRequestThread.run();
    }

    private void setJsonData(JSONObject json) {
        this.getJsonData().postValue(json);
        setOutputText(json.toString());
    }

    public MutableLiveData<JSONObject> getJsonData() {
        if (jsonData == null) {
            jsonData = new MutableLiveData<>();
        }
        return jsonData;
    }

    private class HTTPRequestTask implements Runnable {

        private final String method, urlString, jsonData;

        HTTPRequestTask(String method, String urlString, String jsonData) {
            this.method = method;
            this.urlString = urlString;
            this.jsonData = jsonData;
        }

        @Override
        public void run() {
            JSONObject results = doRequest(urlString, jsonData);
            setJsonData(results);
        }

        private JSONObject doRequest(String urlString, String jsonData) {
            StringBuilder r = new StringBuilder();
            String line;
            HttpURLConnection conn = null;
            JSONObject results = null;

            try {
                if (Thread.interrupted()) throw new InterruptedException();

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);

                conn.setRequestMethod(method);
                conn.setDoInput(true);

                if (method.equals("POST")) {
                    conn.setDoOutput(true);

                    OutputStream out = conn.getOutputStream();
                    out.write(jsonData.getBytes());
                    out.flush();
                    out.close();
                }

                conn.connect();

                int code = conn.getResponseCode();

                // Handle DELETE
                if (method.equals("DELETE")) {
                    if (code == HttpURLConnection.HTTP_OK) {
                        Log.d(TAG, "Delete success");
                        setOutputText("Message Board Cleared");
                    } else {
                        Log.e(TAG, "Delete failed");
                    }
                } else {
                    if (code == HttpsURLConnection.HTTP_OK || code == HttpsURLConnection.HTTP_CREATED) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                        do {
                            line = reader.readLine();
                            if (line != null) r.append(line);
                        } while (line != null);
                    }

                    results = new JSONObject(r.toString());
                }

            } catch (Exception e) {
                Log.e(TAG, " Exception: ", e);
            } finally {
                if (conn != null) { conn.disconnect(); }
            }

            Log.d(TAG, " JSON: " + r);
            return results;
        }

    }


}