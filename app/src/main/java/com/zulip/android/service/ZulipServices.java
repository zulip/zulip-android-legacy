package com.zulip.android.service;

import com.zulip.android.networking.response.LoginResponse;
import com.zulip.android.networking.response.UserConfigurationResponse;
import com.zulip.android.networking.response.ZulipBackendResponse;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public interface ZulipServices {
//    request.setProperty("queue_id", app.getEventQueueId());
//    request.setProperty("last_event_id", "" + app.getLastEventId());
//    if (!registeredOrGotEventsThisRun) {
//        request.setProperty("dont_block", "true");
//    }
//    request.setMethodAndUrl("GET", "v1/events");

    @GET("v1/events")
    Call<String> getEvents(@Query("dont_block") Boolean dontLongPoll, @Query("last_event_id") int id, @Query("queue_id") String queueId);

    @FormUrlEncoded
    @POST("v1/register")
    Call<UserConfigurationResponse> register(@Field("apply_markdown") boolean applyMarkdown);

    @FormUrlEncoded
    @POST("v1/register")
    Call<String> registerDebug(@Field("apply_markdown") boolean applyMarkdown);

    @FormUrlEncoded
    @PUT("v1/users/me/pointer")
    Call<ResponseBody> updatePointer(@Field("pointer") String pointer);

    @POST("v1/get_auth_backends")
    Call<ZulipBackendResponse> getAuthBackends();

    @FormUrlEncoded
    @POST("v1/fetch_api_key")
    Call<LoginResponse> login(@Field("username") String username, @Field("password") String password);

    @POST("v1/dev_fetch_api_key")
    Call<LoginResponse> loginDEV(@Field("username") String username);

    class ToStringConverterFactory extends Converter.Factory {
        private static final MediaType MEDIA_TYPE = MediaType.parse("text/plain");

        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            if (String.class.equals(type)) {
                return new Converter<ResponseBody, String>() {
                    @Override public String convert(ResponseBody value) throws IOException {
                        return value.string();
                    }
                };
            }
            return null;
        }

        @Override
        public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
            if (String.class.equals(type)) {
                return new Converter<String, RequestBody>() {
                    @Override public RequestBody convert(String value) throws IOException {
                        return RequestBody.create(MEDIA_TYPE, value);
                    }
                };
            }
            return null;
        }


        //        @Override public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
//            if (String.class.equals(type)) {
//                return new Converter<String, RequestBody>() {
//                    @Override public RequestBody convert(String value) throws IOException {
//                        return RequestBody.create(MEDIA_TYPE, value);
//                    }
//                };
//            }
//            return null;
//        }
    }

}
