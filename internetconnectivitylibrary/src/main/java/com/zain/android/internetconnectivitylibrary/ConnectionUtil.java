package com.zain.android.internetconnectivitylibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class ConnectionUtil implements LifecycleObserver {

    private static final String TAG = "LOG_TAG";
    private ConnectivityManager mConnectivityMgr;
    private Context mContext;
    private NetworkStateReceiver mNetworkStateReceiver;
    private boolean mIsConnected = false;
    private ConnectionStateMonitorAPI21 mConnectionStateMonitorAPI21;
    /**
     * Indicates there is no available network.
     */
    private static final int NO_NETWORK_AVAILABLE = -1;

    /**
     * Indicates this network uses a Cellular transport.
     */
    public static final int TRANSPORT_CELLULAR = 0;

    /**
     * Indicates this network uses a Wi-Fi transport.
     */
    public static final int TRANSPORT_WIFI = 1;


    public interface ConnectionStateListener {
        void onAvailable(boolean isAvailable);
    }

    public ConnectionUtil(Context context) {
        mContext = context;
        mConnectivityMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ((AppCompatActivity) mContext).getLifecycle().addObserver(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mConnectionStateMonitorAPI21 = new ConnectionStateMonitorAPI21();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
            mConnectivityMgr.registerNetworkCallback(networkRequest, mConnectionStateMonitorAPI21);
        }


    }

    /**
     * Returns true if connected to the internet, and false otherwise
     *
     * <p>
     * NetworkInfo is deprecated as of API 29
     * https://developer.android.com/reference/android/net/NetworkInfo
     * <p>
     * getActiveNetworkInfo() is  deprecated as of API 29
     * https://developer.android.com/reference/android/net/ConnectivityManager#getActiveNetworkInfo()
     * <p>
     * getNetworkInfo(int) is deprecated as of API 23
     * https://developer.android.com/reference/android/net/ConnectivityManager#getNetworkInfo(int)
     */
    public boolean isOnline() {

        mIsConnected = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Checking internet connectivity
            NetworkInfo activeNetwork = null;
            if (mConnectivityMgr != null) {
                activeNetwork = mConnectivityMgr.getActiveNetworkInfo();
            }
            mIsConnected = activeNetwork != null;

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                Network activeNetwork = mConnectivityMgr.getActiveNetwork();
                NetworkCapabilities networkCapabilities = mConnectivityMgr.getNetworkCapabilities(activeNetwork);
                if (networkCapabilities == null) return false;
                mIsConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

            } else {
                Network[] allNetworks = mConnectivityMgr.getAllNetworks();

                for (Network network : allNetworks) {
                    NetworkCapabilities networkCapabilities = mConnectivityMgr.getNetworkCapabilities(network);
                    if (networkCapabilities != null) {
                        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                            mIsConnected = true;
//                    boolean b = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
//                return b;
                    }
                }
            }

        }
        return mIsConnected;

    }

    /**
     * Returns
     * <p> <p>
     * <p><p> NO_NETWORK_AVAILABLE >>> when you're offline
     * <p><p> TRANSPORT_CELLULAR >> When Cellular is the active network
     * <p><p> TRANSPORT_WIFI >> When Wi-Fi is the Active network
     * <p>
     */
    public int getActiveNetwork() {

        NetworkInfo activeNetwork = mConnectivityMgr.getActiveNetworkInfo();
        if (activeNetwork != null)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = mConnectivityMgr.getNetworkCapabilities(mConnectivityMgr.getActiveNetwork());
                if (capabilities != null)
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        // connected to mobile data
                        return TRANSPORT_CELLULAR;

                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        // connected to wifi
                        return TRANSPORT_WIFI;
                    }

            } else {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    // connected to mobile data
                    return TRANSPORT_CELLULAR;

                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    return TRANSPORT_WIFI;
                }
            }
        return NO_NETWORK_AVAILABLE;
    }

    public void onInternetStateListener(ConnectionStateListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mNetworkStateReceiver = new NetworkStateReceiver(listener);
            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetworkStateReceiver, intentFilter);

        } else {
            mConnectionStateMonitorAPI21.setOnConnectionStateListener(listener);
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        ((AppCompatActivity) mContext).getLifecycle().removeObserver(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mConnectionStateMonitorAPI21 != null)
                mConnectivityMgr.unregisterNetworkCallback(mConnectionStateMonitorAPI21);
        } else {
            if (mNetworkStateReceiver != null)
                mContext.unregisterReceiver(mNetworkStateReceiver);
        }

    }


    public class NetworkStateReceiver extends BroadcastReceiver {

        ConnectionStateListener mListener;

        public NetworkStateReceiver(ConnectionStateListener listener) {
            mListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {

                final Handler handler = new Handler(Looper.getMainLooper());

                NetworkInfo activeNetworkInfo = mConnectivityMgr.getActiveNetworkInfo();
                if (activeNetworkInfo != null && activeNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    Log.d(TAG, "onReceive: " + "Connected To: " + activeNetworkInfo.getTypeName());
                    mListener.onAvailable(true);

                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    /*
                     * Adding a delay to handle the case that both WiFi & cellular are connected, and when
                     * the WiFi gets disconnected, then we need to wait some delay before reporting that we are
                     * disconnected; so during this delay it can take time to transfer the active network from
                     * the disconnected WiFi to the Cellualr network, and then the listener callback doesn't
                     * return the wrong status
                     */
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            handler.removeCallbacks(this);
                            if (isOnline()) {
                                if (!mIsConnected)
                                    mListener.onAvailable(true);
                                mIsConnected = true;

                            } else {
                                mListener.onAvailable(false);
                                mIsConnected = false;
                            }
                        }

                    }, 2000); // 2 sec of delay
                }

            }
        }
    }


    /**
     * Receiver without a handler delay
     */
