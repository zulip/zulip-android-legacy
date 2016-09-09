package com.zulip.android.service;

import com.zulip.android.filters.NarrowFilter;
import com.zulip.android.networking.response.GetMessagesResponse;
import com.zulip.android.networking.response.LoginResponse;
import com.zulip.android.networking.response.UserConfigurationResponse;
import com.zulip.android.networking.response.ZulipBackendResponse;
import com.zulip.android.networking.response.events.GetEventResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;


public interface ZulipServices {

    @GET("v1/messages?apply_markdown=true")

    Call<GetMessagesResponse> getMessages(@Query("anchor") String anchor,
                                          @Query("num_before") String numBefore,
                                          @Query("num_after") String numAfter,
                                          @Query("narrow")NarrowFilter narrowFilter);

    @GET("v1/events")
    Call<GetEventResponse> getEvents(@Query("dont_block") Boolean dontLongPoll, @Query("last_event_id") int id, @Query("queue_id") String queueId);

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

}
