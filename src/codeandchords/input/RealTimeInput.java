package codeandchords.input;

import processing.core.PApplet;
import codeandchords.PortAudioAudioIO;

//import org.jaudiolibs.beads.AudioServerIO;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Plug;

public class RealTimeInput extends Input {
	Gain                   mute;
	boolean					skip5thru8;

	/**
	 *  Creates an Input object connected to PortAudio, with the given number of inputs.
	 *
	 *  @param  numInputs  an int specifying the number of lines in the AudioFormat.
	 */
	public RealTimeInput(int numInputs, PApplet pa)
	{
//		this(numInputs, new AudioContext(new AudioServerIO.JavaSound(), 512, AudioContext.defaultAudioFormat(numInputs, numInputs)));
		this(numInputs, new AudioContext(new PortAudioAudioIO(numInputs), 512, AudioContext.defaultAudioFormat(numInputs, numInputs)), pa);
		
	} // constructor - int, AudioContext
	
	/**
	 *  Creates an Input object connected to PortAudio, with the given number of inputs,
	 * as well as the option to skip inputs 5-8 (this assumes that you will have adjusted for the skip
	 * and passed in 4 more inputs than you plan to need; e.g., numInputs = 16 for 12 lines).
	 *
	 *  @param  numInputs  an int specifying the number of lines in the AudioFormat.
	 */
	public RealTimeInput(int numInputs, boolean skip5thru8, PApplet pa)
	{
		this(numInputs, new AudioContext(new PortAudioAudioIO(numInputs), 512, AudioContext.defaultAudioFormat(numInputs, numInputs)), skip5thru8, pa);

	} // constructor - int, AudioContext

	/**
	 *  Creates an Input object with the given number of inputs and particular AudioContext.
	 *
	 *  @param  numInputs     an int specifying the number of lines in the AudioFormat.
	 *  @param  audioContext  an AudioContext whose input lines will be procured as a UGen and used for the analysis calculations.
	 */
	public RealTimeInput(int numInputs, AudioContext audioContext, PApplet pa)
	{
		this(numInputs, audioContext, false, pa);
	} // int, AudioContext
	
	/**
	 * Creates an Input object with the given number of inputs and the particular AudioContext,
	 * as well as the option to skip inputs 5-8 (this assumes that you will have adjusted for the skip
	 * and passed in 4 more inputs than you plan to need; e.g., numInputs = 16 for 12 lines).
	 * @param numInputs
	 * @param audioContext
	 * @param skip5thru8
	 */
	public RealTimeInput(int numInputs, AudioContext audioContext, boolean skip5thru8, PApplet pa)
	{
		if(numInputs < 1)  {
			throw new IllegalArgumentException("Input.constructor(int, AudioContext): int parameter " + numInputs + " is less than 1; must be 1 or greater.");
		} // if(numInputs < 1)
		if(audioContext == null) {
			throw new IllegalArgumentException("Input.constructor(int, AudioContext): AudioContext parameter " + audioContext + " is null.");
		} // if(numInputs < 1)

		this.numInputs  = numInputs;
		this.ac 		= audioContext;
		this.pa			= pa;
		this.skip5thru8	= skip5thru8;
		
		if(this.skip5thru8)
		{
			this.adjustedNumInputs	= this.numInputs - 4;
			System.out.println("this.adjustedNumInputs = " + this.adjustedNumInputs);
		} else {
			this.adjustedNumInputs	= this.numInputs;
		} // 
		
		this.disposeHandler	= new DisposeHandler(this.pa, this);
		System.out.println("just registered DisposeHandler for " + pa);

		this.uGenArrayFromNumInputs(this.numInputs);
	} // constructor(int, AudioContext, boolean)
	
