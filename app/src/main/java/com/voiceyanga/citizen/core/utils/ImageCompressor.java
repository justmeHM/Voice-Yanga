package com.voiceyanga.citizen.core.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Utility to compress images locally before upload.
 * Reduces data usage and improves success rate on weak networks.
 */
public class ImageCompressor {

    public static File compress(Context context, String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            InputStream input = context.getContentResolver().openInputStream(uri);
            
            // Decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            int width = options.outWidth;
            int height = options.outHeight;
            int reqWidth = 1200;
            int reqHeight = 1200;

            int inSampleSize = 1;
            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            // Decode with inSampleSize
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            
            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            input.close();

            if (bitmap == null) return null;

            File compressedFile = new File(context.getCacheDir(), "comp_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
            bitmap.recycle();

            return compressedFile;
        } catch (Exception e) {
            android.util.Log.e("ImageCompressor", "Compression failed", e);
            return null;
        }
    }
}
