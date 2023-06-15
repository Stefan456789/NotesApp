package me.stefan.notes.backend;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConnectionHandler{


    public static void get(String url, Consumer<String> resultConsumer) {
        ConnectionTask connectionTask = new ConnectionTask(resultConsumer);
        connectionTask.execute(url, "GET");

    }

    public static void post(String url, String jsonData, Consumer<String> resultConsumer) {
        ConnectionTask connectionTask = new ConnectionTask(resultConsumer);
        connectionTask.execute(url, "POST", jsonData);
    }

    public static void put(String url, String json, Consumer<String> resultConsumer) {
        ConnectionTask connectionTask = new ConnectionTask(resultConsumer);
        connectionTask.execute(url, "PUT", json);
    }

    public static void delete(String url, Consumer<String> resultConsumer) {
        ConnectionTask connectionTask = new ConnectionTask(resultConsumer);
        connectionTask.execute(url, "DELETE");
    }


    private static class ConnectionTask extends AsyncTask<String, Void, String> {
        private final Consumer<String> resultConsumer;

        public ConnectionTask(Consumer<String> resultConsumer) {
            this.resultConsumer = resultConsumer;
        }

        @Override
        protected String doInBackground(String... strings) {
            String sJson = "";
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(strings[0]).openConnection();
            } catch (IOException e) {
                Log.e("Internet", e.getMessage());
                return null;
            }
            try {
                connection.setRequestMethod(strings[1]);
                connection.setRequestProperty("Content-Type", "application/json");
                if (strings.length == 3){
                    connection.setDoOutput(true);
                    byte[] data = strings[2].getBytes();
                    connection.setFixedLengthStreamingMode(data.length);
                    connection.getOutputStream().write(data);
                    connection.getOutputStream().flush();
                    connection.getOutputStream().close();
                }

                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));

                    StringBuilder stringBuilder = new StringBuilder() ;
                    String line = "";
                    while ( ( line =reader.readLine()) != null ) {
                        stringBuilder .append(line) ;
                    }
                    sJson = stringBuilder.toString();
                } else {
                    Log.e("Internet", "Error, Response Code: " + responseCode);
                    return null;
                }
            } catch (IOException e) {
                Log.e("Internet", e.getMessage());
                return null;
            }
            return sJson;
        }

        @Override
        protected void onPostExecute(String s) {
            if (resultConsumer != null)
                resultConsumer.accept(s);
        }
    }
}
