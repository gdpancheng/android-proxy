package com.lechucksoftware.proxy.proxysettings.activities.help;

import com.lechucksoftware.proxy.proxysettings.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DisclaimerIntroFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.disclaimer_intro, container, false);
	}

	public static Fragment newInstance(Context _context)
	{
		return new DisclaimerIntroFragment();
	}
}
