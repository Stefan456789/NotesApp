package me.stefan.notes;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class ConnectionHandler{


    public static void get(String url, Consumer<String> resultConsumer) {
        ConnectionTask connectionTask = new ConnectionTask(resultConsumer);
        connectionTask.execute(url, "GET");


    }

    public static void post(String url, String jsonData, Consumer<String> resultConsumer) {
        ConnectionTask connectionTask = new ConnectionTask(resultConsumer);
        connectionTask.execute(url, "POST", jsonData);
    }


    private static class ConnectionTask extends AsyncTask<String, Void, String> {
        private final Consumer<String> resultConsumer;

        public ConnectionTask(Consumer<String> resultConsumer) {
            this.resultConsumer = resultConsumer;
        }

        @Override
        protected String doInBackground(String... strings) {
            String username = strings [0];
            String sJson = "";
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(strings[0]).openConnection();
                connection.setRequestMethod(strings[1]);
                connection.setRequestProperty("Content=Type", "application/json");

                if (strings.length == 3){
                    connection.setDoOutput(true);
                    byte[] data = strings[2].getBytes();
                    connection.setFixedLengthStreamingMode(data.length);
                    connection.getOutputStream().write(data);
                    connection.getOutputStream().flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));

                    StringBuilder stringBuilder = new StringBuilder() ;
                    String line = "";
                    while ( ( line =reader.readLine()) != null ) {
                        stringBuilder .append(line) ;
                    }
                    sJson = stringBuilder.toString();
                }
            } catch (IOException e) {
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
