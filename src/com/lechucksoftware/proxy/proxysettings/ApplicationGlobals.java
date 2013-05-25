package com.lechucksoftware.proxy.proxysettings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.bugsense.trace.BugSenseHandler;
import com.lechucksoftware.proxy.proxysettings.utils.LogWrapper;
import com.lechucksoftware.proxy.proxysettings.utils.Utils;
import com.shouldit.proxy.lib.ProxyConfiguration;
import com.shouldit.proxy.lib.ProxySettings;
import com.shouldit.proxy.lib.ProxyUtils;
import com.shouldit.proxy.lib.reflection.android.ProxySetting;



public class ApplicationGlobals extends Application
{
	private static ApplicationGlobals mInstance;



    private Map<String, ProxyConfiguration> configurations;
    private Map<String, ProxyConfiguration> getConfigurations()
    {
        return configurations;
    }

    private Map<String, ScanResult> notConfiguredWifi;
    public Map<String, ScanResult> getNotConfiguredWifi()
    {
        return notConfiguredWifi;
    }

	public int timeout;
	private WifiManager mWifiManager;
	private ConnectivityManager mConnManager;
	private ProxyConfiguration currentConfiguration;

	private static final String TAG = "ApplicationGlobals";
    private static ProxyConfiguration selectedConfiguration;

    public static WifiManager getWifiManager()
	{
		return getInstance().mWifiManager;
	}

	public static ConnectivityManager getConnectivityManager()
	{
		return getInstance().mConnManager;
	}

    public static void setSelectedConfiguration(ProxyConfiguration selectedConfiguration)
    {
        ApplicationGlobals.selectedConfiguration = selectedConfiguration;
    }

    public static ProxyConfiguration getSelectedConfiguration()
    {
        return ApplicationGlobals.selectedConfiguration;
    }

    @Override
	public void onCreate()
	{
		super.onCreate();

		timeout = 10000; // Set default timeout value (10 seconds)
		mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		mConnManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		configurations = new ConcurrentHashMap<String, ProxyConfiguration>();
        notConfiguredWifi = new ConcurrentHashMap<String, ScanResult>();

		mInstance = this;
				
		Utils.SetupBugSense(getApplicationContext());
				
		LogWrapper.d(TAG, "Calling broadcast intent " + Constants.PROXY_SETTINGS_STARTED);
		sendBroadcast(new Intent(Constants.PROXY_SETTINGS_STARTED));
	}

	public static ApplicationGlobals getInstance()
	{
		if (mInstance == null)
			BugSenseHandler.sendException(new Exception("Cannot find valid instance of ApplicationGlobals"));
		
		return mInstance;
	}

	public void addConfiguration(String SSID, ProxyConfiguration conf)
	{
		if (getConfigurations().containsKey(SSID))
		{
            getConfigurations().remove(SSID);
		}

        getConfigurations().put(SSID, conf);
	}
	
	public void updateProxyConfigurationList()
	{
		// Get information regarding other configured AP
		List<ProxyConfiguration> confs = ProxySettings.getProxiesConfigurations(getInstance());
		List<ScanResult> scanResults = getWifiManager().getScanResults();
		
		for (ProxyConfiguration conf : confs)
		{
			addConfiguration(ProxyUtils.cleanUpSSID(conf.getSSID()), conf);
		}
		
		if (scanResults != null)
		{
			for (ScanResult res : scanResults)
			{
				String currSSID = ProxyUtils.cleanUpSSID(res.SSID);
				if (getConfigurations().containsKey(currSSID))
				{
                    getConfigurations().get(currSSID).ap.update(res);
				}
                else
                {
                    if (getNotConfiguredWifi().containsKey(currSSID))
                    {
                        getNotConfiguredWifi().remove(currSSID);
                    }

                    getNotConfiguredWifi().put(currSSID, res);
                }
			}
		}
	}

	public ProxyConfiguration getCurrentConfiguration()
	{
		ProxyConfiguration conf = null;

		if (getInstance().mWifiManager != null && getInstance().mWifiManager.isWifiEnabled())
		{
			WifiInfo info = getInstance().mWifiManager.getConnectionInfo();
			String SSID = ProxyUtils.cleanUpSSID(info.getSSID());

			if (getConfigurations().isEmpty())
				updateProxyConfigurationList();
			
			if (getConfigurations().containsKey(SSID))
			{
				conf = getConfigurations().get(SSID);
			}

            getInstance().currentConfiguration = conf;
		}
		
		// Always return a not null configuration
		if (getInstance().currentConfiguration == null)
		{
            getInstance().currentConfiguration = new ProxyConfiguration(getInstance().getApplicationContext(), ProxySetting.NONE, null, null, null, null);
		}
		
		return getInstance().currentConfiguration;
	}

	public ProxyConfiguration getCachedConfiguration()
	{
		if (getInstance().currentConfiguration == null)
		{
			return getCurrentConfiguration();
		}
		
		return getInstance().currentConfiguration;
	}

	public List<ProxyConfiguration> getConfigurationsList()
	{
//        if (getInstance().configurations.isEmpty())

        updateProxyConfigurationList();
        Collection<ProxyConfiguration> values = getConfigurations().values();
		ArrayList<ProxyConfiguration> results = new ArrayList<ProxyConfiguration>(values);
        Collections.sort(results);

        return results;
	}
	
	public ProxyConfiguration getConfiguration(String SSID)
	{
		String cleanSSID = ProxyUtils.cleanUpSSID(SSID);
		
		if (getConfigurations().containsKey(cleanSSID))
		{
			return getConfigurations().get(cleanSSID);
		}
		else return null;
	}



    public static void connectToAP(ProxyConfiguration conf)
    {
        if(getInstance().mWifiManager != null && getInstance().mWifiManager.isWifiEnabled())
        {
            if (conf != null && conf.ap != null && conf.ap.getLevel() < Integer.MAX_VALUE)
            {
                // Connect to AP only if it's available
                getInstance().mWifiManager.enableNetwork(conf.ap.networkId, false);
            }
        }
    }

    public static Boolean isConnected()
    {
        NetworkInfo ni = getCurrentNetworkInfo();
        if (ni != null && ni.isAvailable() && ni.isConnected())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static Boolean isConnectedToWiFi()
    {
        NetworkInfo ni = getCurrentWiFiInfo();
        if (ni != null && ni.isAvailable() && ni.isConnected())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static NetworkInfo getCurrentNetworkInfo()
    {
        NetworkInfo ni = getConnectivityManager().getActiveNetworkInfo();
        return ni;
    }

    public static NetworkInfo getCurrentWiFiInfo()
    {
        NetworkInfo ni = getConnectivityManager().getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return ni;
    }

	public static void startWifiScan()
	{
		if (getInstance().mWifiManager != null && getInstance().mWifiManager.isWifiEnabled())
		{
            getInstance().mWifiManager.startScan();
		}
	}


}
