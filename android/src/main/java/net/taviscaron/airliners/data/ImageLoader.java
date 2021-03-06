package net.taviscaron.airliners.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import net.taviscaron.airliners.util.IOUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Image loader (w/ cache support)
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Object lock = new Object();
    private static final Set<String> loadedUrls = new HashSet<String>();

    public static final String THUMB_CACHE_TAG = "thumb";
    public static final String IMAGE_CACHE_TAG = "image";

    private final File cacheBaseDir;
    private final Handler handler;
    private final URLStreamHandler urlStreamHandler;

    public interface ImageLoaderCallback {
        public void imageLoadStarted(ImageLoader loader, String url);
        public void imageLoaded(ImageLoader loader, String url, Bitmap bitmap, String imageCachePath);
        public void imageLoadFailed(ImageLoader loader, String url);
    }

    public ImageLoader(Context context, String cacheTag) {
        this(context, cacheTag, null);
    }

    public ImageLoader(Context context, String cacheTag, URLStreamHandler urlStreamHandler) {
        this.cacheBaseDir = IOUtil.getExternalCacheDir(context, cacheTag);
        this.handler = new Handler(Looper.getMainLooper());
        this.urlStreamHandler = urlStreamHandler;
    }

    public void loadImage(String url, ImageLoaderCallback callback) {
        callbackLoadStarted(callback, url);
        executor.execute(new Loader(url, callback));
    }

    private String filenameFromUrl(String url) {
        int startIndex = url.lastIndexOf('/') + 1;
        int endIndex = url.indexOf('?');

        if(endIndex == -1) {
            endIndex = url.length();
        }

        if(endIndex <= startIndex) {
            throw new IllegalArgumentException("Strange url: " + url);
        }

        return url.substring(startIndex, endIndex);
    }

    private class Loader implements Runnable {
        private String url;
        private ImageLoaderCallback callback;

        private Loader(String url, ImageLoaderCallback callback) {
            this.callback = callback;
            this.url = url;
        }

        public void run() {
            // TODO: needs to lock on concrete url
            synchronized (lock) {
                try {
                    while(!loadedUrls.add(url)) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    // noop
                }
            }

            String filename = filenameFromUrl(url);
            File bitmapFile = new File(cacheBaseDir, filename);

            if(!bitmapFile.exists()) {
                InputStream is = null;
                OutputStream os = null;
                HttpURLConnection connection = null;
                try {
                    URL connectionUrl = new URL(null, url, urlStreamHandler);
                    connection = (HttpURLConnection)connectionUrl.openConnection();

                    if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        is = connection.getInputStream();
                        os = new FileOutputStream(bitmapFile);
                        IOUtil.copy(is, os);
                    }
                } catch (MalformedURLException e) {
                    Log.w(TAG, "Bad url: " + url, e);
                } catch (IOException e) {
                    Log.w(TAG, "IO was bad: " + url, e);
                } finally {
                    IOUtil.disconnect(connection);
                    IOUtil.close(is);
                    IOUtil.close(os);
                }
            }

            synchronized (lock) {
                loadedUrls.remove(url);
                lock.notifyAll();
            }

            Bitmap result = (bitmapFile.exists()) ? BitmapFactory.decodeFile(bitmapFile.getAbsolutePath()) : null;
            callbackLoadFinished(callback, url, result, bitmapFile.getAbsolutePath());
        }
    }

    protected void callbackLoadStarted(final ImageLoaderCallback callback, final String url) {
        handler.post(new Runnable() {
            public void run() {
                callback.imageLoadStarted(ImageLoader.this, url);
            }
        });
    }

    protected void callbackLoadFinished(final ImageLoaderCallback callback, final String url, final Bitmap result, final String imageCachePath) {
        handler.post(new Runnable() {
            public void run() {
                if(result != null) {
                    callback.imageLoaded(ImageLoader.this, url, result, imageCachePath);
                } else {
                    callback.imageLoadFailed(ImageLoader.this, url);
                }
            }
        });
    }
}
