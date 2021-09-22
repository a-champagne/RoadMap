package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import ca.ubc.zachrivard.self.test.roadmap.api.BackendWebService;
import ca.ubc.zachrivard.self.test.roadmap.api.UserType;
import ca.ubc.zachrivard.self.test.roadmap.bluetooth.BluetoothService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class App extends Application {

    public static final String TAG = "App";
    public static String userType = "NA";

    private BackendWebService mBackendWebService;
    private BluetoothService mBluetoothService;

    public BluetoothService getBluetoothService() {
        return mBluetoothService;
    }

    public BackendWebService getBackendWebService() {
        if (mBackendWebService == null) {
            initBackendService("null");
            refreshAuthToken();
        }

        return mBackendWebService;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startBluetoothService();

        refreshAuthToken();
    }

    public void refreshAuthToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true)
                    .addOnCompleteListener(task -> {
                        String authToken = task.getResult().getToken();
                        initBackendService(authToken);
                    });
        } else {
            initBackendService("null");
        }
    }

    private void startBluetoothService() {
        Intent intent = new Intent(this, BluetoothService.class);

        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBluetoothService = ((BluetoothService.LocalBinder)service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBluetoothService = null;

            }
        };

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initBackendService(String token) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request()
                            .newBuilder()
                            .addHeader("authtoken", token)
                            .build();
                    return chain.proceed(request);
                })
                .build();

        mBackendWebService = new Retrofit.Builder()
                .baseUrl(BackendWebService.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(BackendWebService.class);
    }

    private void setUserType(String userType) {
        this.userType = userType;
        Log.e(TAG, "UserType_________");
        Log.e(TAG, userType);
    }
    public void startMainActivityForUser(Context context, FirebaseUser user) {
        // Get user type
        getBackendWebService()
                .getUserType(user.getUid())
                .enqueue(new Callback<UserType>() {
                    @Override
                    public void onResponse(Call<UserType> call, Response<UserType> response) {
                        if (response.body() == null) {
                            Log.d(TAG, "Bad response. Redirecting to login activity.");
                            launchActivity(context, LoginActivity.class);
                        } else if (response.body().getUserType().equals("manager")) {
                            Log.d(TAG, "Logging in manager.");
                            setUserType("manager");
                            Log.e(TAG, "____________________REACHED 1 _________________");
                            Intent intent = new Intent(context, ManagerActivity.class);
                            intent.putExtra(userType, "manager");

                            launchActivity(context, ManagerActivity.class);
                        } else {
                            Log.d(TAG, "Logging in driver.");
                            setUserType("driver");
                            Log.e(TAG, "____________________REACHED 2 _________________");
                            Intent intent = new Intent(context, MapsActivity.class);
                            intent.putExtra(userType, "driver");

                            launchActivity(context, MapsActivity.class);
                        }
                    }

                    @Override
                    public void onFailure(Call<UserType> call, Throwable t) {
                        Log.d(TAG, "Failed to log in. Launching login activity.");
                        launchActivity(context, LoginActivity.class);
                    }
                });
    }

    public void launchActivity(Context context, Class<? extends Activity> cls) {
        Intent i = new Intent(this, cls);
        context.startActivity(i);
    }

    public String getUserType() {
        return this.userType;
    }
}