//    public class NetworkStateReceiver extends BroadcastReceiver {
//
//        ConnectionStateListener mListener;
//
//        public NetworkStateReceiver(ConnectionStateListener listener) {
//            mListener = listener;
//        }
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.getExtras() != null) {
//
//                NetworkInfo activeNetworkInfo = mConnectivityMgr.getActiveNetworkInfo();
//
//                if (activeNetworkInfo != null && activeNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
//                    Log.d(TAG, "onReceive: " + "Connected To: " + activeNetworkInfo.getTypeName());
//                    mListener.onAvailable(true);
//
//                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
//                    Log.d(TAG, "onReceive: " + "Disconnected ");
//                    mListener.onAvailable(false);
//                }
//            }
//        }
//    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public class ConnectionStateMonitorAPI21 extends ConnectivityManager.NetworkCallback {

        private ConnectionStateListener mConnectionStateListener;

        void setOnConnectionStateListener(ConnectionStateListener connectionStateListener) {
            mConnectionStateListener = connectionStateListener;
        }

        @Override
        public void onAvailable(@NonNull Network network) {

            if (mIsConnected)
                return;

            Log.d(TAG, "onAvailable: ");

            if (mConnectionStateListener != null) {
                mConnectionStateListener.onAvailable(true);
                mIsConnected = true;
            }

        }


        @Override
        public void onLost(@NonNull Network network) {

            final Handler handler = new Handler(Looper.getMainLooper());

            /*
             * Adding a delay to handle the case that both WiFi & cellular are connected, and when
             * the WiFi gets disconnected, then we need to wait some delay before reporting that we are
             * disconnected; so during this delay it can take time to transfer the active network from
             * the disconnected WiFi to the Cellular network, and then the listener callback doesn't
             * return the wrong status
             */
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isOnline()) {
                        handler.removeCallbacks(this);
                        return;
                    }
                    Log.d(TAG, "onLost: ");

                    if (mConnectionStateListener != null) {
                        mConnectionStateListener.onAvailable(false);
                        mIsConnected = false;
                    }
                    handler.removeCallbacks(this);
                }
            }, 2000);

        }

    }

}