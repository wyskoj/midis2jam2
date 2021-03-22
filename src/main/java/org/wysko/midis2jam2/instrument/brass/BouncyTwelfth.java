package org.wysko.midis2jam2.instrument.brass;

public class BouncyTwelfth extends WrappedOctaveSustained.TwelfthOfOctave {
	@Override
	public void play(double duration) {
		playing = true;
		progress = 0;
		this.duration = duration;
	}
	
	@Override
	public void tick(double time, float delta) {
		if (progress >= 1) {
			playing = false;
			progress = 0;
		}
		if (playing) {
			progress += delta / duration;
			float y = (float) (9.5 - 9.5 * progress);
			y = Math.max(y, 0);
			animNode.setLocalTranslation(0, y, 0);
		} else {
			animNode.setLocalTranslation(0, 0, 0);
		}
	}
}
