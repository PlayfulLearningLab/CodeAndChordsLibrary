package codeandchords.input;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.SamplePlayer;
import processing.core.PApplet;

public class RecordedInput extends Input {
	
	SampleManager	sampleManager;
	
	public RecordedInput(PApplet pApplet, String sample)
	{
		this(pApplet, new String[] { sample } );
	}
	
	public RecordedInput(PApplet pApplet, String[] samples)
	{
		if(pApplet == null)
		{
			throw new IllegalArgumentException("RecordedInput.constructor: PApplet parameter is null.");
		}
		if(samples == null)
		{
			throw new IllegalArgumentException("RecordedInput.constructor: String[] parameter is null.");
		}

		this.numInputs			= samples.length;
		this.adjustedNumInputs	= this.numInputs;
		
		this.pa	= pApplet;
		this.ac	= new AudioContext();
		this.pause	= false;
		
		this.disposeHandler	= new DisposeHandler(this.pa, this);
		
		this.uGenArrayFromSamples(samples);
	}
	
	public void uGenArrayFromSamples(String[] sampleFilenames)
	{
		// Moved this from the constructor:
		this.numInputs  = sampleFilenames.length;
		this.sampleManager  = new SampleManager();
		Sample[] samples    = new Sample[sampleFilenames.length];  // samples will be initialized in a try/catch in order to determine whether or not the operation was successful.
		int  semaphore      = 1;

		try {
			//      samples  = new Sample[sampleFilenames.length];

			for (int i = 0; i < samples.length; i++)
			{
				samples[i]  = new Sample(this.pa.sketchPath(sampleFilenames[i]));
//				samples[i]  = new Sample("./" + sampleFilenames[i]);
			} // for
		}
		catch(Exception e)
		{
			semaphore  = 0;
		}
		// If semaphore is 0, then we caught an Exception when looking for the samples in the sketchpath;
		// check the data path, and throw an Exception if they still can't be found there.
		if (semaphore == 0)
		{
			try {
				for (int i = 0; i < samples.length; i++)
				{
					samples[i]  = new Sample(this.pa.dataPath(sampleFilenames[i]));
				} // for

				semaphore  = 1;
			}
			catch(Exception e)
			{
				// if there is an error, show an error message (at the bottom of the processing window)
				System.out.println("Exception while attempting to load sample!");
				e.printStackTrace(); // then print a technical description of the error
				throw new IllegalArgumentException("Input.constructor(String[]): the specified files could not be found.");
				//    exit(); // and exit the program
			}
		} // if

		if (semaphore  == 0)
		{
			RuntimeException re  = new RuntimeException("Input.constructor(String[]): the specified files could not be found.");
			re.printStackTrace();
			throw re;
		} // if

		for (int i = 0; i < samples.length; i++)
		{
			SampleManager.addToGroup("group", samples[i]);
		} // for

		this.uGenArray  		= new UGen[this.numInputs];
//		this.gainArray	= new Gain[this.numInputs];
		for (int i = 0; i < uGenArray.length; i++)
		{
			// Samples are not UGens, but SamplePlayers are; thus; make a SamplePlayer to put in uGenArray.
			uGenArray[i]  = new SamplePlayer(this.ac, SampleManager.getGroup("group").get(i));
			((SamplePlayer) uGenArray[i]).setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
//			this.gainArray[i]	= new Gain(this.ac, 1);
//			this.gainArray[i]	= new Gain(this.ac, 1, 0.5f);
		} // for
		
		SampleManager.destroyGroup("group");

		this.initInput(this.uGenArray, 1);
	} // uGenArrayFromSample(String[])

	@Override
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

}
