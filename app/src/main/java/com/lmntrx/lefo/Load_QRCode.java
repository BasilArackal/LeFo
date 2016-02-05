package com.lmntrx.lefo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/*
 * Created by livin on 11/1/16.
 */
public class Load_QRCode extends AsyncTask<Void, Void, Bitmap> {

    private String url;            //URL from which qr code is obtained
    private ImageView imageView;   //ImageView in which qr Code is displayed

    public Load_QRCode(String url, ImageView imageView) {
        this.url = url;
        this.imageView = imageView;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        try {
            URL urlConnection = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);
        imageView.setImageBitmap(result);
        if (result == null) {    //if  the result from server is null
            Bitmap errorImage = BitmapFactory.decodeResource(Lead.CON.getResources(), R.drawable.error);
            imageView.setImageBitmap(errorImage);
        }
    }
}
