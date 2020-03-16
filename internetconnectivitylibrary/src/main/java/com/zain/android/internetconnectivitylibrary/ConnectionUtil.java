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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class ConnectionUtil implements LifecycleObserver {

    private static final String TAG = "LOG_TAG";
    private ConnectivityManager mConnectivityMgr;
    private Context mContext;
    private NetworkStateReceiver mNetworkStateReceiver;
    /*
     * boolean indicates if my device is connected to the internet or not
     * */
    private boolean mIsConnected = false;
    private ConnectionMonitor mConnectionMonitor;

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
            mConnectionMonitor = new ConnectionMonitor();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
            mConnectivityMgr.registerNetworkCallback(networkRequest, mConnectionMonitor);
        }

    }

    /**
     * Returns true if connected to the internet, and false otherwise
     *
     * <p>
     * NetworkInfo is deprecated in API 29
     * https://developer.android.com/reference/android/net/NetworkInfo
     * <p>
     * getActiveNetworkInfo() is deprecated in API 29
     * https://developer.android.com/reference/android/net/ConnectivityManager#getActiveNetworkInfo()
     * <p>
     * getNetworkInfo(int) is deprecated as of API 23
     * https://developer.android.com/reference/android/net/ConnectivityManager#getNetworkInfo(int)
     */
    public boolean isOnline() {

        mIsConnected = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Checking internet connectivity
            NetworkInfo activeNetwork = null;
            if (mConnectivityMgr != null) {
                activeNetwork = mConnectivityMgr.getActiveNetworkInfo(); // Deprecated in API 29
            }
            mIsConnected = activeNetwork != null;

        } else {
            Network[] allNetworks = mConnectivityMgr.getAllNetworks(); // added in API 21 (Lollipop)

            for (Network network : allNetworks) {
                NetworkCapabilities networkCapabilities = mConnectivityMgr.getNetworkCapabilities(network);
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                        mIsConnected = true;
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

        NetworkInfo activeNetwork = mConnectivityMgr.getActiveNetworkInfo(); // Deprecated in API 29
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
                if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) { // Deprecated in API 28
                    // connected to mobile data
                    return TRANSPORT_CELLULAR;

                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) { // Deprecated in API 28
                    // connected to wifi
                    return TRANSPORT_WIFI;
                }
            }
        return NO_NETWORK_AVAILABLE;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getAvailableNetworksCount() {

        int count = 0;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Network[] allNetworks = mConnectivityMgr.getAllNetworks(); // added in API 21 (Lollipop)
            for (Network network : allNetworks) {
                NetworkCapabilities networkCapabilities = mConnectivityMgr.getNetworkCapabilities(network);
                if (networkCapabilities != null)
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                        count++;
            }

        }

//        // Has bugs below API 21
//        else {
//            NetworkInfo[] infos = mConnectivityMgr.getAllNetworkInfo(); // Deprecated in API 23
//            for (NetworkInfo networkInfo : infos) {
//                if (networkInfo.getState() == NetworkInfo.State.CONNECTED &&
//                        (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) || (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
//                    count++;
//                }
//
//            }
//
//        }
        return count;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<Integer> getAvailableNetworks() {

        List<Integer> activeNetworks = new ArrayList<>();

        Network[] allNetworks; // added in API 21 (Lollipop)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            allNetworks = mConnectivityMgr.getAllNetworks();
            for (Network network : allNetworks) {
                NetworkCapabilities networkCapabilities = mConnectivityMgr.getNetworkCapabilities(network);
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                        activeNetworks.add(TRANSPORT_WIFI);

                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                        activeNetworks.add(TRANSPORT_CELLULAR);

                }
            }
        }

//        // Has bugs below API 21
//        else {
//            NetworkInfo[] info = mConnectivityMgr.getAllNetworkInfo(); // Deprecated in API 23
//            for (NetworkInfo networkInfo : info) {
//                if (networkInfo.getState() == NetworkInfo.State.CONNECTED && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//                    activeNetworks.add(TRANSPORT_WIFI);
//                }
//                if (networkInfo.getState() == NetworkInfo.State.CONNECTED && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
//                    activeNetworks.add(TRANSPORT_CELLULAR);
//                }
//            }
//
//        }
        return activeNetworks;
    }


    public void onInternetStateListener(ConnectionStateListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mNetworkStateReceiver = new NetworkStateReceiver(listener);
            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetworkStateReceiver, intentFilter);

        } else {
            mConnectionMonitor.setOnConnectionStateListener(listener);
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        ((AppCompatActivity) mContext).getLifecycle().removeObserver(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mConnectionMonitor != null)
                mConnectivityMgr.unregisterNetworkCallback(mConnectionMonitor);
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

                NetworkInfo activeNetworkInfo = mConnectivityMgr.getActiveNetworkInfo(); // deprecated in API 29

                /*
                 * activeNetworkInfo.getState() deprecated in API 28
                 * NetworkInfo.State.CONNECTED deprecated in API 29
                 * */
                if (!mIsConnected && activeNetworkInfo != null && activeNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    Log.d(TAG, "onReceive: " + "Connected To: " + activeNetworkInfo.getTypeName());
                    mIsConnected = true;
                    mListener.onAvailable(true);

                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    if (!isOnline()) {
                        mListener.onAvailable(false);
                        mIsConnected = false;
                    }

                }

            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public class ConnectionMonitor extends ConnectivityManager.NetworkCallback {

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

            if (getAvailableNetworksCount() == 0) {
                mConnectionStateListener.onAvailable(false);
                mIsConnected = false;
            }

        }

    }

}
