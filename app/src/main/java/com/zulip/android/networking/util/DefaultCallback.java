package com.zulip.android.networking.util;

import android.support.annotation.CallSuper;

import com.zulip.android.util.ZLog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public abstract class DefaultCallback<T> implements Callback<T> {

    @Override
    @CallSuper
    public void onResponse(Call<T> call, Response<T> response) {
        if(response.isSuccessful()) {
            onSuccess(call, response);
        }
        else {
            onError(call, response);
        }
    }

    public abstract void onSuccess(Call<T> call, Response<T> response);

    public abstract void onError(Call<T> call, Response<T> response);


    @Override
    public void onFailure(Call<T> call, Throwable t) {
        //log error
        ZLog.logException(t);
    }
}
