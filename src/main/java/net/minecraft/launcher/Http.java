package net.minecraft.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

public class Http {
    public static String buildQuery(final Map<String, Object> query) {
        final StringBuilder builder = new StringBuilder();

        for(final Entry<String, Object> entry : query.entrySet()) {
            if(builder.length() > 0)
                builder.append('&');
            try {
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            }
            catch(final UnsupportedEncodingException e) {
                Launcher.getInstance().println("Unexpected exception building query", e);
            }

            if(entry.getValue() != null) {
                builder.append('=');
                try {
                    builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                }
                catch(final UnsupportedEncodingException e) {
                    Launcher.getInstance().println("Unexpected exception building query", e);
                }
            }
        }

        return builder.toString();
    }

    public static URL concatenateURL(final URL url, final String args) throws MalformedURLException {
        if(url.getQuery() != null && url.getQuery().length() > 0)
            return new URL(url.getProtocol(), url.getHost(), url.getFile() + "?" + args);
        return new URL(url.getProtocol(), url.getHost(), url.getFile() + "&" + args);
    }

    public static String performGet(final URL url, final Proxy proxy) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod("GET");

        final InputStream inputStream = connection.getInputStream();
        try {
            return IOUtils.toString(inputStream);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static String performPost(final URL url, final Map<String, Object> query, final Proxy proxy) throws IOException {
        return Util.performPost(url, buildQuery(query), proxy, "application/x-www-form-urlencoded", false);
    }
}