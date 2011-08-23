package net.hurstfrost.hudson.sounds;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor.SoundBite;
import net.hurstfrost.hudson.sounds.SoundsBuildTask.SoundSource.SourceType;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class SoundsBuildTask extends Builder {
	private final Integer	afterDelayMs;
	
	private final SoundSource soundSource;
	
	public static class SoundSource {
		public enum SourceType { INTERNAL, URL }
		
		protected final SourceType	sourceType;
		
		protected final URL	url;
		
		protected final String	soundId;
		
	    @DataBoundConstructor
		public SoundSource(String selectedSound, SourceType value, String soundUrl) {
			this.soundId = selectedSound;
			sourceType = value;
			URL url = null;
			try {
				url = new URL(soundUrl);
			} catch (MalformedURLException e) {
				// Invalid URL, handled by Descriptor.doCheckSoundUrl()
			}
			this.url = url;
		}
	}
	
	/*
	 * {"delayMs":"","kind":"net.hurstfrost.hudson.sounds.SoundsBuildTask$DescriptorImpl","soundSource":{"selectedSound":"EXPLODE","sound":"EXPLODE","value":"INTERNAL"}
	 */
    @DataBoundConstructor
    public SoundsBuildTask(String afterDelayMs, SoundSource soundSource) {
    	this.afterDelayMs = nonNegativeOrNull(afterDelayMs);
		this.soundSource = soundSource;
    }

	protected static Integer nonNegativeOrNull(String longString) {
		Integer	d = null;
    	try {
			d = Integer.parseInt(longString);
		} catch (RuntimeException e) {
			// Invalid, leave as null
		}
		return d;
	}

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    	switch (soundSource.sourceType) {
		case INTERNAL:
	    	listener.getLogger().format("Playing internal sound '%s'\n", soundSource.soundId);
			try {
				HudsonSoundsNotifier.getSoundsDescriptor().playSound(soundSource.soundId, afterDelayMs);
			} catch (Exception e) {
				listener.error(e.toString());
				return false;
			}
			break;
		case URL:
	    	listener.getLogger().format("Playing sound at '%s'\n", soundSource.url);
			try {
				HudsonSoundsNotifier.getSoundsDescriptor().playSoundFromUrl(soundSource.url, afterDelayMs);
			} catch (Exception e) {
				listener.error(e.toString());
				return false;
			}
			break;

		default:
			break;
		}
    	return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public SourceType getSourceType() {
    	return soundSource.sourceType;
	}

    public String getSoundId() {
    	return soundSource.soundId;
	}

    public URL getSoundUrl() {
    	return soundSource.url;
	}

    public long getAfterDelayMs() {
    	return afterDelayMs;
	}

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        protected DescriptorImpl(Class<? extends SoundsBuildTask> clazz) {
            super(clazz);
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
        
        public FormValidation doCheckSoundUrl(@QueryParameter String soundUrl) {
        	if (StringUtils.isEmpty(soundUrl)) {
        		return FormValidation.warning("Missing URL");
        	}
        	
        	try {
				URL url = new URL(soundUrl);
				
				if (url.getProtocol().toLowerCase().equals("file")) {
					URI	uri = new URI(soundUrl);
					
					File file = new File(uri);
					
					if (!file.exists() || !file.isFile()) {
			        	return FormValidation.error("File not found or not readable");
					}
					
					try {
						FileInputStream inputStream = new FileInputStream(file);
						
						inputStream.close();
					} catch (IOException e) {
			        	return FormValidation.error("File not found or not readable");
					}
 				}
			} catch (Exception e) {
	        	return FormValidation.error("Invalid URL");
			}
			
			return FormValidation.ok();
    	}
        
        public FormValidation doCheckAfterDelayMs(@QueryParameter String afterDelayMs) {
    		Integer	delayMsLong = null;
    		
			try {
				delayMsLong = Integer.parseInt(afterDelayMs);
			} catch (Exception e) {
				// Invalid, leave as null
			}
			
        	if (delayMsLong != null) {
        		if (delayMsLong < 0) {
        			return FormValidation.error("Invalid delay, must be non-negative.");
        		}
        		
        		if (delayMsLong <= SoundsAgentAction.DEFAULT_POLL_INTERVAL + SoundsAgentAction.LATENCY_COMPENSATION) {
        			return FormValidation.warning("Setting a short delay can disrupt sound auto-synchronisation, leave blank to play soon");
        		}
        	}
        	
        	return FormValidation.ok();
		}
        
        public List<SoundBite> getSounds() {
        	HudsonSoundsDescriptor hudsonSoundsDescriptor = HudsonSoundsNotifier.getSoundsDescriptor();
        	
        	return hudsonSoundsDescriptor.getSounds();
		}

		public FormValidation doTestSound(@QueryParameter String selectedSound) {
			try {
				HudsonSoundsNotifier.getSoundsDescriptor().playSound(selectedSound, null);
				return FormValidation.ok(String.format("Sound played successfully"));
			} catch (Exception e) {
				return FormValidation.error(String.format("Sound failed : " + e));
			}
		}
		
		public FormValidation doTestUrl(@QueryParameter String soundUrl) {
			try {
				URL url = new URL(soundUrl);
				HudsonSoundsNotifier.getSoundsDescriptor().playSoundFromUrl(url, null);
				return FormValidation.ok(String.format("Sound played successfully"));
			} catch (Exception e) {
				return FormValidation.error(String.format("Sound failed : " + e));
			}
		}
		
		@Override
		public boolean configure(final StaplerRequest req, JSONObject json) {
//			setSoundArchive(json.optString("soundArchive"));
//			JSONObject	playMethod = json.optJSONObject("playMethod");
//			if (playMethod != null) {
//				try {
//					PLAY_METHOD method = PLAY_METHOD.valueOf(playMethod.getString("value"));
//					setPlayMethod(method);
//				} catch (Exception e) {
//					Log.debug("Exception setting play method", e);
//				}
//			}
//			setSystemCommand(json.optString("systemCommand"));
//			setPipeTimeoutSecs(json.optInt("pipeTimeoutSecs"));
			save();
			return true;
		}

        @Override
        public String getHelpFile() {
            return "/plugin/sounds/help-soundsTask.html";
        }

        @Override
        public String getDisplayName() {
            return "Play a sound";
        }

        @Override
        public SoundsBuildTask newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            SoundsBuildTask soundsBuildTask = (SoundsBuildTask)req.bindJSON(clazz,formData);
			return soundsBuildTask;
        }
    }
}
