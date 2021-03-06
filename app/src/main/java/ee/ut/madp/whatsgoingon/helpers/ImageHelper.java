package ee.ut.madp.whatsgoingon.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper for working with images, mainly converting between bitmap and base64 formats.
 */
public class ImageHelper {

    private static final String TAG = ImageHelper.class.getSimpleName();

    /**
     * Encodes the given bitmap to String
     * @param bitmap
     * @return
     */
    public static String encodeBitmap(Bitmap bitmap) {
        Log.i(TAG, "encodeBitmap");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    /**
     * Decodes the given String to the bitmap
     * @param encodedImage
     * @return
     */
    public static Bitmap decodeBitmap(String encodedImage) {
        Log.i(TAG, "decodeBitmap");
        Bitmap imageDecoded = null;
        if (encodedImage != null && !encodedImage.isEmpty()) {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            imageDecoded = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }

        return imageDecoded;
    }

    public static Bitmap getScaledBitmap(Context context, String path) {
        Log.i(TAG, "getScaledBitmap: " + path);
        Uri uri = Uri.fromFile(new File(path));
        InputStream in;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = context.getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            assert in != null;
            in.close();

            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }

            Bitmap b;
            in = context.getContentResolver().openInputStream(uri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            assert in != null;
            in.close();
            return b;
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap resize(Bitmap bitmap) {
        float maxImageSize = 250;
        float ratio = Math.min(
                maxImageSize / bitmap.getWidth(),
                maxImageSize / bitmap.getHeight());
        int width = Math.round(ratio *  bitmap.getWidth());
        int height = Math.round(ratio * bitmap.getHeight());
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }
}
