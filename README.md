## *This is Android Library that allows to:*


* Get the current status the current connectivity (online / offline).
	
* Continuous check/listen to internet connection and trigger a callback when the device goes offline or online.
	
* Get the type of the device active internet connection (WiFi or Cellular).


## *API Support*

It supports minimum of API level 15, and tested till API level 28 (Android 9 / Pie).
  
  
## *How to Use:*
  
### Step 1: Add Project level dependency
```  
    allprojects {
      repositories {
        google()
        jcenter()
        maven { url 'https://jitpack.io' }
      }
    }
```

### Step 2: Add App level dependency

```
implementation 'com.github.M7mdZain:InternetConnectivityLibrary:1.2'
```     
     
### *Usage Example*
  
  
###     1. Get the current status the current connectivity (online / offline).

```
Toast.makeText(MainActivity.this, "Is online: " + connectionUtil.isOnline(), Toast.LENGTH_SHORT).show();
 ```	    


###     2. Continuous check/listen to internet connection and show a Toast message when the device goes offline or online

```
    connectionUtil.onInternetStateListener(new ConnectionUtil.ConnectionStateListener() {
	@Override
	public void onAvailable(boolean isAvailable) {
	    Toast.makeText(MainActivity.this, "Online? " + isAvailable, Toast.LENGTH_SHORT).show();
	}
    });
```

###     3. Get the type of the device active internet connection.

```
	switch (connectionUtil.getActiveNetwork()) {
		case TRANSPORT_WIFI:
		    Toast.makeText(MainActivity.this, "WiFi", Toast.LENGTH_SHORT).show();
		    break;

		case TRANSPORT_CELLULAR:
		    Toast.makeText(MainActivity.this, "Cellular", Toast.LENGTH_SHORT).show();
		    break;

		default:
		    Toast.makeText(MainActivity.this, "Offline", Toast.LENGTH_SHORT).show();
	}
```

### Make sure to do proper imports
```
  import com.zain.android.internetconnectivitylibrary.ConnectionUtil;
  import static com.zain.android.internetconnectivitylibrary.ConnectionUtil.TRANSPORT_CELLULAR;
  import static com.zain.android.internetconnectivitylibrary.ConnectionUtil.TRANSPORT_WIFI;
```

<img src="https://i.imgur.com/a169U5R.gif"/>

[img]https://i.imgur.com/a169U5R.gif[/img]
