/**
 * ALP Astrology CRM - Web Audio API Synths & Sound Alarms Engine
 * Provides high-fidelity, standalone audio notifications without external file network dependencies.
 */

export function playTone(freqs: number[], duration = 0.3, type: OscillatorType = "sine", sequence = false) {
  try {
    const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioContext) return;
    const ctx = new AudioContext();
    
    if (sequence) {
      // Play notes one after another (like an arpeggio)
      freqs.forEach((freq, index) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = type;
        osc.frequency.setValueAtTime(freq, ctx.currentTime + index * 0.12);
        
        gain.gain.setValueAtTime(0, ctx.currentTime + index * 0.12);
        gain.gain.linearRampToValueAtTime(0.25, ctx.currentTime + index * 0.12 + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + index * 0.12 + duration);
        
        osc.connect(gain);
        gain.connect(ctx.destination);
        
        osc.start(ctx.currentTime + index * 0.12);
        osc.stop(ctx.currentTime + index * 0.12 + duration + 0.1);
      });
    } else {
      // Play notes simultaneously (like a chord)
      freqs.forEach((freq) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        
        osc.type = type;
        osc.frequency.setValueAtTime(freq, ctx.currentTime);
        
        gain.gain.setValueAtTime(0, ctx.currentTime);
        gain.gain.linearRampToValueAtTime(0.2, ctx.currentTime + 0.04);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
        
        osc.connect(gain);
        gain.connect(ctx.destination);
        
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + duration + 0.1);
      });
    }
  } catch (err) {
    console.warn("Unable to play custom sound alarm inside sandbox:", err);
  }
}

// Pre-defined high fidelity alarms
export const SoundEngine = {
  playChime: () => {
    // Elegant system dual-tone chime
    playTone([440, 880], 0.45, "sine");
  },
  playCallAlert: () => {
    // Telephone ring ring sound using sine & triangle synthesis
    playTone([587.33, 659.25], 0.15, "triangle");
    setTimeout(() => {
      playTone([587.33, 659.25], 0.15, "triangle");
    }, 200);
    setTimeout(() => {
      playTone([587.33, 659.25], 0.15, "triangle");
    }, 400);
  },
  playTaskReminder: () => {
    // Ascending arpeggio melody - motivating & sharp
    playTone([523.25, 659.25, 783.99, 1046.50], 0.25, "sine", true);
  },
  playSuccessAlert: () => {
    // Uplifting chord sequence
    playTone([523.25, 659.25, 783.99], 0.4, "sine");
    setTimeout(() => {
      playTone([659.25, 783.99, 987.77], 0.5, "sine");
    }, 150);
  },
  playWarningSiren: () => {
    // Pulse attention alarm
    playTone([330, 440], 0.6, "sawtooth");
  }
};