	/**
	 * Constructor for creating an Input object with 2 lines, 
	 * using the right and left channels of default in.
	 * 
	 * @param leftAndRight	Simply distinguishes this constructor from the others
	 */
/*	public Input(boolean leftAndRight)
	{
		this.numInputs	= 1;
		AudioIn	in1	= new AudioIn(this, 0);
		in1.play();
		/*
		this.numInputs	= 2;
		this.ac = new AudioContext();

		//TODO: might not need this:
		// creates an int[] of the input channel numbers - e.g., { 1, 2, 3, 4 } for a 4 channel input.
		int[]  inputNums  = new int[this.numInputs];
		for (int i = 0; i < this.numInputs; i++)
		{
			inputNums[i]  = i + 1;
			println("inputNums[" + i + "] = " + inputNums[i]);
		} // for
		
		// fill the uGenArray with UGens, each one from a particular line of the AudioContext.
		UGen	leftRightUGen	= this.ac.getAudioInput(inputNums);
		UGen	leftUGen	= new Throughput(this.ac, 1);
		UGen	rightUGen	= new Throughput(this.ac, 1);
		
		leftUGen.addInput(0, leftRightUGen, 0);
		rightUGen.addInput(0, leftRightUGen, 1);
		
		this.uGenArray = new UGen[this.numInputs];
		this.uGenArray[0]	= leftUGen;
		this.uGenArray[1]	= rightUGen;
//		this.uGenArray	= (UGen[])leftRightUGen.getConnectedInputs().toArray();

		// get the audio lines from the AudioContext:
		//  this.inputsUGen = ac.getAudioInput(inputNums);

		/*
		for (int i = 0; i < getuGenArray().length; i++)
		{
			// getAudioInput needs an int[] with the number of the particular line.
			getuGenArray()[i]  = ac.getAudioInput(new int[] {(i + 1)});
		}

		initInput(this.uGenArray);

	} // constructor(boolean, boolean)
		*/
	
	/**
	 * Constructor for creating a one (or two?)-channel Input object 
	 * from the machine's default audio input device;
	 * does not require Jack.
	 */
	public RealTimeInput(PApplet pa)
	{
		this(2, pa); //, new AudioContext());
	} // constructor()

	protected void uGenArrayFromNumInputs(int numInputs)
	{
		// TODO: make sure we set this everywhere else
//		this.numInputs  = numInputs;

		// creates an int[] of the input channel numbers - e.g., { 1, 2, 3, 4 } for a 4 channel input.
		int[][]	inputNums2d	= new int[this.numInputs][1];
		int[]	inputNums1d	= new int[this.numInputs];
		for (int i = 0; i < inputNums2d.length; i++)
		{
			inputNums2d[i][0]	= (i + 1);
			inputNums1d[i]		= (i + 1);
		} // for

		// get the audio lines from the AudioContext:
		//    this.inputsUGen = ac.getAudioInput(inputNums);

		// fill the uGenArray with UGens, each one from a particular line of the AudioContext.
		this.uGenArray  = new UGen[this.adjustedNumInputs];
		this.gainArray	= new Gain[this.adjustedNumInputs];
		System.out.println("uGenArray.length = " + this.uGenArray.length);
		
		UGen	audioInput	= this.ac.getAudioInput(inputNums1d);
		
		int	channelPos	= 0;
		for (int i = 0; i < this.uGenArray.length; i++)
		{
//			uGenArray[i]  = this.ac.getAudioInput(inputNums[i]);
			if(channelPos == 4 && this.skip5thru8)
			{
				channelPos	= 8;
			}
			
			this.uGenArray[i]  = new Plug(this.ac, audioInput, channelPos);
			System.out.println("Input: uGenArray[" + i + "] = " + uGenArray[i]);
			this.gainArray[i]	= new Gain(this.ac, 0, 0);
			
			channelPos	= channelPos + 1;
		} // for

		initInput(this.uGenArray, 0);
	} // uGenArrayFromNumInputs


	
	/**
	 *  Fills the fundamentalArray and adjustedFundArray with the current pitches of each input line:
	 */
	public void setFund() {
		// catching a NullPointer because I'm not sure why it happens and fear a crash during a concert.
		try
		{
			// TODO: maybe this won't be necessary once the threads are implemented.
			if(!this.pause)
			{
				for (int i = 0; i < this.adjustedNumInputs; i++)
				{
					//     println("setFund(); this.frequencyArray[i] = " + this.frequencyArray[i].getFeatures());
	
					// want to assign the value of .getFeatures() to a variable and check for null,
					// but can't, b/c it returns a float. :/  (So that must not be exactly the problem.)
					if (this.frequencyArray[i].getFeatures() != null) {
						//       println("i = " + i);
						//       println("setFund(); this.fundamentalArray[i] = " + this.fundamentalArray[i] + "this.frequencyArray[i].getFeatures() = " + this.frequencyArray[i].getFeatures());
						this.fundamentalArray[i] = this.frequencyArray[i].getFeatures();
						this.amplitudeArray[i]	= this.frequencyArray[i].getAmplitude(); // * 100;
	
						// ignores pitches with amplitude lower than "sensitivity":
						if (this.frequencyArray[i].getAmplitude() > this.sensitivity) {
							this.adjustedFundArray[i]  = this.fundamentalArray[i];
						} // if: amp > sensitivity
					} // if: features() != null
				} // if: > numInputs
			}
		} catch(NullPointerException npe)  {}
	} // setFund


