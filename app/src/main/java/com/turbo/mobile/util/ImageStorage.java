package com.turbo.mobile.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public class ImageStorage {

    public enum Folder {
        PROFILES, DOCUMENTS, VEHICLES, BOOKINGS
    }

    public static final String SEED_PHOTOS_PREFIX = "seed/photos/";

    private static final String CAMERA_DIR = "camera";
    private static final String IMAGES_DIR = "images";
    private static final String ASSET_PREFIX = "seed/";

    private final Context context;
    private final String authority;

    public ImageStorage(Context context) {
        this.context = context.getApplicationContext();
        this.authority = this.context.getPackageName() + ".fileprovider";
    }

    public Uri createCameraUri() throws IOException {
        File dir = new File(context.getCacheDir(), CAMERA_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        File file = File.createTempFile("IMG_", ".jpg", dir);
        return FileProvider.getUriForFile(context, authority, file);
    }

    public String save(Uri source, Folder folder) throws IOException {
        File dir = new File(new File(context.getFilesDir(), IMAGES_DIR), folder.name().toLowerCase(Locale.ROOT));
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        File target = new File(dir, UUID.randomUUID() + ".jpg");
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) {
                throw new IOException("Cannot open " + source);
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException | SecurityException e) {
            target.delete();
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        }
        if (authority.equals(source.getAuthority())) {
            context.getContentResolver().delete(source, null, null);
        }
        return target.getAbsolutePath();
    }

    public String saveAsset(String assetPath) throws IOException {
        String relative = assetPath.substring(SEED_PHOTOS_PREFIX.length());
        File target = new File(new File(context.getFilesDir(), IMAGES_DIR), relative);
        File dir = target.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return target.getAbsolutePath();
    }

    public boolean delete(@Nullable String path) {
        if (path == null) {
            return false;
        }
        File file = new File(path);
        File images = new File(context.getFilesDir(), IMAGES_DIR);
        return file.getAbsolutePath().startsWith(images.getAbsolutePath() + File.separator) && file.delete();
    }

    @Nullable
    public Bitmap load(@Nullable String path, int maxSide) {
        if (path == null) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(path)) {
            BitmapFactory.decodeStream(in, null, bounds);
        } catch (IOException | SecurityException e) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (options.inSampleSize * 2) >= maxSide) {
            options.inSampleSize *= 2;
        }
        try (InputStream in = open(path)) {
            return BitmapFactory.decodeStream(in, null, options);
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    private InputStream open(String path) throws IOException {
        if (path.startsWith(ASSET_PREFIX)) {
            return context.getAssets().open(path);
        }
        if (path.contains("://")) {
            InputStream in = context.getContentResolver().openInputStream(Uri.parse(path));
            if (in == null) {
                throw new IOException("Cannot open " + path);
            }
            return in;
        }
        return new FileInputStream(path);
    }
}
