package com.utar.ucycle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Cloud Storage for Firebase now requires a billing account, so listing photos
 * are compressed and stored as a Base64 string on the listing document itself.
 *
 * A Firestore document is capped at 1 MB, so images are scaled down and
 * compressed until the encoded string comfortably fits.
 */
public final class ImageUtils {

    private ImageUtils() { }

    /** Longest edge of the stored image, in pixels. */
    private static final int MAX_DIMENSION = 800;

    /** Stay well under Firestore's 1 MB document limit. */
    private static final int MAX_ENCODED_BYTES = 350_000;

    private static final int MIN_QUALITY = 40;

    /**
     * Reads the picked image, downscales it, fixes its rotation and returns a
     * Base64 string ready to be written to Firestore.
     *
     * @return the encoded image, or null if the image could not be read.
     */
    public static String encodeForFirestore(Context context, Uri uri) {
        try {
            Bitmap bitmap = decodeScaled(context, uri);
            if (bitmap == null) return null;

            bitmap = fixRotation(context, uri, bitmap);

            int quality = 80;
            byte[] bytes = compress(bitmap, quality);

            // Shrink further if the encoded result is still too large.
            while (bytes.length > MAX_ENCODED_BYTES && quality > MIN_QUALITY) {
                quality -= 15;
                bytes = compress(bitmap, quality);
            }

            if (bytes.length > MAX_ENCODED_BYTES) {
                // Last resort: halve the dimensions and compress again.
                Bitmap smaller = Bitmap.createScaledBitmap(
                        bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
                bytes = compress(smaller, MIN_QUALITY);
                smaller.recycle();
            }

            bitmap.recycle();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** Turns a stored Base64 string back into a bitmap for display. */
    public static Bitmap decode(String encoded) {
        if (TextUtils.isEmpty(encoded)) return null;
        try {
            byte[] bytes = Base64.decode(encoded, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** Shows the photo if there is one, otherwise hides the ImageView entirely. */
    public static void loadIntoOrHide(android.widget.ImageView view, String encoded) {
        Bitmap bitmap = decode(encoded);
        if (bitmap != null) {
            view.setVisibility(android.view.View.VISIBLE);
            view.setImageBitmap(bitmap);
        } else {
            view.setVisibility(android.view.View.GONE);
        }
    }

    private static byte[] compress(Bitmap bitmap, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return out.toByteArray();
    }

    /** Loads the image already downsampled, so a large photo never fills memory. */
    private static Bitmap decodeScaled(Context context, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }

        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        int sample = 1;
        while (longest / sample > MAX_DIMENSION * 2) {
            sample *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;

        Bitmap decoded;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            decoded = BitmapFactory.decodeStream(in, null, options);
        }
        if (decoded == null) return null;

        // Scale the longest edge down to MAX_DIMENSION.
        int w = decoded.getWidth();
        int h = decoded.getHeight();
        int longestNow = Math.max(w, h);
        if (longestNow > MAX_DIMENSION) {
            float ratio = (float) MAX_DIMENSION / longestNow;
            Bitmap scaled = Bitmap.createScaledBitmap(
                    decoded, Math.round(w * ratio), Math.round(h * ratio), true);
            if (scaled != decoded) decoded.recycle();
            return scaled;
        }
        return decoded;
    }

    /** Photos taken in portrait often carry rotation only in their EXIF data. */
    private static Bitmap fixRotation(Context context, Uri uri, Bitmap bitmap) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return bitmap;
            ExifInterface exif = new ExifInterface(in);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int degrees;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  degrees = 90;  break;
                case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
                default: return bitmap;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (Exception e) {
            return bitmap;
        }
    }
}