	/**
	 *  Calculates the average frequency of multiple input lines.
	 *
	 *  @param   inputsToAverage  an int[] with the numbers of each of the lines whose frequency is to be averaged.
	 *
	 *  @return  float            The average pitch of the inputs whose numbers are given in the int[] param.
	 */
	public float  getAverageFund(int[] inputsToAverage)
	{
		if (inputsToAverage == null) {
			throw new IllegalArgumentException("Input_Jack.getAverageFund: int[] parameter is null.");
		} // error checking
		if (inputsToAverage.length < 1) {
			throw new IllegalArgumentException("Input_Jack.getAverageFund: int[] parameter's length is " + inputsToAverage.length + "; must be at least 1.");
		} // error checking

		float  result  = 0;

		// adds the freqencies of the specified inputs:
		for (int i = 0; i < inputsToAverage.length; i++)
		{
			result  += this.getAdjustedFund(inputsToAverage[i]);
		} // for

		// divides to find the average:
		return result/inputsToAverage.length;
	} // getAverageFund(int[])

	/**
	 *  Calculates the average frequency of multiple consecutive input lines,
	 *  numbered from "firstInput" to "lastInput".
	 *
	 *  @param   firstInput  the number of the first input whose frequency is to be averaged.
	 *  @param   lastInput   the number of the last input whose frequency is to be averaged.
	 *
	 *  @return  float            The average pitch of the inputs from "firstInput" to "lastInput".
	 */
	public float getAverageFund(int firstInput, int lastInput)
	{
		inputNumErrorCheck(firstInput, "getAverageFund(int, int) - first int");
		inputNumErrorCheck(lastInput, "getAverageFund(int, int) - second int");
		if (!(lastInput > firstInput)) {  
			throw new IllegalArgumentException("InputClassJack.getAverageFund():  lastInput param " + lastInput + " is not greater than firstInput param " + firstInput);
		} // error checking

		int  curInput  = firstInput;

		// creates an array and fills it with the ints denoting the inputs from firstInput to lastInput:
		int[]  inputsToAverage  = new int[lastInput - firstInput + 1];
		for (int i = 0; i < inputsToAverage.length; i++)
		{
			inputsToAverage[i]  = curInput;
			curInput++;
		} // for

		// calculates the average by calling the other getAverageFund on the inputsToAverage array:
		return getAverageFund(inputsToAverage);
	} // getAverageFund

	/**
	 *  Calculates the average frequency of multiple input lines.
	 *
	 *  @param   inputsToAverage  an int[] with the numbers of each of the lines whose amplitude is to be averaged.
	 *
	 *  @return  float  The average amplitude of the inputs whose numbers are given in the int[] param.
	 */
	public float  getAverageAmp(int[] inputsToAverage)
	{
		if (inputsToAverage == null) {
			throw new IllegalArgumentException("Input_Jack.getAverageAmp: int[] parameter is null.");
		} // error checking
		if (inputsToAverage.length < 1) {
			throw new IllegalArgumentException("Input_Jack.getAverageAmp: int[] parameter's length is " + inputsToAverage.length + "; must be at least 1.");
		} // error checking

		float  result  = 0;

		for (int i : inputsToAverage) {
			result  += this.getAmplitude(i);
		} // for

		return result/inputsToAverage.length;
	} // getAverageAmp

