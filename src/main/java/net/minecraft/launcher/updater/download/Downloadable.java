package net.minecraft.launcher.updater.download;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Downloadable {
    public static void closeSilently(final Closeable closeable) {
        if(closeable != null)
            try {
                closeable.close();
            }
            catch(final IOException localIOException) {
            }
    }

    public static String copyAndDigest(final InputStream inputStream, final OutputStream outputStream) throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("MD5");
        final byte[] buffer = new byte[65536];
        try {
            int read = inputStream.read(buffer);
            while(read >= 1) {
                digest.update(buffer, 0, read);
                outputStream.write(buffer, 0, read);
                read = inputStream.read(buffer);
            }
        }
        finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
        }

        return String.format("%1$032x", new Object[] { new BigInteger(1, digest.digest()) });
    }

    public static String getEtag(final HttpURLConnection connection) {
        return getEtag(connection.getHeaderField("ETag"));
    }

    public static String getEtag(String etag) {
        if(etag == null)
            etag = "-";
        else if(etag.startsWith("\"") && etag.endsWith("\""))
            etag = etag.substring(1, etag.length() - 1);

        return etag;
    }

    public static String getMD5(final File file) {
        DigestInputStream stream = null;
        try {
            stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"));
            final byte[] buffer = new byte[65536];

            int read = stream.read(buffer);
            while(read >= 1)
                read = stream.read(buffer);
        }
        catch(final Exception ignored) {
            return null;
        }
        finally {
            closeSilently(stream);
        }

        return String.format("%1$032x", new Object[] { new BigInteger(1, stream.getMessageDigest().digest()) });
    }

    private final URL url;
    private final File target;

    private final boolean forceDownload;

    private final Proxy proxy;

    private final ProgressContainer monitor;

    private int numAttempts;

    private long expectedSize;

    public Downloadable(final Proxy proxy, final URL remoteFile, final File localFile, final boolean forceDownload) {
        this.proxy = proxy;
        url = remoteFile;
        target = localFile;
        this.forceDownload = forceDownload;
        monitor = new ProgressContainer();
    }

    public String download() throws IOException {
        String localMd5 = null;
        numAttempts += 1;

        if(target.getParentFile() != null && !target.getParentFile().isDirectory())
            target.getParentFile().mkdirs();
        if(!forceDownload && target.isFile())
            localMd5 = getMD5(target);

        if(target.isFile() && !target.canWrite())
            throw new RuntimeException("Do not have write permissions for " + target + " - aborting!");
        try {
            final HttpURLConnection connection = makeConnection(localMd5);
            final int status = connection.getResponseCode();

            if(status == 304)
                return "Used own copy as it matched etag";
            if(status / 100 == 2) {
                if(expectedSize == 0L)
                    monitor.setTotal(connection.getContentLength());
                else
                    monitor.setTotal(expectedSize);

                final InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), monitor);
                final FileOutputStream outputStream = new FileOutputStream(target);
                final String md5 = copyAndDigest(inputStream, outputStream);
                final String etag = getEtag(connection);

                if(etag.contains("-"))
                    return "Didn't have etag so assuming our copy is good";
                if(etag.equalsIgnoreCase(md5))
                    return "Downloaded successfully and etag matched";
                throw new RuntimeException(String.format("E-tag did not match downloaded MD5 (ETag was %s, downloaded %s)", new Object[] { etag, md5 }));
            }
            if(target.isFile())
                return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
            throw new RuntimeException("Server responded with " + status);
        }
        catch(final IOException e) {
            if(target.isFile())
                return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
            throw e;
        }
        catch(final NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing Digest.MD5", e);
        }
    }

    public long getExpectedSize() {
        return expectedSize;
    }

    public ProgressContainer getMonitor() {
        return monitor;
    }

    public int getNumAttempts() {
        return numAttempts;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public File getTarget() {
        return target;
    }

    public URL getUrl() {
        return url;
    }

    protected HttpURLConnection makeConnection(final String localMd5) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);

        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
        connection.setRequestProperty("Expires", "0");
        connection.setRequestProperty("Pragma", "no-cache");
        if(localMd5 != null)
            connection.setRequestProperty("If-None-Match", localMd5);

        connection.connect();

        return connection;
    }

    public void setExpectedSize(final long expectedSize) {
        this.expectedSize = expectedSize;
    }

    public boolean shouldIgnoreLocal() {
        return forceDownload;
    }
}