// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import java.io.IOException;

import android.media.MediaRecorder;

public class SoundMeter {
	static final private double EMA_FILTER = 0.6;
	
	private MediaRecorder mRecorder = null;
	private double mEMA = 0.0;
	private double _amplitudeDenominator = 10;
	
	public void start(double amplitudeDenominator) throws IllegalStateException, IOException {
		_amplitudeDenominator = amplitudeDenominator == 0 ? 1 : amplitudeDenominator;
		if (mRecorder == null) {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mRecorder.setOutputFile("/dev/null");
			mRecorder.prepare();
			mRecorder.start();
			mEMA = 0.0;
		}
	}
	
	public void stop() {
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
	}
	
	public double getAmplitude() {
		if (mRecorder != null)
			return (mRecorder.getMaxAmplitude() / _amplitudeDenominator);
		else
			return 0;
	}
	
	public double getAmplutideEMA() {
		double amp = getAmplitude();
		mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
		return mEMA;
	}
}