	/**
	 *  Calculates the average amplitude of multiple consecutive input lines,
	 *  numbered from "firstInput" to "lastInput".
	 *
	 *  @param   firstInput  the number of the first input whose amplitude is to be averaged.
	 *  @param   lastInput   the number of the last input whose amplitude is to be averaged.
	 *
	 *  @return  float            The average pitch of the inputs from "firstInput" to "lastInput".
	 */
	public float getAverageAmp(int firstInput, int lastInput)
	{
		inputNumErrorCheck(firstInput, "getAverageFund(int, int) - first int");
		inputNumErrorCheck(lastInput, "getAverageFund(int, int) - second int");
		if (!(lastInput > firstInput)) {  
			throw new IllegalArgumentException("InputClassJack.getAverageFund():  lastInput param " + lastInput + " is not greater than firstInput param " + firstInput);
		} // error checking

		int  curInput  = firstInput;

		int[]  inputsToAverage  = new int[lastInput - firstInput + 1];
		for (int i = 0; i < inputsToAverage.length; i++)
		{
			inputsToAverage[i]  = curInput;
			curInput++;
		} // for

		return getAverageAmp(inputsToAverage);
	} // getAverageAmp

	/**
	 *  Setter for sensitivity float instance var.
	 *
	 *  @param  newSensitivity  float with the value to which sensitivity is to be set.
	 */
	public void setSensitivity(float newSensitivity)
	{
		this.sensitivity = newSensitivity;
	}

	/*
	 * This file is part of Beads. See http://www.beadsproject.net for all information.
	 * CREDIT: This class uses portions of code taken from MEAP. See readme/CREDITS.txt.
	 *
	 *  07/02/2016
	 *  Emily Meuer
	 *
	 *  Edited to allow access to amplitude, so classes using these Frequencies
	 *  can cut out some background noise.
	 */

	//package net.beadsproject.beads.analysis.featureextractors;

	/*
import beads.FeatureExtractor;
import beads.TimeStamp;
	 */

	public UGen[] getuGenArray() {
		return uGenArray;
	}

	public void setuGenArray(UGen[] uGenArray) {
		this.uGenArray = uGenArray;
	}

