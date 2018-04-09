package com.edsapp.weather.network;

import android.content.Context;
import android.support.annotation.NonNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 *  Purely for Testing a mocking the backend.
 */
public class FakeInterceptor implements Interceptor {
    private final Context context;
    private boolean faked = false;

    public FakeInterceptor(Context context) {
        super();
        this.context = context;
    }

    public FakeInterceptor(Context context, boolean faked) {
        super();
        this.context = context;
        this.faked = faked;
    }

    void setFaked(boolean faked) {
        this.faked = faked;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response;

        if(faked) { //
            StringBuilder buf=new StringBuilder();
            InputStream json=context.getAssets().open("fake.json");
            BufferedReader in=
                    new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String responseString;

            while ((responseString=in.readLine()) != null) {
                buf.append(responseString);
            }

            in.close();

            response = new Response.Builder()
                    .code(200)
                    .message(buf.toString())
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_0)
                    .body(ResponseBody.create(MediaType.parse("application/json"), buf.toString().getBytes()))
                    .addHeader("content-type", "application/json")
                    .build();
        } else {
            response = chain.proceed(chain.request());
        }

        return response;
    }
}