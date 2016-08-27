package com.zulip.android.networking.util;

import android.support.annotation.CallSuper;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DefaultCallback<T> implements Callback<T> {

    @Override
    @CallSuper
    public void onResponse(Call<T> call, Response<T> response) {
        if(response.isSuccessful()) {
            onSuccess(call, response);
        }
        else {
            onFailure(call, null);
        }
    }

    public void onSuccess(Call<T> call, Response<T> response) {

    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        //log error
        Log.e("DefaultCallback", "Nework failure", t);
    }
}