	/**
	 * Frequency processes spectral data forwarded to it by a {@link PowerSpectrum}
	 * to determine the best estimate for the frequency of the current signal.
	 *
	 * @beads.category analysis
	 */
/*	class FrequencyEMM extends FeatureExtractor<Float, float[]> {

		/** The Constant FIRSTBAND. */
/*		static final int FIRSTBAND = 3;

		/** The ratio bin2hz. */
/*		private float bin2hz;

		private int bufferSize;

		private  float[]  hps;      // Harmonic Product Spectrum summed up here

		private float sampleRate;

		private float amplitude;

		/**
		 * Instantiates a new Frequency.
		 *
		 * @param sampleRate The sample rate of the audio context
		 */
/*		public FrequencyEMM(float sampleRate) {
			bufferSize = -1;
			this.sampleRate = sampleRate;
			features = null;
		}

		/* (non-Javadoc)
		 * @see com.olliebown.beads.core.PowerSpectrumListener#calculateFeatures(float[])
		 */
/*		public synchronized void process(TimeStamp startTime, TimeStamp endTime, float[] powerSpectrum) {
			if (bufferSize != powerSpectrum.length) {
				bufferSize = powerSpectrum.length;
				bin2hz = sampleRate / (2 * bufferSize);
			} // if

			hps  = new float[powerSpectrum.length];

			features = null;
			// now pick best peak from linspec
			double pmax = -1;
			int maxbin = 0;    

			for(int i = 0; i < hps.length; i++)
			{
				hps[i]  = powerSpectrum[i];
			} // for

			// 2:
			int  i;
			for(i = 0; (i * 2) < hps.length; i++)
			{
				hps[i]  = hps[i] + powerSpectrum[i*2];
			} // for

			// 3:
			for(i = 0; (i * 3) < hps.length; i++)
			{
				hps[i]  = hps[i] + powerSpectrum[i*3];
			} // for

			// 4:
			for(i = 0; (i * 4) < hps.length; i++)
			{
				hps[i]  = hps[i] + powerSpectrum[i*4];
			} // for

			for (int band = FIRSTBAND; band < powerSpectrum.length; band++) {
				double pwr = powerSpectrum[band];
				if (pwr > pmax) {
					pmax = pwr;
					maxbin = band;
				} // if
			} // for

			// I added the following line;
			// 10/5 edits may cause it to be a larger num than it was previously:
			amplitude  = (float)pmax;

			// cubic interpolation
			double yz = powerSpectrum[maxbin];
			double ym;
			if(maxbin <= 0) {
				ym = powerSpectrum[maxbin];
			} else {
				ym = powerSpectrum[maxbin - 1];
			} // else

			double yp;
			if(maxbin < powerSpectrum.length - 1) {
				yp  = powerSpectrum[maxbin + 1];
			} else {
				yp  = powerSpectrum[maxbin];
			} // else

			double k = (yp + ym) / 2 - yz;
			double x0 = (ym - yp) / (4 * k);
			features = (float)(bin2hz * (maxbin + x0));

			forward(startTime, endTime);
		}

		/* (non-Javadoc)
		 * @see com.olliebown.beads.core.FrameFeatureExtractor#getFeatureDescriptions()
		 */
/*		public String[] getFeatureDescriptions() {
			return new String[]{"frequency"};
		}

		/**
		 * @return float  amplitude of the fundamental frequency (in unknown units).
		 */
/*		public float getAmplitude() {  
			return this.amplitude;
		}
	} // FrequencyEMM

	public void setFundamentalArray(float[] newVal) {
		if(newVal == null)
		{
			throw new IllegalArgumentException("Input.setFundamentalArray: float[] parameter is null.");
		}
		
		if(newVal.length <= this.fundamentalArray.length)
		{
			for(int i = 0; i < newVal.length; i++)
			{
				this.fundamentalArray[i]	= newVal[i];
			} // for
		} // if
	} // setFundamentalArray

	public void setAdjustedFundArray(float[] newVal) {
		if(newVal == null)
		{
			throw new IllegalArgumentException("Input.setAdjustedFundArray: float[] parameter is null.");
		}
		
		if(newVal.length <= this.adjustedFundArray.length)
		{
			for(int i = 0; i < newVal.length; i++)
			{
				this.adjustedFundArray[i]	= newVal[i];
			} // for
		} // if
	} // setAdjustedFundArray
	
	public void setAmplitudeArray(float[] newVal) {
		if(newVal == null)
		{
			throw new IllegalArgumentException("Input.setAmplitudeArray: float[] parameter is null.");
		}
		
		if(newVal.length <= this.amplitudeArray.length)
		{
			for(int i = 0; i < newVal.length; i++)
			{
				this.amplitudeArray[i]	= newVal[i];
			} // for
		} // if
	} // setAmplitudeArray
*/
	/**
	 * 08/01/2017
	 * Emily Meuer
	 * 
	 * Class to stop the Input (which needs to stop the AudioContext,
	 * because it needs to stop the AudioIO, esp. when it's using the PortAudioAudioIO,
	 * which needs to call PortAudio.terminate to avoid a weird set of 
	 * NoClassDefFoundError/ClassNotFoundException/BadFileDescriptor errors that will happen occasionally on start-up).
	 * 
	 * Taken from https://forum.processing.org/two/discussion/579/run-code-on-exit-follow-up
	 *
	 */
/*
		RealTimeInput	input;

		DisposeHandler(PApplet pa, RealTimeInput input)
		{
			pa.registerMethod("dispose", this);
			this.input	= input;
		}

		public void dispose()
		{
			this.input.stop();
		}
	} // DisposeHandler
	*/
} // Input class