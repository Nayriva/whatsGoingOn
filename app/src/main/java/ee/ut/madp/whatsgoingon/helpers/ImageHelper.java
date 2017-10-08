package ee.ut.madp.whatsgoingon.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageHelper {

    /**
     * Encodes the given bitmap to String
     * @param bitmap
     * @return
     */
    public static String encodeBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        return imageEncoded;
    }

    /**
     * Decodes the given String to the bitmap
     * @param encodedImage
     * @return
     */
    public static Bitmap decodeBitmap(String encodedImage) {
        Bitmap imageDecoded = null;
        if (encodedImage != null && !encodedImage.isEmpty()) {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            imageDecoded = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }

        return imageDecoded;
    }
}
