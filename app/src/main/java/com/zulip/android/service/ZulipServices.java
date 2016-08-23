package com.zulip.android.service;

import com.zulip.android.networking.response.UserConfigurationResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public interface ZulipServices {

    @POST("v1/register")
    Call<UserConfigurationResponse> register();

    @FormUrlEncoded
    @PUT("v1/users/me/pointer")
    Call<ResponseBody> updatePointer(@Field("pointer") String pointer);

}
